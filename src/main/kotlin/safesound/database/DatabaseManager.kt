package safesound.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import safesound.entities.*
import safesound.interfaces.DBInterface
import safesound.model.*
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * class used to manage the resources of database
 */
class DatabaseManager : DBInterface {
    /**
     * connection to the database and creation of the tables
     */
    fun init() {
        Database.connect(hikariLocal())
        transaction {
            SchemaUtils.create(UserTable)
            SchemaUtils.create(ActivationTable)
            SchemaUtils.create(CrimesTable)
            SchemaUtils.create(PoliceTable)
            SchemaUtils.create(VenuesTable)
            SchemaUtils.create(ReportsTable)
        }
    }

    /**
     * server configuration for real server
     */
    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = System.getenv("JDBC_DRIVER")
        val uri = URI(System.getenv("DATABASE_URL"))
        val username = uri.userInfo.split(":").toTypedArray()[0]
        val password = uri.userInfo.split(":").toTypedArray()[1]
        config.jdbcUrl = "jdbc:postgresql://" + uri.host + ":" + uri.port + uri.path + "?sslmode=require" +
                "&user=$username&password=$password"
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    /**
     * server configuration for localhost
     */
    private fun hikariLocal(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = System.getenv("JDBC_DRIVER")
        config.jdbcUrl = System.getenv("DATABASE_URL")
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    //region user

    /**
     * query to add users to database
     */
    override suspend fun addUser(user: User, code: String) {
        query {
            UserTable.insert {
                it[name] = user.name.toString()
                it[surname] = user.surname.toString()
                it[email] = user.user_email.toString()
                it[dob] = LocalDate.parse(user.dob)
                it[nhs_number] = user.nhs_number.toString()
                it[user_password] = user.user_password.toString()
                it[gender] = user.gender.toString()
                it[active] = false
                it[phone] = user.user_phone.toString()
            }
            ActivationTable.insert {
                it[activation_code] = code
                it[email_of_user] = user.user_email.toString()
                it[date_of_creation] = LocalDateTime.now()
            }
        }
    }

    /**
     * query to find a row in user table from the email column
     */
    override suspend fun findUserByEmail(user_email: String): User? {
        return query {
            UserTable.select { UserTable.email.eq(user_email) }
                .map { rowToUser(it) }
                .singleOrNull()
        }
    }

    /**
     * query to find a row in user table from the id column
     */
    override suspend fun findUserById(user_id: Int): User? {
        return query {
            UserTable.select { UserTable.user_id.eq(user_id) }
                .map { rowToUser(it) }
                .singleOrNull()
        }
    }

    /**
     * query to find a row in activation table from the activation code
     */
    override suspend fun findActivationCode(activation_code: String): Activation? {
        return query {
            ActivationTable.select { ActivationTable.activation_code.eq(activation_code) }
                .map { rowToActivation(it) }
                .singleOrNull()
        }
    }

    /**
     * query to update a row in user table from the deactivated to activated
     */
    override suspend fun activateUser(user_id: Int, activation_code: String) {
        query {
            UserTable.update(where = { UserTable.user_id eq user_id }) {
                it[active] = true
            }
            ActivationTable.deleteWhere { ActivationTable.activation_code.eq(activation_code) }
        }
    }

    /**
     * query to update a row in user table from the activated to deactivated
     */
    override suspend fun deactivateUser(user_email: String) {
        query {
            UserTable.update(where = { UserTable.email eq user_email }) {
                it[active] = false
            }
        }
    }

    /**
     * query to update the password column in user table
     */
    override suspend fun updatePassword(email: String, newPassword: String) {
        query {
            UserTable.update(where = { UserTable.email eq email }) {
                it[user_password] = newPassword
            }
        }
    }

    /**
     * method transform the row received from user table to a class
     */
    private fun rowToUser(row: ResultRow?): User? {
        if (row == null) {
            return null
        }
        return User(
            user_id = row[UserTable.user_id],
            name = row[UserTable.name],
            surname = row[UserTable.surname],
            user_email = row[UserTable.email],
            user_phone = row[UserTable.phone],
            dob = row[UserTable.dob].toString(),
            nhs_number = row[UserTable.nhs_number],
            user_password = row[UserTable.user_password],
            gender = row[UserTable.gender],
            active = row[UserTable.active]
        )
    }

    /**
     * method transform the row received from activation table to a class
     */
    private fun rowToActivation(row: ResultRow?): Activation? {
        if (row == null) {
            return null
        }
        return Activation(
            code = row[ActivationTable.activation_code],
            email_of_user = row[ActivationTable.email_of_user],
            date_of_creation = row[ActivationTable.date_of_creation]
        )
    }
    //endregion

    //region Police
    /**
     * add a new police to database
     */
    override suspend fun addPolice(police: Police) {
        query {
            PoliceTable.insert {
                it[police_badge] = police.police_badge!!.toInt()
                it[police_password] = police.police_password.toString()
                it[police_active] = true
                it[police_admin] = police.police_admin.toString().toBoolean()
            }
        }
    }

    /**
     * find all police in database
     */
    override suspend fun findPolice(): List<Police> {
        val police = mutableListOf<Police>()
        query {
            PoliceTable.selectAll().orderBy(PoliceTable.police_badge).map {
                rowToPolice(it)?.let { it1 -> police.add(it1) }
            }
        }
        return police
    }

    /**
     * find police by given badge
     */
    override suspend fun findPoliceByBadge(police_badge: Int): Police? {
        return query {
            PoliceTable.select { PoliceTable.police_badge.eq(police_badge) }
                .map { rowToPolice(it) }
                .singleOrNull()
        }
    }

    /**
     * update the password of a police
     */
    override suspend fun updatePasswordPolice(badge: Int, newPassword: String) {
        query {
            PoliceTable.update(where = { PoliceTable.police_badge eq badge }) {
                it[police_password] = newPassword
            }
        }
    }

    /**
     * update police to be deactivated
     */
    override suspend fun deactivatePolice(badge: Int) {
        query {
            PoliceTable.update(where = { PoliceTable.police_badge eq badge }) {
                it[police_active] = false
            }
        }
    }

    /**
     * update police to be active
     */
    override suspend fun activatePolice(badge: Int) {
        query {
            PoliceTable.update(where = { PoliceTable.police_badge eq badge }) {
                it[police_active] = true
            }
        }
    }

    /**
     * transforms a row from the database into a police object
     */
    private fun rowToPolice(row: ResultRow?): Police? {
        if (row == null) {
            return null
        }
        return Police(
            police_id = row[PoliceTable.police_id],
            police_badge = row[PoliceTable.police_badge],
            police_password = row[PoliceTable.police_password],
            police_active = row[PoliceTable.police_active],
            police_admin = row[PoliceTable.police_admin]
        )
    }
    //endregion

    //region Venues
    /**
     * finds all venues that are active
     */
    override suspend fun findVenues(): List<Venue> {
        val venues = mutableListOf<Venue>()
        query {
            VenuesTable.select { VenuesTable.venue_active.eq(true) }.orderBy(VenuesTable.venue_city).map {
                rowToVenue(it)?.let { it1 -> venues.add(it1) }
            }
        }
        return venues
    }

    /**
     * finds all venues deactivated or not
     */
    override suspend fun findAllVenues(): List<Venue> {
        val venues = mutableListOf<Venue>()
        query {
            VenuesTable.selectAll().orderBy(VenuesTable.venue_city).orderBy(VenuesTable.venue_city).map {
                rowToVenue(it)?.let { it1 -> venues.add(it1) }
            }
        }
        return venues
    }

    /**
     * This method returns a specific venue for the given id
     * @return list of crimes
     */
    override suspend fun findVenueById(venue_id: Int): Venue? {
        return query {
            VenuesTable.select { VenuesTable.venue_id.eq(venue_id) }
                .map { rowToVenue(it) }
                .singleOrNull()
        }
    }

    /**
     * This method returns all venues with a city like the given one
     * that are active
     * To be used in the user front end
     * @return list of crimes
     */
    override suspend fun findVenueByCity(city: String): List<Venue> {
        val venues = mutableListOf<Venue>()
        query {
            VenuesTable.select {
                VenuesTable.venue_city.lowerCase() like "%${city.lowercase()}" and VenuesTable.venue_active.eq(
                    true
                )
            }.orderBy(VenuesTable.venue_city).map { rowToVenue(it)?.let { it1 -> venues.add(it1) } }
        }
        return venues
    }

    /**
     * This method returns all venues with a city like the given one
     * that are active or not
     * To be used in the police front end
     * @return list of crimes
     */
    override suspend fun findAllVenueByCity(city: String): List<Venue> {
        val venues = mutableListOf<Venue>()
        query {
            VenuesTable.select {
                VenuesTable.venue_city.lowerCase() like "%${city.lowercase()}"
            }.orderBy(VenuesTable.venue_city).map { rowToVenue(it)?.let { it1 -> venues.add(it1) } }
        }
        return venues
    }

    /**
     * This method returns all venues with a name like the given one
     * that are active
     * To be used in the user front end
     * @return list of crimes
     */
    override suspend fun findVenueByName(name: String): List<Venue> {
        val venues = mutableListOf<Venue>()
        query {
            VenuesTable.select {
                VenuesTable.venue_name.lowerCase() like "%${name.lowercase()}%" and VenuesTable.venue_active.eq(
                    true
                )
            }.orderBy(VenuesTable.venue_city).map { rowToVenue(it)?.let { it1 -> venues.add(it1) } }
        }
        return venues
    }

    /**
     * This method returns all venues with a name like the given one
     * that are active or not
     * To be used in the police front end
     * @return list of crimes
     */
    override suspend fun findAllVenueByName(name: String): List<Venue> {
        val venues = mutableListOf<Venue>()
        query {
            VenuesTable.select {
                VenuesTable.venue_name.lowerCase() like "%${name.lowercase()}"
            }.orderBy(VenuesTable.venue_city).map { rowToVenue(it)?.let { it1 -> venues.add(it1) } }
        }
        return venues
    }

    /**
     * This method adds a new venue to the database
     */
    override suspend fun addVenue(venue: Venue) {
        query {
            VenuesTable.insert {
                it[venue_name] = venue.venue_name.toString()
                it[venue_city] = venue.venue_city.toString()
                it[venue_lat] = venue.venue_lat!!.toDouble()
                it[venue_long] = venue.venue_long!!.toDouble()
                it[venue_active] = true
            }
        }
    }

    /**
     * This method deactivates a venue
     */
    override suspend fun deactivateVenue(venue_id: Int) {
        query {
            VenuesTable.update(where = { VenuesTable.venue_id eq venue_id }) {
                it[venue_active] = false
            }
        }
    }

    /**
     * This method activates a venue
     */
    override suspend fun activateVenue(venue_id: Int) {
        query {
            VenuesTable.update(where = { VenuesTable.venue_id eq venue_id }) {
                it[venue_active] = true
            }
        }
    }

    /**
     * This method gets the average of the severity of crimes in a specific venue
     */
    override suspend fun getAverageSeverityForVenueId(id: Int): Severity? {
        val average = CrimesTable.crime_severity.avg().alias("average")
        return query {
            ReportsTable.innerJoin(CrimesTable).slice(average).select {
                ReportsTable
                    .report_venue eq id
            }.groupBy(ReportsTable.report_venue).map {
                it[average]?.let { it1 ->
                    Severity(
                        average_severity = it1.toDouble()
                    )
                }
            }.singleOrNull()
        }
    }

    /**
     * This method gets the average of the severity of crimes in all venues
     */
    override suspend fun getAverageSeverity(): HashMap<Int, Severity> {
        val severities = HashMap<Int, Severity>()
        val average = CrimesTable.crime_severity.avg().alias("average")
        query {
            ReportsTable.innerJoin(CrimesTable).innerJoin(VenuesTable).slice(average, ReportsTable.report_venue)
                .select { VenuesTable.venue_active eq true }.groupBy(ReportsTable.report_venue).map {
                    it[average]?.let { it1 ->
                        val sev = Severity(
                            average_severity = it1.toDouble()
                        )
                        severities.put(it[ReportsTable.report_venue].toInt(), sev)
                    }
                }
        }
        return severities
    }

    /**
     * This method transforms the row from a database into
     * a venue object
     * @return Venue object or null
     */
    private fun rowToVenue(row: ResultRow?): Venue? {
        if (row == null) {
            return null
        }
        return Venue(
            venue_id = row[VenuesTable.venue_id],
            venue_name = row[VenuesTable.venue_name],
            venue_city = row[VenuesTable.venue_city],
            venue_active = row[VenuesTable.venue_active],
            venue_lat = row[VenuesTable.venue_lat],
            venue_long = row[VenuesTable.venue_long]
        )
    }
    //endregion

    //region crimes

    /**
     * This method adds a new crime to the database
     */
    override suspend fun addCrime(crime: Crimes) {
        query {
            CrimesTable.insert {
                it[crime_name] = crime.crime_name.toString()
                it[crime_severity] = crime.crime_severity!!.toInt()
                it[crime_description] = crime.crime_description.toString()
                it[crime_active] = true
            }
        }
    }

    /**
     * This method returns all crimes active or not
     * To be used in the police front end
     * @return list of crimes
     */
    override suspend fun findAllCrimes(): List<Crimes> {
        val crimes = mutableListOf<Crimes>()
        query {
            CrimesTable.selectAll().orderBy(CrimesTable.crime_name).map {
                rowToCrime(it)?.let { it1 -> crimes.add(it1) }
            }
        }
        return crimes
    }


    /**
     * This method returns all crimes that are active
     * To be used in the user front end
     * @return list of crimes
     */
    override suspend fun findCrimes(): List<Crimes> {
        val crimes = mutableListOf<Crimes>()
        query {
            CrimesTable.select { CrimesTable.crime_active.eq(true) }.orderBy(CrimesTable.crime_name).map {
                rowToCrime(it)?.let { it1 -> crimes.add(it1) }
            }
        }
        return crimes
    }

    /**
     * This method returns a specific crime given by an id
     * @return Crime object or null
     */
    override suspend fun findCrimesById(crime_id: Int): Crimes? {
        return query {
            CrimesTable.select { CrimesTable.crime_id eq crime_id }
                .map { rowToCrime(it) }
                .singleOrNull()
        }
    }

    /**
     * This method deactivates a crime
     */
    override suspend fun deactivateCrime(crime_id: Int) {
        query {
            CrimesTable.update(where = { CrimesTable.crime_id eq crime_id }) {
                it[crime_active] = false
            }
        }
    }

    /**
     * This method activates a crime
     */
    override suspend fun activateCrime(crime_id: Int) {
        query {
            CrimesTable.update(where = { CrimesTable.crime_id eq crime_id }) {
                it[crime_active] = true
            }
        }
    }

    /**
     * This method transforms the row from a database into
     * a crime object
     * @return Crime object or null
     */
    private fun rowToCrime(row: ResultRow?): Crimes? {
        if (row == null) {
            return null
        }
        return Crimes(
            crime_id = row[CrimesTable.crime_id],
            crime_name = row[CrimesTable.crime_name],
            crime_severity = row[CrimesTable.crime_severity],
            crime_description = row[CrimesTable.crime_description],
            crime_active = row[CrimesTable.crime_active]
        )
    }
    //endregion

    //region reports

    /**
     * add a new report to the database
     */
    override suspend fun addReport(reports: Reports): Reports? {
        var inserted: ResultRow? = null
        query {
            inserted = ReportsTable.insert {
                it[report_date] = LocalDateTime.parse(reports.report_date)
                it[report_details] = reports.report_details.toString()
                it[report_type] = reports.report_type!!.toInt()
                it[report_user] = reports.report_user!!.toInt()
                it[report_venue] = reports.report_venue!!.toInt()
                it[resolved] = false
            }.resultedValues!!.first()
        }
        return rowToReport(inserted)
    }

    /**
     * this finds a specific report by the id given
     */
    override suspend fun findReportById(id: Int): SentReport? {
        return query {
            ReportsTable.innerJoin(CrimesTable).innerJoin(UserTable).innerJoin(VenuesTable).select {
                ReportsTable
                    .report_id eq id
            }.map {
                val lat = it[VenuesTable.venue_lat]
                val long = it[VenuesTable.venue_long]
                val location = listOf(lat, long)
                SentReport(
                    report_venue = it[VenuesTable.venue_name],
                    report_user = it[UserTable.name],
                    report_type = it[CrimesTable.crime_name],
                    report_id = it[ReportsTable.report_id],
                    report_date = it[ReportsTable.report_date].toString(),
                    report_details = it[ReportsTable.report_details],
                    report_phone = it[UserTable.phone],
                    report_severity = it[CrimesTable.crime_severity],
                    report_location = location,
                    resolved = it[ReportsTable.resolved]
                )
            }.singleOrNull()
        }
    }

    /**
     * find all reports that are unresolved
     */
    override suspend fun findReports(): List<SentReport> {
        val reports = mutableListOf<SentReport>()
        query {
            ReportsTable.innerJoin(CrimesTable).innerJoin(UserTable).innerJoin(VenuesTable).select {
                ReportsTable.resolved eq false
            }.map {
                val lat = it[VenuesTable.venue_lat]
                val long = it[VenuesTable.venue_long]
                val location = listOf(lat, long)
                val sr = SentReport(
                    report_venue = it[VenuesTable.venue_name],
                    report_user = it[UserTable.name],
                    report_type = it[CrimesTable.crime_name],
                    report_id = it[ReportsTable.report_id],
                    report_date = it[ReportsTable.report_date].toString(),
                    report_details = it[ReportsTable.report_details],
                    report_phone = it[UserTable.phone],
                    report_severity = it[CrimesTable.crime_severity],
                    report_location = location,
                    resolved = it[ReportsTable.resolved]
                )
                reports.add(sr)
            }
        }
        return reports
    }

    /**
     * This method resolves a report
     */
    override suspend fun resolveReport (report_id: Int) {
        query {
            ReportsTable.update(where = { ReportsTable.report_id eq report_id }) {
                it[resolved] = true
            }
        }
    }

    /**
     * this finds the latest 10 reports for a specific venue
     */
    override suspend fun findLatestReportsVenue(venue_id: Int): List<SentReport> {
        val reports = mutableListOf<SentReport>()
        query {
            ReportsTable.innerJoin(CrimesTable).innerJoin(VenuesTable).innerJoin(UserTable).select {
                ReportsTable
                    .report_venue eq
                        venue_id
            }
                .limit(10)
                .orderBy(ReportsTable.report_id to SortOrder.DESC).map {
                    val lat = it[VenuesTable.venue_lat]
                    val long = it[VenuesTable.venue_long]
                    val location = listOf(lat, long)
                    val sr = SentReport(
                        report_venue = it[VenuesTable.venue_name],
                        report_user = it[UserTable.name],
                        report_type = it[CrimesTable.crime_name],
                        report_id = it[ReportsTable.report_id],
                        report_date = it[ReportsTable.report_date].toString(),
                        report_details = it[ReportsTable.report_details],
                        report_phone = it[UserTable.phone],
                        report_severity = it[CrimesTable.crime_severity],
                        report_location = location,
                        resolved = it[ReportsTable.resolved]
                    )
                    reports.add(sr)
                }
        }
        return reports
    }


    override suspend fun findLatestReports(): List<SentReport>{
        val reports = mutableListOf<SentReport>()
        query {
            ReportsTable.innerJoin(CrimesTable).innerJoin(VenuesTable).innerJoin(UserTable).selectAll()
                .limit(10)
                .orderBy(ReportsTable.report_id to SortOrder.DESC).map {
                    val lat = it[VenuesTable.venue_lat]
                    val long = it[VenuesTable.venue_long]
                    val location = listOf(lat, long)
                    val sr = SentReport(
                        report_venue = it[VenuesTable.venue_name],
                        report_user = it[UserTable.name],
                        report_type = it[CrimesTable.crime_name],
                        report_id = it[ReportsTable.report_id],
                        report_date = it[ReportsTable.report_date].toString(),
                        report_details = it[ReportsTable.report_details],
                        report_phone = it[UserTable.phone],
                        report_severity = it[CrimesTable.crime_severity],
                        report_location = location,
                        resolved = it[ReportsTable.resolved]
                    )
                    reports.add(sr)
                }
        }
        return reports
    }

    /**
     * This method transforms the row from the report table
     * into a report object
     */
    private fun rowToReport(row: ResultRow?): Reports? {
        if (row == null) {
            return null
        }
        return Reports(
            report_id = row[ReportsTable.report_id],
            report_date = row[ReportsTable.report_date].toString(),
            report_details = row[ReportsTable.report_details],
            report_type = row[ReportsTable.report_type],
            report_user = row[ReportsTable.report_user],
            report_venue = row[ReportsTable.report_venue],
            resolved = row[ReportsTable.resolved]
        )
    }
    //endregion

    /**
     * This method runs the transaction on the database in
     * a different thread
     */
    private suspend fun <T> query(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block() }
        }


}