package com.mona.kotlinpractice.mediaplayer

import android.app.Service
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.content.Intent
import android.os.IBinder
import android.app.NotificationManager
import android.support.v4.app.NotificationCompat
import android.app.PendingIntent
import android.content.Context
import android.os.Binder
import android.support.annotation.Nullable
import android.util.Log
import android.widget.RemoteViews
import com.mona.kotlinpractice.R


class MediaplayerBinderService : Service() {
    private val DELETE_PENDINGINTENT_REQUESTCODE = 1022
    private val CONTENT_PENDINGINTENT_REQUESTCODE = 1023
    private val NEXT_PENDINGINTENT_REQUESTCODE = 1024
    private val PLAY_PENDINGINTENT_REQUESTCODE = 1025
    private val STOP_PENDINGINTENT_REQUESTCODE = 1026
    private val NOTIFICATION_PENDINGINTENT_ID = 1// 是用来标记Notifaction，可用于更新，删除Notifition

    private var mediaPlayer: MediaPlayer? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private var notificationManager: NotificationManager? = null
    private var builder: NotificationCompat.Builder? = null
    private var views: RemoteViews? = null

    private val musics = arrayOf("http://ibooker.cc/ibooker/file_packet/musics/1234.mp3", "http://ibooker.cc/ibooker/file_packet/musics/2345.mp3") // 设置音频资源（网络）
    private var current_item = 0
    private var isPause = false

    override fun onCreate() {
        super.onCreate()

        Log.d("MediaPlayerBService", "OnCreate")

        // 初始化MediaPlayer
        initMediaPlayer()

        // 设置点击通知结果
        val intent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(this, CONTENT_PENDINGINTENT_REQUESTCODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val delIntent = Intent(this, MediaPlayerService::class.java)
        val delPendingIntent = PendingIntent.getService(this, DELETE_PENDINGINTENT_REQUESTCODE, delIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // 自定义布局
        views = RemoteViews(packageName, R.layout.layout_mediaplayer)

        // 下一首
        val intentNext = Intent("nextMusic1")
        val nextPendingIntent = PendingIntent.getBroadcast(this, NEXT_PENDINGINTENT_REQUESTCODE, intentNext, PendingIntent.FLAG_CANCEL_CURRENT)
        views!!.setOnClickPendingIntent(R.id.tv_next, nextPendingIntent)

        // 暂停/播放
        val intentPlay = Intent("playMusic1")
        val playPendingIntent = PendingIntent.getBroadcast(this, PLAY_PENDINGINTENT_REQUESTCODE, intentPlay, PendingIntent.FLAG_CANCEL_CURRENT)
        views!!.setOnClickPendingIntent(R.id.tv_pause, playPendingIntent)

        // 停止
        val intentStop = Intent("stopMusic1")
        val stopPendingIntent = PendingIntent.getBroadcast(this, STOP_PENDINGINTENT_REQUESTCODE, intentStop, PendingIntent.FLAG_CANCEL_CURRENT)
        views!!.setOnClickPendingIntent(R.id.tv_cancel, stopPendingIntent)

        builder = NotificationCompat.Builder(this)
                // 设置小图标
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                // 设置标题
                .setContentTitle("ZMediaPlayer")
                // 设置内容
                .setContentText("内容")
                // 点击通知后自动清除
                .setAutoCancel(false)
                // 设置点击通知效果
                .setContentIntent(contentPendingIntent)
                // 设置删除时候出发的动作
                .setDeleteIntent(delPendingIntent)
                // 自定义视图
                .setContent(views)

        // 获取NotificationManager实例
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 前台服务
        startForeground(NOTIFICATION_PENDINGINTENT_ID, builder!!.build())
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        Log.d("MediaPlayerBService", "onBind")
        return mediaplayerBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d("MediaPlayerBService", "onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MediaPlayerBService", "onDestroy")
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        if (wifiLock != null && wifiLock!!.isHeld)
            wifiLock!!.release()
        // 取消Notification
        if (notificationManager != null)
            notificationManager!!.cancel(NOTIFICATION_PENDINGINTENT_ID)
        stopForeground(true)
        // 停止服务
        stopSelf()
    }

    // 初始化MediaPlayer
    private fun initMediaPlayer() {
        if (mediaPlayer == null)
            mediaPlayer = MediaPlayer()

        // 设置音量，参数分别表示左右声道声音大小，取值范围为0~1
        mediaPlayer!!.setVolume(0.5f, 0.5f)

        // 设置是否循环播放
        mediaPlayer!!.isLooping = false

        // 设置设备进入锁状态模式-可在后台播放或者缓冲音乐-CPU一直工作
        mediaPlayer!!.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
        //        // 当播放的时候一直让屏幕变亮
        //        mediaPlayer.setScreenOnWhilePlaying(true);

        // 如果你使用wifi播放流媒体，你还需要持有wifi锁
        wifiLock = (applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager)
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "wifilock")
        wifiLock!!.acquire()

        mediaPlayer!!.setOnPreparedListener {
            mediaPlayer!!.start()
            isPause = false
        }

        // 设置播放错误监听
        mediaPlayer!!.setOnErrorListener { mediaPlayer, i, i1 ->
            mediaPlayer.reset()
            false
        }

        // 设置播放完成监听
        mediaPlayer!!.setOnCompletionListener { nextMusic() }
    }

    // 播放
    fun play() {
        try {
            if (mediaPlayer == null)
                initMediaPlayer()
            if (isPause) {
                mediaPlayer!!.start()
                isPause = false
            } else {
                // 重置mediaPlayer
                mediaPlayer!!.reset()
                // 重新加载音频资源
                mediaPlayer!!.setDataSource(musics[current_item])
                // 准备播放（异步）
                mediaPlayer!!.prepareAsync()
            }

            updateNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    // 暂停
    fun pause() {
        mediaPlayer!!.pause()
        isPause = true
    }

    // Notification调用
    fun playMusic() {
        if (mediaPlayer != null) {
            if (!isPause) {
                mediaPlayer!!.pause()
                isPause = true
            } else {
                mediaPlayer!!.start()
                isPause = false
            }
        }
    }

    // 下一首
    fun nextMusic() {
        current_item++
        if (current_item >= musics.size)
            current_item = 0
        play()
    }

    // 上一首
    fun preMusic() {
        current_item--
        if (current_item < 0)
            current_item = musics.size - 1
        play()
    }

    // 停止
    fun stop() {
        //        mediaPlayer.stop();
        if (mediaPlayer!!.isPlaying)
            mediaPlayer!!.reset()
    }

    // 更新Notification
    private fun updateNotification() {
        if (views != null) {
            views!!.setTextViewText(R.id.tv_name, "音乐名$current_item")
            views!!.setTextViewText(R.id.tv_author, "作者$current_item")
            if (!isPause) {
                views!!.setTextViewText(R.id.tv_pause, "暂停")
            } else {
                views!!.setTextViewText(R.id.tv_pause, "播放")
            }
        }

        // 刷新notification
        notificationManager!!.notify(NOTIFICATION_PENDINGINTENT_ID, builder!!.build())
    }

    // 定义Binder类-当然也可以写成外部类
    private val mediaplayerBinder = MediaplayerBinder()

    inner class MediaplayerBinder : Binder() {
        val service: Service
            get() = this@MediaplayerBinderService
    }
}