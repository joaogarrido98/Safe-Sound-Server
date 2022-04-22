package safesound.entities

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

/**
 * This object is used to create the table on the database
 */
object UserTable : Table("Users") {
    val user_id = integer("user_id").autoIncrement()
    val name = varchar("name", 50)
    val surname = varchar("surname", 50)
    val email = varchar("email", 512).uniqueIndex()
    val phone = varchar("phone", 30)
    val dob = date("dob")
    val nhs_number = varchar("nhs_number", 512).uniqueIndex()
    val user_password = varchar("user_password", 512)
    val gender = varchar("gender", 50)
    val active = bool("active")

    override val primaryKey = PrimaryKey(user_id, name = "PK_Users")
}