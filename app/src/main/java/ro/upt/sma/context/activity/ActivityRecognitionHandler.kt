package ro.upt.sma.context.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient

class ActivityRecognitionHandler(context: Context) {

    private val client: ActivityRecognitionClient = ActivityRecognition.getClient(context)

    fun registerPendingIntent(): PendingIntent {

        // Create a pending intent for ActivityRecognitionService and register for updates to activity recognition client.

        val intent = Intent(client.applicationContext, ActivityRecognitionService::class.java).apply {

        }

        val newPendingIntent = PendingIntent.getService(client.applicationContext, 0, intent, 0)

        client.requestActivityUpdates(0,newPendingIntent)

        return newPendingIntent
    }

    fun unregisterPendingIntent(pendingIntent: PendingIntent) {
        client.removeActivityUpdates(pendingIntent)
    }

}
