package neth.iecal.questphone.data.remote

import android.content.Context

object DevModeManager {
    private const val PREFS = "dev_mode"
    private const val KEY_ACTIVE = "active"
    private const val KEY_SELECTED = "selected" // whether user has chosen a mode at least once

    fun isActive(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_ACTIVE, false)
    }

    fun hasSelected(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_SELECTED, false)
    }

    fun setActive(context: Context, active: Boolean) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_ACTIVE, active).putBoolean(KEY_SELECTED, true).apply()
    }
}
