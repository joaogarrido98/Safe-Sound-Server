package safesound.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import safesound.database.DatabaseManager
import safesound.model.Reports
import safesound.model.SentReport
import safesound.model.SimpleResponse
import safesound.model.User
import safesound.tools.WebsocketUtil
import java.util.*
import kotlin.collections.LinkedHashSet

fun Route.ReportsRoutes(db: DatabaseManager) {
    val connections = Collections.synchronizedSet<WebsocketUtil?>(LinkedHashSet())

    /**
     * This method validates the requested data
     * @param reports, holds the requested data
     * @return Boolean according to if its valid or not
     */
    fun validateReport(reports: Reports): Boolean {
        return !(reports.report_date == null || reports.report_details == null || reports.report_type == null || reports
            .report_user == null || reports.report_venue == null)
    }

    /**
     * This route creates a websocket that is always open
     * when you enter the websocket you're given an id and a type of user
     * If you're a user type on sending a message to the server
     * this route validates the message inserts the report on the database
     * and the server sends back a success or not message back to this specific user
     * If you're police you receive a report object in case there's one
     */
    webSocket("reports/add/{type}") {
        val pathType = call.parameters["type"].toString()
        val thisConnection = WebsocketUtil(this, pathType)
        var report: Reports? = null
        var final_report: SentReport? = null
        connections += thisConnection
        try {
            while (true) {
                val data = receiveDeserialized<Reports>()
                if (validateReport(data)) {
                    report = db.addReport(data)
                    if (report != null) {
                        thisConnection.user_id = report.report_user!!
                    }
                    final_report = report?.report_id?.let { db.findReportById(it) }
                }
                connections.forEach {
                    if (report == null && it.user_id == thisConnection.user_id) {
                        it.session.sendSerialized(SimpleResponse(false, "Unable to report"))
                    }
                    if (report != null && it.user_id == thisConnection.user_id) {
                        it.session.sendSerialized(SimpleResponse(true, "Report Made"))
                    }
                    if (it.type == "police") {
                        final_report?.let { reports ->
                            it.session.sendSerialized(
                                SimpleResponse(
                                    true, "New Report",
                                    reports
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            send(e.toString())
        } finally {
            connections -= thisConnection
        }
    }

    authenticate("police-interaction") {
        /**
         * resolves a specific report
         */
        post("reports/resolve/{id}") {
            try {
                val id = call.parameters["id"]?.toInt()
                if (id != null) {
                    val report = db.findReportById(id)
                    if (report == null) {
                        call.respond(SimpleResponse(false, "Report does not exist"))
                    } else {
                        db.resolveReport(id)
                        call.respond(SimpleResponse(true, "Report resolved"))
                    }
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to resolve Report"))
            }
        }

        /**
         * gets all the reports that are unresolved
         */
        get("reports/unresolved") {
            try {
                val reports = db.findReports()
                if (reports.isEmpty()) {
                    call.respond(SimpleResponse(false, "No reports found"))
                } else {
                    call.respond(SimpleResponse(true, "Success", reports))
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get Reports"))
            }
        }
    }

    authenticate("user-interaction", "police-interaction") {
        /**
         * gets the reports for a specific venue
         */
        get("reports/venue/{venue_id}") {
            val venue_id = call.parameters["venue_id"]?.toInt()
            try {
                val reports = venue_id?.let { it1 -> db.findLatestReportsVenue(it1) }
                if (reports != null) {
                    if (reports.isEmpty()) {
                        call.respond(SimpleResponse(false, "No reports found for this venue"))
                    } else {
                        call.respond(SimpleResponse(true, "Success", reports))
                    }
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get reports"))
            }
        }

        /**
         * gets the reports for a specific venue
         */
        get("reports/latest") {
            try {
                val reports = db.findLatestReports()
                if (reports.isEmpty()) {
                    call.respond(SimpleResponse(false, "No reports found"))
                } else {
                    call.respond(SimpleResponse(true, "Success", reports))
                }
            } catch (e: Exception) {
                call.respond(SimpleResponse(false, "Unable to get reports"))
            }
        }
    }

}