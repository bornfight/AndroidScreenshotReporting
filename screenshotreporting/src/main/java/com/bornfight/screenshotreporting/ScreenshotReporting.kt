package com.bornfight.screenshotreporting

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.*
import com.jraska.falcon.Falcon


/**
 * Created by tomislav on 18/09/2017.
 * Updated and converted to Kotlin by lleopoldovic on 06/08/2019.
 */

class ScreenshotReporting private constructor(private val email: String, private val subject: String) {

    private val filter = IntentFilter(ACTION_SCREENSHOT)
    private var mActivityRef: WeakReference<Activity>? = null
    private val mScreenshotReceiver = ScreenshotReceiver()
    private var mScreenshotReceiverRegistered = false

    private fun registerApplication(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, bundle: Bundle?) { }
            override fun onActivityStarted(activity: Activity) { }
            override fun onActivityResumed(activity: Activity) {
                registerActivity(activity)
            }
            override fun onActivityPaused(activity: Activity) {
                unregisterActivity()
            }
            override fun onActivityStopped(activity: Activity) { }
            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle?) { }
            override fun onActivityDestroyed(activity: Activity) { }
        })
    }

    private fun registerActivity(activity: Activity) {
        mActivityRef = WeakReference(activity)
        mActivityRef?.get()?.let { mActivity ->
            mActivity.registerReceiver(mScreenshotReceiver, filter)
            mScreenshotReceiverRegistered = true
            setScreenshotNotification(mActivity)
        }
    }

    private fun unregisterActivity() {
        mActivityRef?.get()?.let { activity ->
            val notificationManager =
                activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(1 /* ID of notification */)

            if (mScreenshotReceiverRegistered) {
                activity.unregisterReceiver(mScreenshotReceiver)
                mScreenshotReceiverRegistered = false
            }
        }
        mActivityRef?.clear()
        mActivityRef = null
    }

    private fun setScreenshotNotification(activity: Activity) {
        val intent1 = Intent(ACTION_SCREENSHOT)
        val pendingIntent = PendingIntent.getBroadcast(
            activity, 0 /* Request code */, intent1,
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel("ssreporting", "Screenshot Reporting", NotificationManager.IMPORTANCE_LOW)
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(activity, "ssreporting")
            .setSmallIcon(getAppIconId(activity))
            .setContentTitle(getApplicationName(activity) + " SCREENSHOT REPORT")
            .setContentText("Tap to send report with screenshot.")
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0))

        notificationManager.notify(1 /* ID of notification */, notificationBuilder.build())
    }

    private fun getApplicationName(context: Context): String {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else context.getString(stringId)
    }

    private fun getAppIconId(context: Context): Int {
        return context.applicationInfo.icon
    }

    private fun takeBugReportScreenshot(activity: Activity) {
        val fileName = android.text.format.DateFormat.format("yyyy_MM_dd_hh_mm_ss", Date()).toString()

        try {
            // Image naming and path to include sd card, appending name you choose for file.
            val mPath = activity.externalCacheDir?.toString() + "/" + fileName + ".jpg"

            val bitmap = Falcon.takeScreenshotBitmap(activity)
            val imageFile = File(mPath)

            val outputStream = FileOutputStream(imageFile)
            val quality = 90
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream as OutputStream)
            outputStream.flush()
            outputStream.close()

            sendScreenshot(activity, imageFile)
        } catch (e: Throwable) {
            // Several errors may come out with file handling or OOM.
            e.printStackTrace()
        }
    }

    private fun sendScreenshot(context: Context, imageFile: File) {
        // https://support.teamwork.com/projects/tasks/posting-tasks-via-email
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "application/image"
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "$subject #android !Task_name" )
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Task description..")
        emailIntent.putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(context, context.packageName + ".provider", imageFile)
        )
        context.startActivity(emailIntent)
    }


    private inner class ScreenshotReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mActivityRef?.get()?.let {
                takeBugReportScreenshot(it)
            }
        }
    }

    companion object {
        private const val ACTION_SCREENSHOT = BuildConfig.APPLICATION_ID + ".SCREENSHOT"

        /**
         * Init Screenshot reporting in Application class
         * @param application main App class
         * @param email Teamwork task-list email for this app
         * @param subject A person to be tagged for tasks ("@username")
         */
        @JvmStatic
        fun init(application: Application, email: String, subject: String) {
            ScreenshotReporting(email, subject).registerApplication(application)
        }
    }

}