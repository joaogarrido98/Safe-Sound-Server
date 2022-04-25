package safesound.routes

import safesound.database.DatabaseManager
import safesound.model.SimpleResponse
import safesound.tools.JwtManager
import safesound.tools.ProjectUtils
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import safesound.model.Police

fun Route.PoliceRoutes(db: DatabaseManager) {

    /**
     * Validates the credentials request
     * @param policeCredentialsRequest, holds all the data needed fot the request
     * @return true or false depending on if all data needed is present
     */
    fun validateCredentialsRequest(policeCredentialsRequest: Police): Boolean {
        return !(policeCredentialsRequest.police_badge == null || policeCredentialsRequest.police_password == null)
    }

    get("police"){
        try{
            db.addPolice(Police(
                police_id = null,
                police_active = true,
                police_badge = 111111,
                police_admin = true,
                police_password = ProjectUtils.hash("Admin123*")
            ))
        }catch(e:Exception){
            call.respond(e.message.toString())
        }
    }

    /**
     * This method checks the validity of the login request and if valid
     * finds police where badge is the same to given
     * if registered, checks if its active or not
     * in case it's active, compare password entered to the one in database
     * @return Simple response object either with message or JWT TOKEN
     */
    post("police/login") {
        val policeCredentialsRequest = call.receive<Police>()
        if (!validateCredentialsRequest(policeCredentialsRequest)) {
            call.respond(SimpleResponse(false, "Badly formatted request"))
            return@post
        }
        try {
            val police = policeCredentialsRequest.police_badge?.let { it1 -> db.findPoliceByBadge(it1) }
            if (police == null) {
                call.respond(SimpleResponse(false, "Police doesn't exist"))
            }else{
                if(!police.police_active!!){
                    call.respond(SimpleResponse(false, "Account is not active"))
                }else{
                    if (police.police_password == policeCredentialsRequest.police_password?.let { it1 ->
                            ProjectUtils.hash(it1)
                        }) {
                        call.respond(SimpleResponse(true, JwtManager.generateTokenPolice(police), police))
                    } else {
                        call.respond(SimpleResponse(false, "Police badge or password incorrect"))
                    }
                }
            }
        } catch (e: Exception) {
            call.respond(SimpleResponse(false, e.message.toString()))
        }
    }

    authenticate("police-interaction"){
        /**
         * route that deactivates a policeman
         * if police that tried is not an admin error message
         * if it is allow to deactivate policeman
         */
        post("police/deactivate"){
            val policeDeactivate = call.receive<Police>()
            if (policeDeactivate.police_badge == null) {
                call.respond(SimpleResponse(false, "Badly formatted request"))
                return@post
            }
            try {
                val badge = call.principal<Police>()?.police_badge
                val police = badge?.let { it1 -> db.findPoliceByBadge(it1) }
                if(police?.police_admin != null){
                    if (!police.police_admin){
                        call.respond(SimpleResponse(false, "Invalid Rank"))
                    }else{
                        db.deactivatePolice(policeDeactivate.police_badge)
                        call.respond(SimpleResponse(true, "Account deactivated"))
                    }
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to deactivate police"))
            }
        }

        /**
         * route that activates a policeman
         * if police that tried is not an admin error message
         * if it is allow to activate policeman
         */
        post("police/activate"){
            val policeDeactivate = call.receive<Police>()
            if (policeDeactivate.police_badge == null) {
                call.respond(SimpleResponse(false, "Badly formatted request"))
                return@post
            }
            try {
                val badge = call.principal<Police>()?.police_badge
                val police = badge?.let { it1 -> db.findPoliceByBadge(it1) }
                if(police?.police_admin != null){
                    if (!police.police_admin){
                        call.respond(SimpleResponse(false, "Invalid Rank"))
                    }else{
                        db.activatePolice(policeDeactivate.police_badge)
                        call.respond(SimpleResponse(true, "Account activated"))
                    }
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to activate account"))
            }
        }

        /**
         * route to allow the registration of a new police
         * takes a police credentials object as a request
         * validates the data and then inserts in db
         */
        post("police/register"){
            val policeRegisterRequest = call.receive<Police>()
            if (!validateCredentialsRequest(policeRegisterRequest)) {
                call.respond(SimpleResponse(false, "Badly formatted request"))
                return@post
            }
            try {
                val badge = call.principal<Police>()?.police_badge
                val police = badge?.let { it1 -> db.findPoliceByBadge(it1) }
                if(police?.police_admin != null){
                    if (!police.police_admin){
                        call.respond(SimpleResponse(false, "Invalid Rank"))
                    }else{
                        policeRegisterRequest.police_password = policeRegisterRequest.police_password?.let { it1 ->
                            ProjectUtils.hash(
                                it1
                            )
                        }
                        db.addPolice(policeRegisterRequest)
                        call.respond(SimpleResponse(true, "Account created"))
                    }
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to add new police"))
            }
        }

        /**
         * This method gets a new password in the request
         * and changes the current password to the new password in the
         * database where the jwt token is the same as the police
         */
        post("police/password"){
            val newPasswordCredential = call.receive<Police>()
            if(newPasswordCredential.police_password.isNullOrEmpty()){
                call.respond(SimpleResponse(false, "Badly Formatted Request"))
                return@post
            }
            try{
                val badge = call.principal<Police>()!!.police_badge
                val newPassword = ProjectUtils.hash(newPasswordCredential.police_password!!)
                if (badge != null) {
                    db.updatePasswordPolice(badge, newPassword)
                }
                call.respond(SimpleResponse(true, "Password Updated"))
            }catch (e: Exception){
                call.respond(SimpleResponse(false, "Unable to update password"))
            }
        }

        /**
         * This method gets a list of all police currently registered
         */
        get("police"){
            try{
                val police = db.findPolice()
                if(police.isEmpty()){
                    call.respond(SimpleResponse(false, "No police found"))
                }else{
                    call.respond(SimpleResponse(true, "Success", police))
                }
            }catch(e: Exception){
                call.respond(SimpleResponse(false, "Unable to get police"))
            }
        }
    }
}