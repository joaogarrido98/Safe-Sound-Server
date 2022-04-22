package safesound.entities

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * This object is used to create the table on the database
 */
object ReportsTable : Table("Reports"){
    val report_id = integer("report_id").autoIncrement()
    val report_date = datetime("report_date")
    val report_details = varchar("report_details",1000)
    val report_user = integer("report_user").references(UserTable.user_id, fkName = "FK_User_Reports")
    val report_type = integer("report_type").references(CrimesTable.crime_id, fkName = "FK_Crimes_Reports")
    val report_venue = integer("report_venue").references(VenuesTable.venue_id, fkName = "FK_Venues_Reports")
    val resolved = bool("resolved")
    override val primaryKey = PrimaryKey(report_id, name = "PK_Reports")
}