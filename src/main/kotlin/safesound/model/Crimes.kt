package safesound.model

/**
 * Crimes model data
 * */
data class Crimes(
    val crime_id: Int?,
    val crime_name: String?,
    val crime_description: String?,
    val crime_severity: Int?,
    val crime_active: Boolean?
)
