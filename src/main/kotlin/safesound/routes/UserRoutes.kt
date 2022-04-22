package safesound.routes

import safesound.database.DatabaseManager
import safesound.model.SimpleResponse
import safesound.model.User
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import safesound.tools.MessageUtils
import safesound.tools.JwtManager
import safesound.tools.ProjectUtils

/**
 * user routes logic
 */
fun Route.UserRoutes(db: DatabaseManager) {
    /**
     * Validates the register request
     * @param userRegisterRequest, holds all the data needed for the request
     * @return true or false depending on if all data needed is present
     */
    fun validateRegisterRequest(userRegisterRequest: User): Boolean {
        return !(userRegisterRequest.name == null || userRegisterRequest.surname == null || userRegisterRequest
            .user_email == null ||
                userRegisterRequest.dob == null || userRegisterRequest.nhs_number == null || userRegisterRequest.user_password ==
                null || userRegisterRequest.gender == null || userRegisterRequest.user_phone == null)
    }

    /**
     * Validates the login request
     * @param userLoginRequest, holds all the data needed fot the request
     * @return true or false depending on if all data needed is present
     */
    fun validateLoginRequest(userLoginRequest: User): Boolean {
        return !(userLoginRequest.user_email == null || userLoginRequest.user_password == null)
    }

    /**
     * This method checks the validity of the register request and if valid
     * hashes password and creates random code to insert in db
     * when database finishes transaction send email
     * @return Simple response object
     */
    post("user/register") {
        val registerRequest = call.receive<User>()
        if (!validateRegisterRequest(registerRequest)) {
            call.respond(SimpleResponse(false, "Badly formatted request"))
            return@post
        }
        try {
            registerRequest.user_password = ProjectUtils.hash(registerRequest.user_password!!)
            val code = ProjectUtils.generateRandomCode()
            db.addUser(registerRequest, code)
            MessageUtils.sendActivationSMS(registerRequest.user_phone!!, code)
            call.respond(SimpleResponse(true, "Account created"))
        } catch (e: Exception) {
            call.respond(SimpleResponse(false, "Unable to add new user"))
        }
    }

    /**
     * This method checks the query parameter code and if is not empty
     * find the activation code in database, match it to the correct user and
     * activate that specific user
     */
    get("user/activate_account") {
        val code = call.request.queryParameters["code"]
        if (code.isNullOrEmpty()) {
            call.respond("Badly formatted request")
            return@get
        }
        try {
            val activationRow = db.findActivationCode(code)
            if (activationRow == null) {
                call.respond("Activation Code not valid")
            } else {
                val user = db.findUserByEmail(activationRow.email_of_user)
                if (user == null) {
                    call.respond("User invalid")
                } else {
                    user.user_id?.let { it1 -> db.activateUser(it1, code) }
                    call.respond("Account Activated")
                }
            }
        } catch (e: Exception) {
            call.respond("Unable to activate account")
        }
    }

    /**
     * This method checks the validity of the login request and if valid
     * finds user where email is the same to given
     * if registered, checks if its active or not
     * in case it's active, compare password entered to the one in database
     * @return Simple response object either with message or JWT TOKEN
     */
    post("user/login") {
        val loginRequest = call.receive<User>()
        if (!validateLoginRequest(loginRequest)) {
            call.respond(SimpleResponse(false, "Badly formatted request"))
            return@post
        }
        try {
            val user = db.findUserByEmail(loginRequest.user_email!!)
            if (user == null) {
                call.respond(SimpleResponse(false, "User doesn't exist"))
            } else {
                if (!user.active!!) {
                    call.respond(SimpleResponse(false, "Account is not active"))
                } else {
                    if (user.user_password == ProjectUtils.hash(loginRequest.user_password!!)) {
                        call.respond(SimpleResponse(true, JwtManager.generateTokenUser(user), user))
                    } else {
                        call.respond(SimpleResponse(false, "User email or password incorrect"))
                    }
                }
            }
        } catch (e: Exception) {
            call.respond(SimpleResponse(false, "Unable to login user"))
        }
    }

    /**
     * This method checks if request is valid and if so,
     * find the user that requested a new password and send it
     * to him through email, updating it and hashing it at the same time
     * in the database
     */
    post("user/password/recover") {
        val recoverPasswordRequest = call.receive<User>()
        if (recoverPasswordRequest.user_email.isNullOrEmpty()) {
            call.respond(SimpleResponse(false, "Badly Formatted Request"))
            return@post
        }
        try {
            val user = db.findUserByEmail(recoverPasswordRequest.user_email)
            if (user == null) {
                call.respond(SimpleResponse(false, "No user with this email"))
            } else {
                val key = ProjectUtils.generateRandomCode()
                val code = ProjectUtils.hash(key)
                db.updatePassword(recoverPasswordRequest.user_email, code)
                MessageUtils.sendRecoverySMS(user.user_phone!!, key)
                call.respond(SimpleResponse(true, "New Password Sent"))
            }
        } catch (e: Exception) {
            call.respond(SimpleResponse(false, "Unable to send new password"))
        }
    }

    /**
     * All routes inside this authenticate block only allow user to
     * actually interact with server if they are authenticated
     */
    authenticate("user-interaction") {
        /**
         * This method gets a new password in the request
         * and changes the current password to the new password in the
         * database where the jwt token is the same as the user
         */
        post("user/password/change") {
            val changePasswordRequest = call.receive<User>()
            if (changePasswordRequest.user_password.isNullOrEmpty()) {
                call.respond(SimpleResponse(false, "Badly Formatted Request"))
                return@post
            }
            try {
                val email = call.principal<User>()!!.user_email
                val newPassword = ProjectUtils.hash(changePasswordRequest.user_password!!)
                if (email != null) {
                    db.updatePassword(email, newPassword)
                }
                call.respond(SimpleResponse(true, "Password Updated"))
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to update password"))
            }
        }

        /**
         * This method deactivates a specific user acount
         */
        post("user/deactivate") {
            try {
                val email = call.principal<User>()!!.user_email
                if (email != null) {
                    db.deactivateUser(email)
                }
                call.respond(SimpleResponse(true, "Account deactivated"))
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to deactivate account"))
            }
        }
    }
}