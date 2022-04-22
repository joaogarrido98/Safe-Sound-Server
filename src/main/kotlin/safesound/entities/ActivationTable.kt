package safesound.entities

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * This object is used to create the table on the database
 */
object ActivationTable : Table("activation"){
    val activation_code = varchar("activation_code",512)
    val email_of_user = varchar("email_of_user", 512).references(UserTable.email, fkName = "FK_Users")
    val date_of_creation = datetime("date_of_creation")

    override val primaryKey = PrimaryKey(activation_code, name = "PK_Activation")
}