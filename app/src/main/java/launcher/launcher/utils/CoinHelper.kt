package launcher.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class CoinHelper(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )
    fun getCoinCount():Int{
        return sharedPreferences.getInt(COIN_COUNT_KEY,10)
    }

    fun incrementCoinCount(value: Int){
        sharedPreferences.edit { putInt(COIN_COUNT_KEY, getCoinCount() + value) }
    }
    fun decrementCoinCount(value: Int){
        sharedPreferences.edit { putInt(COIN_COUNT_KEY, getCoinCount() - value) }
    }
    fun canUserAffordPurchase(value:Int):Boolean{
        return getCoinCount() + value >= 0
    }


    companion object {
        private const val PREF_NAME = "coin_count"
        private const val COIN_COUNT_KEY = "quest_list"
    }
}