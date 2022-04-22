package safesound.entities

import org.jetbrains.exposed.sql.Table

/**
 * This object is used to create the table on the database
 */
object CrimesTable : Table("Crimes") {
    val crime_id = integer("crime_id").autoIncrement()
    val crime_name = varchar("crime_name", 100).uniqueIndex()
    val crime_description = varchar("crime_description", 1000)
    val crime_severity = integer("crime_severity")
    val crime_active = bool("crime_active")

    override val primaryKey = PrimaryKey(crime_id, name = "PK_Crimes")
}