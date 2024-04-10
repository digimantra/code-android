import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONException
import org.json.JSONObject
import java.net.URI

/**
 * Needs factory class to inject.
 */
class GameSocketManager @AssistedInject constructor(
    private val preferenceDataClass: CRPreferenceDataClass,
    private val socketMessageParser: GameSocketMessageParser,
    private val oAuth: CoreOAuth,
    private val serverLoggerKot: ServerLoggerKot,
    @Assisted(KEY_SOCKET_CALLBACK) private val gameSocketCallback: IGameSocketCallback,
    @Assisted(KEY_GAME_STATE_FLOW) private val gameStateFlow: StateFlow<GameState>,
) {

    private var gameSocketClient: GameSocketIOClient? = null
    private lateinit var gameSocketListenerImpl: GameSocketListenerImpl

    init {
    }

    fun isClosed(): Boolean {
        return gameSocketClient == null || gameSocketClient!!.isClosed() || !gameSocketClient!!.isOpen()
    }

    fun connect(tableId: String, serverId: String) {
        Log.d(TAG, "connect: ")
        if (gameSocketClient?.isOpen == true) {
            gameSocketClient?.close()
        }
        gameSocketClient = null

        val socketAddress: String =
            preferenceDataClass.getString(AppConfig.PREFERENCE_GAME_SOCKET_ADDRESS, "")
        val url = "wss://" + ApiUrlConfig.getApiConfig().SOCKET_BASE_URL + socketAddress + "?table_id=" + tableId + "&target=table&server_id=" + serverId + "&session_id=" + oAuth.getAccessToken() + "&uid=" + Utils.getCurrentDate() + Utils.getCurrentTimeInMillis()

        gameSocketListenerImpl = GameSocketListenerImpl(
            socketMessageParser,
            gameSocketCallback,
            gameStateFlow,
            serverLoggerKot
        )
        gameSocketClient = GameSocketIOClient(
            URI(url),
            gameSocketListenerImpl,
            tableId
        )
        gameSocketClient!!.connect()
    }

    fun closeConnection() {
        Log.d(TAG, "closeConnection: ")
        if (gameSocketClient?.getConnection()?.isOpen() == true) {
            gameSocketClient?.close()
        }
        gameSocketClient = null
    }

    fun send_json_message(jsonObject: JSONObject) {
        Log.d(TAG, "send_json_message: $jsonObject")

        try {
            if (gameSocketClient!!.getConnection().isOpen() && gameSocketClient!!.isOpen()) {
                if (!jsonObject.getString("class").equals("echo", ignoreCase = true)) {
                    Loggers.error("Game ID " + this.hashCode() + ", send " + jsonObject.toString())
                }
                if (!gameSocketClient!!.isClosed() && gameSocketClient!!.isOpen()) gameSocketClient!!.send("4:::$jsonObject")
            } else {
                gameSocketListenerImpl.onMessageError("socketClosed")
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    companion object {

        private const val TAG = "GameSocketManager"

        private const val KEY_GAME_STATE_FLOW = "key_server_id"
        private const val KEY_SOCKET_CALLBACK = "key_socket_callback"



    }

    @AssistedFactory
    interface IGameSocketManagerFactory {
        fun create(
            @Assisted(KEY_SOCKET_CALLBACK) gameSocketCallback: IGameSocketCallback,
            @Assisted(KEY_GAME_STATE_FLOW) gameStateFlow: StateFlow<GameState>,
        ): GameSocketManager
    }

}