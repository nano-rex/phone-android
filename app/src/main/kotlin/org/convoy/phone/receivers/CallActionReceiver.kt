package org.convoy.phone.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.convoy.phone.activities.CallActivity
import org.convoy.phone.helpers.ACCEPT_CALL
import org.convoy.phone.helpers.CallManager
import org.convoy.phone.helpers.DECLINE_CALL

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACCEPT_CALL -> {
                context.startActivity(CallActivity.getStartIntent(context))
                CallManager.accept()
            }

            DECLINE_CALL -> CallManager.reject()
        }
    }
}
