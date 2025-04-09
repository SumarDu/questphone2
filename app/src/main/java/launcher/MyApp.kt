package launcher

import android.app.Application
import launcher.launcher.data.game.User
import launcher.launcher.data.game.saveUserInfo
import launcher.launcher.utils.VibrationHelper

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        User.init(this)
        VibrationHelper.init(this)
    }
}