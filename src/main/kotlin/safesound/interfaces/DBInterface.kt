package safesound.interfaces

import safesound.model.*

interface DBInterface {
    //region user
    suspend fun addUser(user: User, code: String)

    suspend fun findUserByEmail(user_email: String): User?

    suspend fun findUserById(user_id: Int): User?

    suspend fun findActivationCode(activation_code: String): Activation?

    suspend fun activateUser(user_id: Int, activation_code: String)

    suspend fun updatePassword(email: String, newPassword: String)

    suspend fun deactivateUser(user_email: String)
    //endregion

    //region Police
    suspend fun findPoliceByBadge(police_badge: Int): Police?

    suspend fun addPolice(police: Police)

    suspend fun activatePolice(badge: Int)

    suspend fun deactivatePolice(badge: Int)

    suspend fun updatePasswordPolice(badge: Int, newPassword: String)

    suspend fun findPolice(): List<Police>
    //endregion

    //region venues
    suspend fun findVenues(): List<Venue>

    suspend fun addVenue(venue: Venue)

    suspend fun deactivateVenue(venue_id: Int)

    suspend fun activateVenue(venue_id: Int)

    suspend fun findVenueById(venue_id: Int): Venue?

    suspend fun findVenueByCity(city: String): List<Venue>

    suspend fun findVenueByName(name: String): List<Venue>

    suspend fun findAllVenues(): List<Venue>

    suspend fun findAllVenueByCity(city: String): List<Venue>

    suspend fun findAllVenueByName(name: String): List<Venue>

    suspend fun getAverageSeverityForVenueId(id: Int): Severity?

    suspend fun getAverageSeverity(): HashMap<Int, Severity>
    //endregion

    //region crimes
    suspend fun addCrime(crime: Crimes)

    suspend fun findCrimes(): List<Crimes>

    suspend fun findCrimesById(crime_id: Int): Crimes?

    suspend fun deactivateCrime(crime_id: Int)

    suspend fun activateCrime(crime_id: Int)

    suspend fun findAllCrimes(): List<Crimes>
    //endregion

    //region reports
    suspend fun addReport(reports: Reports): Reports?

    suspend fun findLatestReportsVenue(venue_id: Int): List<SentReport>

    suspend fun findReportById(id: Int): SentReport?

    suspend fun findReports(): List<SentReport>
    //endregion
    suspend fun resolveReport(report_id: Int)

    suspend fun findLatestReports(): List<SentReport>
}