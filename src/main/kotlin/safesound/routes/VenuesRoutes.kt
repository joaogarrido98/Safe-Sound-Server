package safesound.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import safesound.database.DatabaseManager
import safesound.model.SimpleResponse
import safesound.model.Venue

fun Route.VenuesRoutes(db: DatabaseManager) {

    /**
     * This method validates the requested data
     * @param venue, holds the requested data
     * @return Boolean according to if its valid or not
     */
    fun validateVenueRequest(venue: Venue): Boolean {
        return !(venue.venue_city == null || venue.venue_lat == null || venue.venue_long == null || venue
            .venue_name == null)
    }

    /**
     * routes that need either user or police authorization
     */
    authenticate("user-interaction", "police-interaction") {
        /**
         * get all the venues in the database
         * if non exist error message response
         */
        get("venues") {
            try {
                val venues = db.findVenues()
                if (venues.isEmpty()) {
                    call.respond(SimpleResponse(false, "No venues found"))
                } else {
                    call.respond(SimpleResponse(true, "Success", venues))
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get venues"))
            }
        }

        /**
         * get venue with specified id in the database
         * if non exist error message response
         */
        get("venues/id/{id}") {
            try {
                val id = call.parameters["id"]?.toInt()
                if (id != null) {
                    val venue = db.findVenueById(id)
                    if (venue == null) {
                        call.respond(SimpleResponse(false, "No venues found"))
                    } else {
                        call.respond(SimpleResponse(true, "Success", venue))
                    }
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get venues"))
            }
        }

        /**
         * get venues with specified name in the database
         * if non exist error message response
         */
        get("venues/name/{name}") {
            try {
                val name = call.parameters["name"].toString()
                val venue = db.findVenueByName(name)
                if (venue.isEmpty()) {
                    call.respond(SimpleResponse(false, "No venues found"))
                } else {
                    call.respond(SimpleResponse(true, "Success", venue))
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get venues"))
            }
        }

        /**
         * get venues with specified city in the database
         * if non exist error message response
         */
        get("venues/city/{city}") {
            try {
                val city = call.parameters["city"].toString()
                val venue = db.findVenueByCity(city)
                if (venue.isEmpty()) {
                    call.respond(SimpleResponse(false, "No venues found"))
                } else {
                    call.respond(SimpleResponse(true, "Success", venue))
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get venues"))
            }
        }


        /**
         * gets the severity of a specific venue
         */
        get("venues/severity/{id}") {
            try {
                val id = call.parameters["id"]?.toInt()
                val severity = id?.let { it1 -> db.getAverageSeverityForVenueId(it1) }
                if (severity == null){
                    call.respond(SimpleResponse(false, "No severity on this venue"))
                }else{
                    call.respond(SimpleResponse(true, "Success", severity))
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get severity"))
            }
        }

        /**
         * gets the severity of all venues
         */
        get("venues/severity"){
            try {
                val severity = db.getAverageSeverity()
                if(severity.isEmpty()){
                    call.respond(SimpleResponse(false, "No reports found"))
                }else{
                    call.respond(SimpleResponse(true, "Success", severity))
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get severity"))
            }
        }
    }

    /**
     * routes that need police authorization
     */
    authenticate("police-interaction") {
        /**
         * get id from url
         * find if venue with specified id exists
         * if yes deactivate it else error message response
         */
        post("venues/deactivate/{id}") {
            try {
                val id = call.parameters["id"]?.toInt()
                if (id != null) {
                    val venue = db.findVenueById(id)
                    if (venue == null) {
                        call.respond(SimpleResponse(false, "Venue does not exist"))
                    } else {
                        db.deactivateVenue(id)
                        call.respond(SimpleResponse(true, "Venue deactivated"))
                    }
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to deactivate Venue"))
            }
        }

        /**
         * get id from url
         * find if venue with specified id exists
         * if yes activate it else error message response
         */
        post("venues/activate/{id}") {
            try {
                val id = call.parameters["id"]?.toInt()
                if (id != null) {
                    val venue = db.findVenueById(id)
                    if (venue == null) {
                        call.respond(SimpleResponse(false, "Venue does not exist"))
                    } else {
                        db.activateVenue(id)
                        call.respond(SimpleResponse(true, "Venue activated"))
                    }
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to activate Venue"))
            }
        }

        /**
         * validate request data and if valid
         * insert new venue into db
         */
        post("venues/add") {
            val venuesRequest = call.receive<Venue>()
            if (!validateVenueRequest(venuesRequest)) {
                call.respond(SimpleResponse(false, "Badly formatted request"))
                return@post
            }
            try {
                db.addVenue(venuesRequest)
                call.respond(SimpleResponse(true, "Venue created"))
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to add new venue"))
            }
        }

        /**
         * get all the venues in the database
         * if non exist error message response
         */
        get("police/venues") {
            try {
                val venues = db.findAllVenues()
                if (venues.isEmpty()) {
                    call.respond(SimpleResponse(false, "No venues found"))
                } else {
                    call.respond(SimpleResponse(true, "Success", venues))
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get venues"))
            }
        }

        /**
         * get venues with specified name in the database
         * if non exist error message response
         */
        get("police/venues/name/{name}") {
            try {
                val name = call.parameters["name"].toString()
                val venue = db.findAllVenueByName(name)
                if (venue.isEmpty()) {
                    call.respond(SimpleResponse(false, "No venues found"))
                } else {
                    call.respond(SimpleResponse(true, "Success", venue))
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get venues"))
            }
        }

        /**
         * get venues with specified city in the database
         * if non exist error message response
         */
        get("police/venues/city/{city}") {
            try {
                val city = call.parameters["city"].toString()
                val venue = db.findAllVenueByCity(city)
                if (venue.isEmpty()) {
                    call.respond(SimpleResponse(false, "No venues found"))
                } else {
                    call.respond(SimpleResponse(true, "Success", venue))
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get venues"))
            }
        }
    }
}