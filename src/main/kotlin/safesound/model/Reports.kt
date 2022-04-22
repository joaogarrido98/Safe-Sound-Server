package safesound.model

/**
 * Report model data when it's to be received
 */
data class Reports(
    val report_id: Int?,
    val report_date: String?,
    val report_details: String?,
    val report_user: Int?,
    val report_type: Int?,
    val report_venue: Int?,
    val resolved: Boolean?
)
