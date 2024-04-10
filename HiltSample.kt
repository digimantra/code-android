import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MultiGamesService: Service() {

    val coroutineScope = CoroutineScope(Dispatchers.Default)

    @Inject
    lateinit var multiGamesManager: MultiGamesManager

    @Inject
    lateinit var gameNotificationsHelper: GameNotificationsHelper

    @Inject
    lateinit var foregroundNotificationUtils: ForegroundNotificationUtils

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind: ")
        return null
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "onRebind: ")
        mStopForeground()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind: ")
        mStartForeGround()
        return true
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")

        coroutineScope.launch {
            // Random delay so that a game is created by then.
            // Otherwise active games would be empty and service stops.
            delay(5000)
            multiGamesManager.activeGames.collectLatest{
                if (it.isEmpty()) {
                    stopService()
                }
            }
        }

        // coroutineScope.launch {
        //     val temp: Flow<MultiTableWidgetState> = multiGamesManager.activeGames.map {
        //         it.values
        //     }.flatMapLatest { widgetsState ->
        //         widgetsState.asFlow()
        //     }
        //
        //     temp.collectLatest {
        //         showNotification(it)
        //     }
        // }

        // coroutineScope.launch {
        //     multiGamesManager.gameStateManagers.mapLatest {
        //         collectStateFromGameStateManager(it)
        //     }
        // }

        // coroutineScope.launch {
        //     multiGamesManager.activeGames.map {
        //         it.values
        //     }.distinctUntilChanged { old, new ->
        //         return@distinctUntilChanged false
        //     }.collectLatest { widgetsState ->
        //         // Improvise since this would be repeated many times using above distinct operator.
        //         widgetsState.forEach {
        //             handleNotificationScenario(it)
        //         }
        //     }
        // }

    }

    private suspend fun collectStateFromGameStateManager(it: Map<String, GameStateManager>) {
        it.forEach {
            it.value.stateFlow.collectLatest {

            }
        }
    }

    private fun stopService() {
        Log.d(TAG, "stopService: ")
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ")

        when (intent?.action) {
            ACTION_CREATE_GAME -> {
                val createGameData: CreateGameIntentData? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRAS_DATA, CreateGameIntentData::class.java)
                } else {
                    intent.getParcelableExtra<CreateGameIntentData>(EXTRAS_DATA)
                }
                createGame(createGameData)
            }
            else -> {}
        }


        return START_STICKY
    }

    private fun createGame(createGameData: CreateGameIntentData?) {
        createGameData ?: return
        val gameManager = multiGamesManager.create(
            createGameData.createGameData
        )
        if (createGameData.shouldConnect) {
            gameManager.reconnect("")
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        mStopForeground()
        gameNotificationsHelper.removeGameNotifications(this)
        coroutineScope.cancel()
        super.onDestroy()
    }

    private fun mStopForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    private fun mStartForeGround() {
        foregroundNotificationUtils.initGameForegroundService(
            this,
            CHANNEL_ID,
            CHANNEL_NAME
        )
    }

    companion object{

        private const val TAG = "MultiGamesService"

        private val CHANNEL_ID = "MultiGameChannel"
        private val CHANNEL_NAME = "Multi Game channel"

        private val ACTION_CREATE_GAME = "start"
        private val ACTION_STOP = "start"

        private val EXTRAS_DATA = "data"

        /**
         * Starts the service if not already started and joins the game.
         */
        @JvmStatic
        fun createGameWithService(
            context: Context,
            createGameIntentData: CreateGameIntentData,
        ){
            val serviceIntent = Intent(context.applicationContext, MultiGamesService::class.java)
            serviceIntent.action = ACTION_CREATE_GAME
            serviceIntent.putExtra(EXTRAS_DATA, createGameIntentData)
            context.startService(serviceIntent)
            // ContextCompat.startForegroundService(context.applicationContext, serviceIntent)
        }

        @JvmStatic
        fun stop(context: Context){
            val serviceIntent = Intent(context, MultiGamesService::class.java)
            context.stopService(serviceIntent)
        }

        @JvmStatic
        fun bindServiceEmpty(context: Context, gameServiceConnection: ServiceConnection){
            val gameServiceIntent = Intent(context, MultiGamesService::class.java)
            context.bindService(gameServiceIntent, gameServiceConnection, BIND_ADJUST_WITH_ACTIVITY)
        }


    }
}