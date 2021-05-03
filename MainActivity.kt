package com.digimantra.sceptavpn.ui.main

import android.content.*
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.digimantra.sceptavpn.*
import com.digimantra.sceptavpn.ui.fragments.HomeFragment
import com.digimantra.sceptavpn.ui.fragments.Manage_Subs
import com.digimantra.sceptavpn.ui.main.adapter.DrawerAdapter
import com.digimantra.sceptavpn.ui.price.PriceActivity
import com.digimantra.sceptavpn.utils.CommanMethod
import com.digimantra.sceptavpn.utils.CustomSwipeListener
import com.digimantra.sceptavpn.utils.Preff_manager
import com.digimantra.sceptavpn.utils.toast
import de.blinkt.openvpn.LaunchVPN
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.*
import de.blinkt.openvpn.core.VpnStatus.ByteCountListener
import kotlinx.android.synthetic.main.fragment_home.view.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), CustomSwipeListener.GestureCallback, View.OnClickListener, AdapterView.OnItemClickListener, ByteCountListener, VpnStatus.StateListener {

    val preffManager: Preff_manager by lazy {
        Preff_manager()
    }
    var drawerLayout: DrawerLayout? = null
    var mdrawerlist: ListView? = null
    var fragment: Fragment? = null
    var navigationtoggle: ImageView? = null
    var nvbtn_close: ImageView? = null
    var drawerAdapter: DrawerAdapter? = null
    var titleArray: Array<String>? = null
    var drawerview: LinearLayout? = null
    var bundle: Bundle? = null
    var doubleBackToExitPressedOnce = false
    private val swipeListener by lazy { CustomSwipeListener(this, this) }
    private var currentFragmentPosition = 0
    var mService: IOpenVPNServiceInternal? = null
    var inputStream: InputStream? = null
    var bufferedReader: BufferedReader? = null
    lateinit var cp: ConfigParser
    lateinit var vp: VpnProfile
    lateinit var pm: ProfileManager
    lateinit var thread: Thread
    var tv_message_top_text: TextView? = null
    var EnableConnectButton = false
    var TODAY: String? = null
    lateinit var ConnectionTimer: CountDownTimer

    // new
    var hasFile = false
    var FileID: String? = "NULL"
    var File: String? = "NULL"
    var City: String? = "NULL"
    var Image: String? = "NULL"
    var DarkMode: String? = "false"

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mService = IOpenVPNServiceInternal.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mService = null
        }
    }

    //*** lifecycle onStop function  ***//
    override fun onStop() {
        VpnStatus.removeStateListener(this)
        VpnStatus.removeByteCountListener(this)
        super.onStop()
    }

    //*** lifecycle onResume function (bind service)  ***//
    override fun onResume() {
        super.onResume()
        val SettingsDetails = getSharedPreferences("settings_data", 0)
        DarkMode = SettingsDetails.getString("dark_mode", "false")
        if (!Data.isAppDetails && !Data.isConnectionDetails) {
            try {
                val Welcome = Intent(this@MainActivity1, WelcomeActivity::class.java)
                startActivity(Welcome)
            } catch (e: java.lang.Exception) {
                val params = Bundle()
                params.putString("device_id", App.device_id)
                params.putString("exception", "MA1$e")
                //  mFirebaseAnalytics!!.logEvent("app_param_error", params)
            }
        }
        val En = EncryptData()
        val ConnectionDetails = getSharedPreferences("connection_data", 0)
        FileID = ConnectionDetails.getString("file_id", "NA")
        File = En.decrypt(ConnectionDetails.getString("file", "NA"))
        City = ConnectionDetails.getString("city", "NA")
        Image = ConnectionDetails.getString("image", "NA")
        hasFile = if (!FileID!!.isEmpty()) {
            true
        } else {
            false
        }
        try {
            VpnStatus.addStateListener(this)
            VpnStatus.addByteCountListener(this)
            val intent = Intent(this, OpenVPNService::class.java)
            intent.action = OpenVPNService.START_SERVICE
            bindService(intent, mConnection, BIND_AUTO_CREATE)
            //bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } catch (e: java.lang.Exception) {
            val params = Bundle()
            params.putString("device_id", App.device_id)
            params.putString("exception", "MA2$e")
            // mFirebaseAnalytics!!.logEvent("app_param_error", params)
        }
    }

    //*** lifecycle onPause function (unbind service)  ***//
    override fun onPause() {
        super.onPause()
        unbindService(mConnection)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_two)
        initView()
        initListner()
        initData()
        setUpActivityMain()

    }

    //*** initializing views  ***//
    private fun initView() {
        drawerLayout = findViewById<View>(R.id.drawerlayout) as DrawerLayout
        mdrawerlist = findViewById<View>(R.id.left_drawer) as ListView
        navigationtoggle = findViewById<View>(R.id.navbtn) as ImageView
        drawerview = findViewById<View>(R.id.customdrawer) as LinearLayout
        nvbtn_close = findViewById(R.id.nvclosebtn)
    }

    //*** initializing Data  ***//
    private fun initData() {
        titleArray = resources.getStringArray(R.array.navigation_item)
        val typedArray = resources.obtainTypedArray(R.array.navigation_icon)
        drawerAdapter = DrawerAdapter(this, typedArray, titleArray!!)
        mdrawerlist!!.adapter = drawerAdapter
        if (bundle == null) {
            CommanMethod().fragmentTransaction(this, HomeFragment(), HomeFragment::class.java.simpleName)
        }
    }

    //*** initializing the Listener  ***//
    private fun initListner() {
        navigationtoggle!!.setOnClickListener(this)
        mdrawerlist!!.onItemClickListener = this
        nvbtn_close!!.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.navbtn -> toggleMenu()
            R.id.nvclosebtn -> drawerLayout!!.closeDrawer(drawerview!!)
        }
    }

    //*** drawer toggle button  ***//
    private fun toggleMenu() {
        if (drawerLayout!!.isDrawerOpen(drawerview!!)) {
            drawerLayout!!.closeDrawer(drawerview!!)
        } else {
            drawerLayout!!.openDrawer(drawerview!!)
        }
    }

    //*** on Backpress Call  ***//
    override fun onBackPressed() {
        if (drawerLayout!!.isDrawerOpen(drawerview!!)) {
            drawerLayout!!.closeDrawer(drawerview!!)
        } else if (supportFragmentManager.getBackStackEntryCount() == 1) {
            if (doubleBackToExitPressedOnce) {
                finish();
            }
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press Back Button twice to Exit", Toast.LENGTH_SHORT).show();

            Handler(Looper.getMainLooper()).postDelayed({
                doubleBackToExitPressedOnce = false;

            }, 1000)


        } else {
            super.onBackPressed()
        }
    }

    //*** on swipe dispatchTouchEvent listener  ***//
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        swipeListener.setScreenHeight(getScreenHeight().toInt())
        if (swipeListener.gestureDetector.onTouchEvent(ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    //*** function to get Screen Height  ***//
    private fun getScreenHeight(): Float {
        val dP = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dP)
        return dP.heightPixels.toFloat()
    }

    //***  function onSwipUp  ***//
    override fun onSwipeUp() {
        val f = supportFragmentManager.findFragmentByTag(HomeFragment::class.java.simpleName) as? HomeFragment
        f?.onSwipeUp()
    }

    //***  drawer Item Click Listener  ***//
    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        when (position) {
            0 -> {
                if (currentFragmentPosition == 0) {
                    drawerLayout!!.closeDrawer(drawerview!!)
                    return
                }
                currentFragmentPosition = 0
                CommanMethod().fragmentTransaction(this@MainActivity1, HomeFragment(), HomeFragment::class.java.simpleName)

                Handler(Looper.getMainLooper()).postDelayed({

                    drawerLayout!!.closeDrawer(drawerview!!)

                }, 500)

            }

            1 -> {

                if (currentFragmentPosition == 1) {
                    drawerLayout!!.closeDrawer(drawerview!!)
                    return
                } else {


                    if (preffManager.getActivePlan(this@MainActivity1) == 0) {
                        drawerLayout!!.closeDrawer(drawerview!!)

                        startActivity(Intent(this@MainActivity1, PriceActivity::class.java).putExtra("show_backbtn", true))

                    } else {
                        currentFragmentPosition = 1
                        CommanMethod().fragmentTransaction(this@MainActivity1, Manage_Subs())


                        Handler(Looper.getMainLooper()).postDelayed({

                            drawerLayout!!.closeDrawer(drawerview!!)

                        }, 500)

                    }
                }


            }
            2 -> {
                drawerLayout!!.closeDrawer(drawerview!!)

                startActivity(Intent(this, CommonPDFActivity::class.java).apply {
                    putExtra("title", "FAQs")
                })
            }
            3 -> {
                //drawerLayout!!.closeDrawer(drawerview!!)

                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("admin@sceptavpn.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "")
                    if (resolveActivity(packageManager) != null) {
                        startActivity(Intent.createChooser(this, "Open with..."))
                    } else {
                        toast("No apps were found to compose an email")
                    }
                }
            }
            4 -> {
                drawerLayout!!.closeDrawer(drawerview!!)

                startActivity(Intent(this, CommonPDFActivity::class.java).apply {
                    putExtra("title", "About Us")
                })
            }
            5 -> {
                drawerLayout!!.closeDrawer(drawerview!!)

                startActivity(Intent(this, CommonPDFActivity::class.java).apply {
                    putExtra("title", "Terms of Service")
                })
            }
            6 -> Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    //***  function to Check internet Connection  ***//
    private fun hasInternetConnection(): Boolean {
        var haveConnectedWifi = false
        var haveConnectedMobile = false
        try {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.allNetworkInfo
            for (ni in netInfo) {
                if (ni.typeName.equals("WIFI", ignoreCase = true)) if (ni.isConnected) haveConnectedWifi = true
                if (ni.typeName.equals("MOBILE", ignoreCase = true)) if (ni.isConnected) haveConnectedMobile = true
            }
        } catch (e: java.lang.Exception) {
            val params = Bundle()
            params.putString("device_id", App.device_id)
            params.putString("exception", "MA10$e")
            //  mFirebaseAnalytics.logEvent("app_param_error", params)
        }
        return haveConnectedWifi || haveConnectedMobile
    }

    //***  function to Stop VPN  ***//
    fun stop_vpn() {
        App.connection_status = 0
        OpenVPNService.abortConnectionVPN = true
        ProfileManager.setConntectedVpnProfileDisconnected(this)
        val frag = supportFragmentManager.findFragmentByTag(HomeFragment::class.java.simpleName) as? HomeFragment
        if (mService != null) {
            try {
                mService?.stopVPN(false)
            } catch (e: RemoteException) {
                val params = Bundle()
                params.putString("device_id", App.device_id)
                params.putString("exception", "MA18$e")
            }
            try {
                pm = ProfileManager.getInstance(this)
                vp = pm.getProfileByName(Build.MODEL)
                pm.removeProfile(this, vp)
            } catch (e: Exception) {
                val params = Bundle()
                params.putString("device_id", App.device_id)
                params.putString("exception", "MA17$e")
            }
            frag?.finalConnectionStopped()
        }
    }

    //***  function to Start VPN  ***//
    private fun start_vpn(VPNFile: String?) {
        val sp_settings: SharedPreferences = getSharedPreferences("daily_usage", 0)
        val connection_today = sp_settings.getLong(TODAY + "_connections", 0)
        val connection_total = sp_settings.getLong("total_connections", 0)
        val editor = sp_settings.edit()
        editor.putLong(TODAY + "_connections", connection_today + 1)
        editor.putLong("total_connections", connection_total + 1)
        editor.apply()
        var params = Bundle()
        params.putString("device_id", App.device_id)
        params.putString("city", City)
        // mFirebaseAnalytics.logEvent("app_param_country", params)
        App.connection_status = 1
        try {
            inputStream = null
            bufferedReader = null
            try {
                assert(VPNFile != null)
                inputStream = ByteArrayInputStream(VPNFile!!.toByteArray(Charset.forName("UTF-8")))
            } catch (e: java.lang.Exception) {
                params = Bundle()
                params.putString("device_id", App.device_id)
                params.putString("exception", "MA11$e")
                // mFirebaseAnalytics.logEvent("app_param_error", params)
            }
            try { // M8
                assert(inputStream != null)
                bufferedReader = BufferedReader(InputStreamReader(inputStream /*, Charset.forName("UTF-8")*/))
            } catch (e: java.lang.Exception) {
                params = Bundle()
                params.putString("device_id", App.device_id)
                params.putString("exception", "MA12$e")
                // mFirebaseAnalytics.logEvent("app_param_error", params)
            }
            cp = ConfigParser()
            try {
                cp.parseConfig(bufferedReader)
            } catch (e: java.lang.Exception) {
                params = Bundle()
                params.putString("device_id", App.device_id)
                params.putString("exception", "MA13$e")
                // mFirebaseAnalytics.logEvent("app_param_error", params)
            }
            vp = cp.convertProfile()
            vp.mAllowedAppsVpnAreDisallowed = true
            val En = EncryptData()
            val AppValues = getSharedPreferences("app_values", 0)
            val AppDetailsValues = En.decrypt(AppValues.getString("app_details", "NA"))
            try {
                val json_response = JSONObject(AppDetailsValues)
                val jsonArray = json_response.getJSONArray("blocked")
                for (i in 0 until jsonArray.length()) {
                    val json_object = jsonArray.getJSONObject(i)
                    vp.mAllowedAppsVpn.add(json_object.getString("app"))
                    Log.e("packages", json_object.getString("app"))
                }
            } catch (e: JSONException) {
                params = Bundle()
                params.putString("device_id", App.device_id)
                params.putString("exception", "MA14$e")
                // mFirebaseAnalytics.logEvent("app_param_error", params)
            }
            try {
                vp.mName = Build.MODEL
            } catch (e: java.lang.Exception) {
                params = Bundle()
                params.putString("device_id", App.device_id)
                params.putString("exception", "MA15$e")
                //  mFirebaseAnalytics.logEvent("app_param_error", params)
            }
            vp.mUsername = Data.FileUsername
            vp.mPassword = Data.FilePassword
            try {
                pm = ProfileManager.getInstance(this@MainActivity1)
                pm.addProfile(vp)
                pm.saveProfileList(this@MainActivity1)
                pm.saveProfile(this@MainActivity1, vp)
                vp = pm.getProfileByName(Build.MODEL)
                val intent = Intent(applicationContext, LaunchVPN::class.java)
                intent.putExtra(LaunchVPN.EXTRA_KEY, vp.getUUID().toString())
                intent.action = Intent.ACTION_MAIN
                startActivity(intent)
                App.isStart = false
            } catch (e: java.lang.Exception) {
                params = Bundle()
                params.putString("device_id", App.device_id)
                params.putString("exception", "MA16$e")
                //  mFirebaseAnalytics.logEvent("app_param_error", params)
            }
        } catch (e: java.lang.Exception) {
            params = Bundle()
            params.putString("device_id", App.device_id)
            params.putString("exception", "MA17$e")
            // mFirebaseAnalytics.logEvent("app_param_error", params)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    //***  function to update the state of VPN  ***//
    override fun updateState(state: String, logmessage: String?, localizedResId: Int, level: ConnectionStatus?) {
        runOnUiThread {
            if (state == "CONNECTED") {
                App.isStart = true
                App.connection_status = 2
                EnableConnectButton = true
            }
        }
    }

    override fun setConnectedVPN(uuid: String?) {}


    override fun updateByteCount(ins: Long, outs: Long, diffIns: Long, diffOuts: Long) {
        /* val Total = ins + outs
         runOnUiThread { // size
             if (Total < 1000) {
                 tv_data_text.setText("1KB")
                 tv_data_name.setText("USED")
             } else if (Total >= 1000 && Total <= 1000000) {
                 tv_data_text.setText((Total / 1000).toString() + "KB")
                 tv_data_name.setText("USED")
             } else {
                 tv_data_text.setText((Total / 1000000).toString() + "MB")
                 tv_data_name.setText("USED")
             }
         }*/
    }


    private fun setUpActivityMain() {
        val Today = Calendar.getInstance().time
        val df = SimpleDateFormat("dd-MMM-yyyy")
        TODAY = df.format(Today)
        tv_message_top_text = findViewById<TextView>(R.id.tv_message_top_text)

        // ui refresh
        thread = object : Thread() {
            var ShowData = true
            var ShowAnimation = true
            override fun run() {
                try {
                    while (!thread.isInterrupted()) {
                        sleep(500)
                        runOnUiThread {
                            // set country flag
                            if (App.abortConnection) {
                                App.abortConnection = false
                                if (App.connection_status != 2) {
                                    App.CountDown = 1
                                }
                                if (App.connection_status == 2) {
                                    try {
                                        stop_vpn()
                                        try {
                                            ConnectionTimer.cancel()
                                        } catch (e: java.lang.Exception) {
                                            val params = Bundle()
                                            params.putString("device_id", App.device_id)
                                            params.putString("exception", "MA7$e")
                                            // mFirebaseAnalytics.logEvent("app_param_error", params)
                                        }
                                        App.ShowDailyUsage = true
                                    } catch (e: java.lang.Exception) {
                                        val params = Bundle()
                                        params.putString("device_id", App.device_id)
                                        params.putString("exception", "MA8$e")
                                        // mFirebaseAnalytics.logEvent("app_param_error", params)
                                    }
                                    App.isStart = false
                                }
                            }
                            when (Image) {

                            }

                            // set connection button
                            if (hasFile) {
                                val frag = supportFragmentManager.findFragmentByTag(HomeFragment::class.java.simpleName) as? HomeFragment
                                if (App.connection_status == 0) {
                                    // disconnected
                                    frag?.setConnectionButtonStatus("Connect")
                                } else if (App.connection_status == 1) {
                                    // connecting
                                    if (EnableConnectButton) {
                                        frag?.setConnectionButtonStatus("Cancel")
                                    } else {
                                        frag?.setConnectionButtonStatus("Connecting")
                                    }
                                } else if (App.connection_status == 2) {
                                    // connected
                                    frag?.setConnectionButtonStatus("Disconnect")
                                } else if (App.connection_status == 3) {
                                    // connected
                                    frag?.setConnectionButtonStatus("Remove VPN Apps")
                                }
                            }

                            // set message text
                            if (hasFile) {
                                val frag = supportFragmentManager.findFragmentByTag(HomeFragment::class.java.simpleName) as? HomeFragment
                                if (hasInternetConnection()) {
                                    if (App.connection_status == 0) {
                                        // disconnected
                                        frag?.setConnectionText("Status: Not Connected")
                                        frag?.connectionStopped()
                                    } else if (App.connection_status == 1) {

                                        frag?.setConnectionText("Status: Connecting...")

                                    } else if (App.connection_status == 2) {
                                        // connected
                                        frag?.setConnectionText("Status: Connected")

                                    } else if (App.connection_status == 3) {
                                        // connected
                                        frag?.setConnectionText("Dangerous VPN apps found")
                                    }
                                } else {
                                    frag?.setConnectionText("Connection is not available")
                                }
                            }

                            // show data limit
                            if (ShowData) {
                                ShowData = false
                                if (App.connection_status == 0) {
                                    //  val handlerData = Handler()
                                    // handlerData.postDelayed({ startAnimation(this@MainActivity, R.id.ll_main_today, R.anim.slide_up_800, true) }, 1000)
                                } else if (App.connection_status == 1) {
                                    //  val handlerData = Handler()
                                    //  handlerData.postDelayed({ startAnimation(this@MainActivity, R.id.ll_main_today, R.anim.slide_up_800, true) }, 1000)
                                } else if (App.connection_status == 2) {
                                    //  val handlerData = Handler()
                                    //  handlerData.postDelayed({ startAnimation(this@MainActivity, R.id.ll_main_data, R.anim.slide_up_800, true) }, 1000)
                                } else if (App.connection_status == 3) {
                                    // connected
                                    //  val handlerData = Handler()
                                    // handlerData.postDelayed({ startAnimation(this@MainActivity, R.id.ll_main_today, R.anim.slide_up_800, true) }, 1000)
                                }
                            }

                            // get daily usage
                            if (hasFile) {
                                if (App.connection_status == 0) {
                                    // disconnected
                                    if (App.ShowDailyUsage) {
                                        App.ShowDailyUsage = false
                                        val PREF_USAGE = "daily_usage"
                                        val settings = getSharedPreferences(PREF_USAGE, 0)
                                        val long_usage_today = settings.getLong(TODAY, 0)
                                        if (long_usage_today < 1000) {
                                            // tv_data_today_text.setText("1KB")
                                        } else if (long_usage_today >= 1000 && long_usage_today <= 1000000) {
                                            // tv_data_today_text.setText((long_usage_today / 1000).toString() + "KB")
                                        } else {
                                            //  tv_data_today_text.setText((long_usage_today / 1000000).toString() + "MB")
                                        }
                                    }
                                }
                            }

                            // show animation
                            if (hasFile) {
                                if (ShowAnimation) {
                                    ShowAnimation = false
                                    if (App.connection_status == 0) {
                                        // disconnected
                                        /*startAnimation(this@MainActivity, R.id.la_animation, R.anim.fade_in_1000, true)
                                        la_animation.cancelAnimation()
                                        la_animation.setAnimation(R.raw.ninjainsecure)
                                        la_animation.playAnimation()*/
                                    } else if (App.connection_status == 1) {
                                        // connecting
                                        /*startAnimation(this@MainActivity, R.id.la_animation, R.anim.fade_in_1000, true)
                                        la_animation.cancelAnimation()
                                        la_animation.setAnimation(R.raw.conneting)
                                        la_animation.playAnimation()*/
                                    } else if (App.connection_status == 3) {
                                        // connected
                                        /*startAnimation(this@MainActivity, R.id.la_animation, R.anim.fade_in_1000, true)
                                        la_animation.cancelAnimation()
                                        la_animation.setAnimation(R.raw.ninjainsecure)
                                        la_animation.playAnimation()*/
                                    }
                                }
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    val params = Bundle()
                    params.putString("device_id", App.device_id)
                    params.putString("exception", "MA9$e")
                    // mFirebaseAnalytics.logEvent("app_param_error", params)
                }
            }
        }
        thread.start()
    }

    //***  function to start/stop VPN  ***//
    fun startStopVPNInitial() {
        val r = Runnable {
            if (!App.isStart) {
                if (!hasFile) {
                    val Servers = Intent(this@MainActivity1, ServerActivity::class.java)
                    startActivity(Servers)
                    overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left)
                } else {
                    if (hasInternetConnection()) {
                        try {
                            start_vpn(File)
                            App.CountDown = 30
                            try {
                                ConnectionTimer = object : CountDownTimer(32000, 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                        App.CountDown = App.CountDown - 1

                                        if (App.connection_status == 2) {
                                            ConnectionTimer.cancel()
                                            val SharedAppDetails = getSharedPreferences("settings_data", 0)
                                            val Editor = SharedAppDetails.edit()
                                            Editor.putString("connection_time", App.CountDown.toString())
                                            Editor.apply()
                                            if (App.CountDown >= 20) {
                                                val settings = getSharedPreferences("settings_data", 0)

                                            }

                                        }
                                        if (App.CountDown <= 20) {
                                            EnableConnectButton = true
                                        }
                                        if (App.CountDown <= 1) {
                                            ConnectionTimer.cancel()

                                            try {
                                                stop_vpn()

                                                App.ShowDailyUsage = true
                                            } catch (e: java.lang.Exception) {
                                                val params = Bundle()
                                                params.putString("device_id", App.device_id)
                                                params.putString("exception", "MA3$e")
                                                // mFirebaseAnalytics.logEvent("app_param_error", params)
                                            }
                                            App.isStart = false
                                        }
                                    }

                                    override fun onFinish() {}
                                }
                            } catch (e: java.lang.Exception) {
                                val params = Bundle()
                                params.putString("device_id", App.device_id)
                                params.putString("exception", "MA4$e")
                                // mFirebaseAnalytics.logEvent("app_param_error", params)
                            }
                            ConnectionTimer.start()
                            EnableConnectButton = false
                            App.isStart = true
                        } catch (e: java.lang.Exception) {
                            val params = Bundle()
                            params.putString("device_id", App.device_id)
                            params.putString("exception", "MA5$e")
                            // mFirebaseAnalytics.logEvent("app_param_error", params)
                        }
                    }
                }
            } else {
                if (EnableConnectButton) {
                    try {
                        stop_vpn()
                        try {
                            ConnectionTimer.cancel()
                        } catch (ignored: java.lang.Exception) {
                            //new SyncFunctions(MainActivity.this, "MA6 " +  e.toString()).set_error_log();
                        }

                        val settings = getSharedPreferences("settings_data", 0)
                        val ConnectionTime = settings.getString("connection_time", "0")
                        if (java.lang.Long.valueOf(ConnectionTime) >= 20) {
                            val Editor = settings.edit()
                            Editor.putString("connection_time", "0")
                            Editor.apply()
                        }
                        App.ShowDailyUsage = true
                    } catch (e: java.lang.Exception) {
                        val params = Bundle()
                        params.putString("device_id", App.device_id)
                        params.putString("exception", "MA6$e")
                        //  mFirebaseAnalytics.logEvent("app_param_error", params)
                    }
                    App.isStart = false
                }
            }
        }
        r.run()
    }

}