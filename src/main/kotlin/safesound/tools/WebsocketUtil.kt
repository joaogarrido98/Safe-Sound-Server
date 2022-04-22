package safesound.tools

import io.ktor.server.websocket.*

/**
 * This class allows the websocket to deal with multiple sessions at once
 */
class WebsocketUtil(val session: WebSocketServerSession, pathType: String) {
    val type = pathType
    var user_id : Int = 0
}