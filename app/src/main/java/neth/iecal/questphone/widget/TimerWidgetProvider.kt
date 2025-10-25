package neth.iecal.questphone.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.content.res.Configuration
import android.util.Log
import android.widget.RemoteViews
import neth.iecal.questphone.R
import neth.iecal.questphone.data.timer.TimerMode
import neth.iecal.questphone.data.timer.TimerService
import java.time.Duration

class TimerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Widget enabled, starting update service")
        // Start the update service when the first widget is added
        context.startService(Intent(context, TimerWidgetUpdateService::class.java))
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Widget disabled, stopping update service")
        // Stop the update service when the last widget is removed
        context.stopService(Intent(context, TimerWidgetUpdateService::class.java))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_WIDGET_UPDATE -> {
                val timerText = intent.getStringExtra(EXTRA_TIMER_TEXT) ?: "--:--"
                val timerMode = intent.getSerializableExtra(EXTRA_TIMER_MODE) as? TimerMode ?: TimerMode.INACTIVE
                val isBreakOvertime = intent.getBooleanExtra(EXTRA_IS_BREAK_OVERTIME, false)
                val isEnabled = intent.getBooleanExtra(EXTRA_IS_ENABLED, false)
                
                Log.d(TAG, "Received update: $timerText, mode: $timerMode, enabled: $isEnabled")
                
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, TimerWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, timerText, timerMode, isBreakOvertime, isEnabled)
                }
            }
            ACTION_INFO_CLICKED -> {
                Log.d(TAG, "Info button clicked")
                val serviceIntent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_TOGGLE_INFO_MODE
                }
                context.startService(serviceIntent)
            }
            ACTION_TIMER_CLICKED -> {
                val timerMode = intent.getSerializableExtra(EXTRA_TIMER_MODE) as? TimerMode ?: TimerMode.INACTIVE
                Log.d(TAG, "Timer clicked in mode: $timerMode")
                
                when (timerMode) {
                    TimerMode.UNPLANNED_BREAK -> {
                        // Stop unplanned break
                        val serviceIntent = Intent(context, TimerService::class.java).apply {
                            action = TimerService.ACTION_STOP_UNPLANNED_BREAK
                        }
                        context.startService(serviceIntent)
                    }
                    TimerMode.BREAK -> {
                        // End break early
                        val serviceIntent = Intent(context, TimerService::class.java).apply {
                            action = TimerService.ACTION_END_BREAK_EARLY
                        }
                        context.startService(serviceIntent)
                    }
                    TimerMode.QUEST_COUNTDOWN, TimerMode.INACTIVE -> {
                        // Show unplanned break dialog
                        val dialogIntent = Intent(context, WidgetUnplannedBreakDialogActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(dialogIntent)
                    }
                    else -> {
                        Log.d(TAG, "Timer click ignored in mode: $timerMode")
                    }
                }
            }
            ACTION_ADD_CLICKED -> {
                val timerMode = intent.getSerializableExtra(EXTRA_TIMER_MODE) as? TimerMode ?: TimerMode.INACTIVE
                val isBreakOvertime = intent.getBooleanExtra(EXTRA_IS_BREAK_OVERTIME, false)
                Log.d(TAG, "Add button clicked in mode: $timerMode, isBreakOvertime: $isBreakOvertime")
                
                if (timerMode == TimerMode.BREAK || (timerMode == TimerMode.OVERTIME && isBreakOvertime)) {
                    // Show quest selection dialog
                    val dialogIntent = Intent(context, WidgetQuestSelectionDialogActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(dialogIntent)
                } else {
                    // Show add time dialog
                    val dialogIntent = Intent(context, WidgetAddTimeDialogActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(dialogIntent)
                }
            }
        }
    }

    companion object {
        private const val TAG = "TimerWidgetProvider"
        
        const val ACTION_WIDGET_UPDATE = "neth.iecal.questphone.widget.ACTION_WIDGET_UPDATE"
        const val ACTION_INFO_CLICKED = "neth.iecal.questphone.widget.ACTION_INFO_CLICKED"
        const val ACTION_TIMER_CLICKED = "neth.iecal.questphone.widget.ACTION_TIMER_CLICKED"
        const val ACTION_ADD_CLICKED = "neth.iecal.questphone.widget.ACTION_ADD_CLICKED"
        
        const val EXTRA_TIMER_TEXT = "extra_timer_text"
        const val EXTRA_TIMER_MODE = "extra_timer_mode"
        const val EXTRA_IS_ENABLED = "extra_is_enabled"
        const val EXTRA_IS_BREAK_OVERTIME = "extra_is_break_overtime"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            timerText: String = "--:--",
            timerMode: TimerMode = TimerMode.INACTIVE,
            isBreakOvertime: Boolean = false,
            isAddButtonEnabled: Boolean = false
        ) {
            val views = RemoteViews(context.packageName, R.layout.timer_widget_layout)
            
            // Update timer text
            views.setTextViewText(R.id.widget_timer_text, timerText)
            
            // Detect system dark theme for launcher integration
            val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            // Update timer color based on mode (fallback respects theme)
            val timerColor = when (timerMode) {
                TimerMode.QUEST_COUNTDOWN -> Color.parseColor("#1976D2") // Blue
                TimerMode.OVERTIME -> Color.parseColor("#DC143C") // Crimson
                TimerMode.BREAK -> Color.parseColor("#4CAF50") // Green
                TimerMode.UNPLANNED_BREAK -> Color.parseColor("#808080") // Gray
                TimerMode.INFO -> Color.parseColor("#8A2BE2") // Purple
                TimerMode.UNLOCK -> Color.parseColor("#FFEB3B") // Yellow
                else -> if (isDark) Color.WHITE else Color.BLACK
            }
            views.setTextColor(R.id.widget_timer_text, timerColor)

            // Icon tint to match theme/background
            val baseIconColor = if (isDark) Color.WHITE else Color.BLACK
            views.setInt(R.id.widget_info_button, "setColorFilter", baseIconColor)
            views.setInt(R.id.widget_add_button, "setColorFilter", baseIconColor)
            
            // Set click listeners
            val infoIntent = Intent(context, TimerWidgetProvider::class.java).apply {
                action = ACTION_INFO_CLICKED
            }
            val infoPendingIntent = PendingIntent.getBroadcast(
                context, 0, infoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_info_button, infoPendingIntent)
            
            val timerIntent = Intent(context, TimerWidgetProvider::class.java).apply {
                action = ACTION_TIMER_CLICKED
                putExtra(EXTRA_TIMER_MODE, timerMode)
            }
            val timerPendingIntent = PendingIntent.getBroadcast(
                context, 1, timerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_timer_text, timerPendingIntent)
            
            val addIntent = Intent(context, TimerWidgetProvider::class.java).apply {
                action = ACTION_ADD_CLICKED
                putExtra(EXTRA_TIMER_MODE, timerMode)
                putExtra(EXTRA_IS_BREAK_OVERTIME, isBreakOvertime)
            }
            val addPendingIntent = PendingIntent.getBroadcast(
                context, 2, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_add_button, addPendingIntent)
            
            // Update button states
            val infoEnabled = timerMode in listOf(
                TimerMode.QUEST_COUNTDOWN,
                TimerMode.BREAK,
                TimerMode.INFO
            )
            views.setInt(R.id.widget_info_button, "setAlpha", if (infoEnabled) 255 else 77)
            
            views.setInt(R.id.widget_add_button, "setAlpha", if (isAddButtonEnabled) 255 else 77)
            
            // Tell the AppWidgetManager to perform an update on the current widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
