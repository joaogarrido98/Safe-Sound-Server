package safesound.entities

import org.jetbrains.exposed.sql.Table

/**
 * This object is used to create the table on the database
 */
object PoliceTable : Table("Police"){
    val police_id = integer("police_id").autoIncrement()
    val police_badge = integer("badge").uniqueIndex()
    val police_password = varchar("police_password", 512)
    val police_active = bool("police_active")
    val police_admin = bool("police_admin")

    override val primaryKey = PrimaryKey(police_id, name = "PK_Police")
}