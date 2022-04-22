package safesound

import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import safesound.database.DatabaseManager
import safesound.routes.*
import safesound.tools.JwtManager
import java.time.Duration

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    val db = DatabaseManager()

    install(ContentNegotiation) {
        gson {
            this.setPrettyPrinting()
        }
    }

    install(DefaultHeaders)

    install(CORS) {
        method(HttpMethod.Options)
        allowNonSimpleContentTypes = true
        header(HttpHeaders.ContentType)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.AccessControlAllowOrigin)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        allowSameOrigin = true
        anyHost()
    }

    install(CachingHeaders)

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = GsonWebsocketContentConverter()
    }

    install(Authentication){
        /**
         * create user authentication path with email validation
         */
        jwt("user-interaction"){
            verifier(JwtManager.verifier)
            realm = "Safe&SoundUser"
            validate {
                val payload = it.payload
                val email = payload.getClaim("email").asString()
                val user = db.findUserByEmail(email)
                user
            }
        }

        /**
         * create police authentication path with badge validation
         */
        jwt("police-interaction"){
            verifier(JwtManager.verifier)
            realm = "Safe&SoundPolice"
            validate {
                val payload = it.payload
                val badge = payload.getClaim("badge").asInt()
                val police = db.findPoliceByBadge(badge)
                police
            }
        }
    }

    /**
     * install routes in server
     */
    routing {
        db.init()
        UserRoutes(db)
        PoliceRoutes(db)
        VenuesRoutes(db)
        ReportsRoutes(db)
        CrimesRoutes(db)
    }
}
