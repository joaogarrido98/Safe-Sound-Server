package safesound.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import safesound.database.DatabaseManager
import safesound.model.Crimes
import safesound.model.SimpleResponse

fun Route.CrimesRoutes(db: DatabaseManager) {

    /**
     * This method validates the requested data
     * @param crime, holds the requested data
     * @return Boolean according to if its valid or not
     */
    fun validateCrimeRequest(crime: Crimes): Boolean {
        return !(crime.crime_name == null || crime.crime_description == null || crime.crime_severity == null)
    }

    /**
     * routes that need police authorization
     */
    authenticate("police-interaction") {
        /**
         * validate request data and if valid
         * insert new crime into db
         */
        post("crimes/add") {
            val crimeRequest = call.receive<Crimes>()
            if (!validateCrimeRequest(crimeRequest)) {
                call.respond(SimpleResponse(false, "Badly formatted request"))
                return@post
            }
            try {
                db.addCrime(crimeRequest)
                call.respond(SimpleResponse(true, "Crime added"))
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to add new crime"))
            }
        }

        /**
         * Deactivate a specific crime
         * if given id doesnt exist send error message
         */
        post("crimes/deactivate/{id}"){
            try {
                val id = call.parameters["id"]?.toInt()
                if (id != null) {
                    val crime = db.findCrimesById(id)
                    if (crime == null) {
                        call.respond(SimpleResponse(false, "Crime does not exist"))
                    } else {
                        db.deactivateCrime(id)
                        call.respond(SimpleResponse(true, "Crime deactivated"))
                    }
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to deactivate Crime"))
            }
        }

        /**
         * Activate a specific crime
         * if given id doesnt exist send error message
         */
        post("crimes/activate/{id}"){
            try {
                val id = call.parameters["id"]?.toInt()
                if (id != null) {
                    val crime = db.findCrimesById(id)
                    if (crime == null) {
                        call.respond(SimpleResponse(false, "Crime does not exist"))
                    } else {
                        db.activateCrime(id)
                        call.respond(SimpleResponse(true, "Crime activated"))
                    }
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to activate Crime"))
            }
        }

        /**
         * get all the crimes in the database that are active or not
         * if non exist error message response
         */
        get("police/crimes"){
            try {
                val crimes = db.findAllCrimes()
                if (crimes.isEmpty()) {
                    call.respond(SimpleResponse(false, "No crimes found"))
                } else {
                    call.respond(SimpleResponse(true, "Success", crimes))
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get crimes"))
            }
        }
    }

    authenticate("police-interaction", "user-interaction"){
        /**
         * get crime with specified id in the database
         * if non exist error message response
         */
        get("crimes/id/{id}"){
            try {
                val id = call.parameters["id"]?.toInt()
                if (id != null) {
                    val crime = db.findCrimesById(id)
                    if (crime == null) {
                        call.respond(SimpleResponse(false, "No crimes found"))
                    } else {
                        call.respond(SimpleResponse(true, "Success", crime))
                    }
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get crimes"))
            }
        }

        /**
         * get all the crimes in the database that are active
         * if non exist error message response
         */
        get("user/crimes") {
            try {
                val crimes = db.findCrimes()
                if (crimes.isEmpty()) {
                    call.respond(SimpleResponse(false, "No crimes found"))
                } else {
                    call.respond(SimpleResponse(true, "Success", crimes))
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get crimes"))
            }
        }
    }
}