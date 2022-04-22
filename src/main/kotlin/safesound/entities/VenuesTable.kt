package safesound.entities

import org.jetbrains.exposed.sql.Table

/**
 * This object is used to create the table on the database
 */
object VenuesTable : Table("Venues"){
    val venue_id = integer("venue_id").autoIncrement()
    val venue_name = varchar("venue_name", 100).uniqueIndex()
    val venue_lat = double("venue_lat")
    val venue_long = double("venue_long")
    val venue_city = varchar("venue_city", 100)
    val venue_active = bool("venue_active")

    override val primaryKey = PrimaryKey(venue_id, name = "PK_Venues")
}