package safesound.model

/**
 * Report model data when it's to be sent
 */
data class SentReport(
    val report_id: Int?,
    val report_date: String?,
    val report_phone: String?,
    val report_details: String?,
    val report_user: String?,
    val report_type: String?,
    val report_severity: Int?,
    val report_venue: String?,
    val report_location: List<Double>,
    val resolved: Boolean?
)
