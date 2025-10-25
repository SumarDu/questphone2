package neth.iecal.questphone.widget

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.timer.TimerMode
import neth.iecal.questphone.data.timer.TimerService
import neth.iecal.questphone.data.timer.TimerState
import java.time.Duration

/**
 * Service that monitors TimerService state and updates the widget in real-time
 */
class TimerWidgetUpdateService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var monitorJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TimerWidgetUpdateService created")
        startMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "TimerWidgetUpdateService started")
        if (monitorJob == null || monitorJob?.isActive == false) {
            startMonitoring()
        }
        return START_STICKY
    }
    
    private fun startMonitoring() {
        Log.d(TAG, "Starting timer state monitoring")
        monitorJob = serviceScope.launch {
            try {
                TimerService.timerState.collect { state ->
                    Log.d(TAG, "Timer state changed: mode=${state.mode}, time=${state.time}")
                    updateWidget(state)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring timer state", e)
            }
        }
    }
    
    private fun updateWidget(state: TimerState) {
        val timerText = formatDuration(state.time)
        val isAddButtonEnabled = isAddButtonEnabled(state)
        
        Log.d(TAG, "Updating widget: text=$timerText, mode=${state.mode}, addEnabled=$isAddButtonEnabled")
        
        // Show deferred reason dialog if requested
        if (state.requestUnplannedBreakReason) {
            val dialogIntent = Intent(this, WidgetDeferredReasonDialogActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(dialogIntent)
        }
        
        // Broadcast update to all widgets
        val updateIntent = Intent(this, TimerWidgetProvider::class.java).apply {
            action = TimerWidgetProvider.ACTION_WIDGET_UPDATE
            putExtra(TimerWidgetProvider.EXTRA_TIMER_TEXT, timerText)
            putExtra(TimerWidgetProvider.EXTRA_TIMER_MODE, state.mode)
            putExtra(TimerWidgetProvider.EXTRA_IS_BREAK_OVERTIME, state.isBreakOvertime)
            putExtra(TimerWidgetProvider.EXTRA_IS_ENABLED, isAddButtonEnabled)
        }
        sendBroadcast(updateIntent)
    }
    
    private fun isAddButtonEnabled(state: TimerState): Boolean {
        return when (state.mode) {
            TimerMode.UNPLANNED_BREAK -> false
            TimerMode.INFO -> false
            TimerMode.UNLOCK -> false
            TimerMode.OVERTIME -> !state.isDeepFocusLocking && !state.isBreakOvertime
            else -> !state.isDeepFocusLocking
        }
    }
    
    private fun formatDuration(duration: Duration): String {
        val seconds = duration.seconds
        val absSeconds = kotlin.math.abs(seconds)

        val positive = if (absSeconds >= 3600) {
            String.format(
                "%02d:%02d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60
            )
        } else {
            String.format(
                "%02d:%02d",
                (absSeconds % 3600) / 60,
                absSeconds % 60
            )
        }

        return if (seconds < 0) "-$positive" else positive
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "TimerWidgetUpdateService destroyed")
        monitorJob?.cancel()
        serviceScope.cancel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        private const val TAG = "TimerWidgetUpdateSvc"
    }
}
