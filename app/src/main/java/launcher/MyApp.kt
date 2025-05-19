package launcher

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import launcher.launcher.data.game.User
import launcher.launcher.data.game.saveUserInfo
import launcher.launcher.utils.VibrationHelper
import launcher.launcher.utils.isOnline
import launcher.launcher.utils.triggerSync

class MyApp : Application() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreate() {
        super.onCreate()
        User.init(this)
        VibrationHelper.init(this)


        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Trigger sync when network becomes available
                triggerSync(applicationContext)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        if (isOnline()) {
            triggerSync(applicationContext)
        }
    }
}