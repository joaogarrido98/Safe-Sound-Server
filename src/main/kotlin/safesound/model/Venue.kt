package safesound.model

/**
 * Venue model data
 * */
data class Venue(
    val venue_id : Int?,
    val venue_name: String?,
    val venue_lat: Double?,
    val venue_long: Double?,
    val venue_city: String?,
    val venue_active: Boolean?
)
