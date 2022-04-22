package safesound.model

import io.ktor.server.auth.*

/**
 * User model data
 */
data class User(
    val user_id : Int?,
    val name: String?,
    val surname: String?,
    val user_phone : String?,
    val user_email : String?,
    val dob : String?,
    val nhs_number : String?,
    var user_password: String?,
    val gender : String?,
    val active : Boolean?
) : Principal
