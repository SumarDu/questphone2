package launcher

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import launcher.launcher.data.game.Pet
import launcher.launcher.data.game.User
import launcher.launcher.services.AppBlockerService
import launcher.launcher.utils.VibrationHelper
import launcher.launcher.utils.isOnline
import launcher.launcher.utils.triggerQuestSync
import launcher.launcher.utils.triggerStatsSync

class MyApp : Application() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    var appLockServiceInstance: AppBlockerService? = null

    override fun onCreate() {
        super.onCreate()

        User.init(this)
        VibrationHelper.init(this)
        Pet.init(this)

        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Trigger sync when network becomes available
                triggerQuestSync(applicationContext)
                triggerStatsSync(applicationContext)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        if (isOnline()) {
            triggerQuestSync(applicationContext)
        }
    }
}