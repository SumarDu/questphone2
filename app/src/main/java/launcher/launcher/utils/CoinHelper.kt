package launcher.launcher.utils

import android.content.Context
import android.content.SharedPreferences

class CoinHelper(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )
    fun getCoinCount():Int{
        return sharedPreferences.getInt(COIN_COUNT_KEY,10)
    }

    fun incrementCoinCount(value: Int){
        sharedPreferences.edit().putInt(COIN_COUNT_KEY,getCoinCount()+value).apply()
    }
    fun decrementCoinCount(value: Int){
        sharedPreferences.edit().putInt(COIN_COUNT_KEY,getCoinCount()-value).apply()
    }

    companion object {
        private const val PREF_NAME = "coin_count"
        private const val COIN_COUNT_KEY = "quest_list"
    }
}