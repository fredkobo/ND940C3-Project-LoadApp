package com.udacity

import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import com.udacity.databinding.ActivityMainBinding
import com.udacity.DownloadStatus.UNKNOWN


class MainActivity : AppCompatActivity() {

    private var downloadID: Long = 0

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action
    private lateinit var binding: ActivityMainBinding
    private var downloadObserver: ContentObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.apply {
            setSupportActionBar(toolbar)
            registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            id?.let {
                val downloadStatus = getDownloadManager().queryStatus(it)
                Log.d(TAG, "Download $it completed with status: ${downloadStatus.statusText}")
                deregisterObserver()
                // todo make notification notify
            }
        }
    }

    private fun download() {
        with(getDownloadManager()) {
            downloadID.takeIf { it != 0L }?.run {
                val downloadsCancelled = remove(downloadID)
                deregisterObserver()
                downloadID = 0L
                Log.d(TAG, "Number of downloads cancelled: $downloadsCancelled")
            }

            val request = DownloadManager.Request(Uri.parse(URL))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadID = enqueue(request)

            registerContentObserver()
        }
    }

    private fun DownloadManager.registerContentObserver() {
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                downloadObserver?.run { queryProgress() }
            }
        }.also {
            downloadObserver = it
            contentResolver.registerContentObserver(
                "content://downloads".toUri(),
                true,
                downloadObserver!!
            )
        }
    }

    private fun DownloadManager.queryProgress() {
        query(DownloadManager.Query().setFilterById(downloadID)).use {
            with(it) {
                if (this != null && moveToFirst()) {
                    val id = getInt(getColumnIndex(DownloadManager.COLUMN_ID))
                    when (getInt(getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_FAILED -> {
                            Log.d(TAG, "Download [$id] failed")
                            binding.mainContent.customButton.changeButtonState(ButtonState.Completed)
                        }
                        DownloadManager.STATUS_PAUSED -> {
                            Log.d(TAG, "Download [$id] paused")
                        }
                        DownloadManager.STATUS_PENDING -> {
                            Log.d(TAG, "Download [$id] pending")
                        }
                        DownloadManager.STATUS_RUNNING -> {
                            Log.d(TAG, "Download [$id] running")
                            binding.mainContent.customButton.changeButtonState(ButtonState.Loading)
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            Log.d(TAG, "Download [$id] successful")
                            binding.mainContent.customButton.changeButtonState(ButtonState.Completed)
                        }
                    }
                }
            }
        }
    }

    private fun DownloadManager.queryStatus(id: Long): DownloadStatus {
        query(DownloadManager.Query().setFilterById(id)).use {
            with(it) {
                if (this != null && moveToFirst()) {
                    return when (getInt(getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.SUCCESSFUL
                        DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                        else -> UNKNOWN
                    }
                }
                return UNKNOWN
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        deregisterObserver()
    }

    private fun deregisterObserver() {
        downloadObserver?.let {
            contentResolver.unregisterContentObserver(it)
            downloadObserver = null
        }
    }

    fun onLoadingButtonClick(view: View) {
        when (binding.mainContent.downloadRadioGroup.checkedRadioButtonId) {
            View.NO_ID ->
                Toast.makeText(this, "Please select the file to download", Toast.LENGTH_SHORT)
                    .show()
            else -> {
                download()
            }
        }
    }

    fun Context.getDownloadManager(): DownloadManager = ContextCompat.getSystemService(
        this,
        DownloadManager::class.java
    ) as DownloadManager

    companion object {
        private const val URL =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
        private const val CHANNEL_ID = "channelId"
        private const val TAG = "MainActivity"
    }
}
