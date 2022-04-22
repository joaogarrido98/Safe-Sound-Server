package safesound.model

import io.ktor.server.auth.*

/**
 * Police model data
 * */
data class Police(
    val police_id : Int?,
    val police_badge : Int?,
    var police_password : String?,
    val police_active : Boolean?,
    val police_admin : Boolean?
): Principal
