package com.mona.kotlinpractice.mediaplayer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.app.NotificationManager
import android.support.v4.app.NotificationCompat
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.mona.kotlinpractice.MainActivity
import com.mona.kotlinpractice.R
import java.io.IOException


class MediaPlayerService : Service() {
    private val DELETE_PENDINGINTENT_REQUESTCODE = 1022
    private val CONTENT_PENDINGINTENT_REQUESTCODE = 1023
    private val NEXT_PENDINGINTENT_REQUESTCODE = 1024
    private val PLAY_PENDINGINTENT_REQUESTCODE = 1025
    private val STOP_PENDINGINTENT_REQUESTCODE = 1026
    private val NOTIFICATION_PENDINGINTENT_ID = 1// 是用来标记Notifaction，可用于更新，删除Notifition

    private var notificationManager: NotificationManager? = null
    private var builder: NotificationCompat.Builder? = null
    private var views: RemoteViews? = null
    private var playerReceiver: BroadcastReceiver? = null

    private var mediaPlayer: MediaPlayer? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private val musics = arrayOf("http://ibooker.cc/ibooker/file_packet/musics/1234.mp3", "http://ibooker.cc/ibooker/file_packet/musics/2345.mp3") // 设置音频资源（网络）
    private var current_item = 0
    private var isPause = false

    override fun onCreate() {
        super.onCreate()


        //        // 如果是放在assets文件中
        //        try {
        //            AssetFileDescriptor fd = getAssets().openFd("sound_music.mp3");
        //            mediaPlayer = new MediaPlayer();
        //            mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
        //        } catch (IOException e) {
        //            e.printStackTrace();
        //        }

        //        // 如果是放在SD卡中
        //        try {
        //            mediaPlayer = new MediaPlayer();
        //            String path = "/sdcard/sound_music.mp3";
        //            mediaPlayer.setDataSource(path);
        //        } catch (IOException e) {
        //            e.printStackTrace();
        //        }

        //        // 如果是放在raw文件中
        //        mediaPlayer = MediaPlayer.create(this, R.raw.sound_music);
        initMediaPlayer()


        // 设置点击通知结果
        val intent = Intent(this, MainActivity::class.java)
        /*
         * Context context：上下文对象
         * int requestCode：请求码
         * Intent intent：intent桥梁
         * int flags：
         * FLAG_CANCEL_CURRENT：如果当前系统中已经存在一个相同的 PendingIntent 对象，那么就将先将已有的 PendingIntent 取消，然后重新生成一个 PendingIntent 对象。
         * FLAG_NO_CREATE：如果当前系统中不存在相同的 PendingIntent 对象，系统将不会创建该 PendingIntent 对象而是直接返回 null 。
         * FLAG_ONE_SHOT：该 PendingIntent 只作用一次。
         * FLAG_UPDATE_CURRENT：如果系统中已存在该 PendingIntent 对象，那么系统将保留该 PendingIntent 对象，但是会使用新的 Intent 来更新之前 PendingIntent 中的 Intent 对象数据，例如更新 Intent 中的 Extras 。
         */
        val contentPendingIntent = PendingIntent.getActivity(this, CONTENT_PENDINGINTENT_REQUESTCODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val delIntent = Intent(this, MediaPlayerService::class.java)
        val delPendingIntent = PendingIntent.getService(this, DELETE_PENDINGINTENT_REQUESTCODE, delIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        //        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.test_bg);
        //        // 初始化Notification，Notification三要素：小图标、标题、内容
        //        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
        //                // 设置小图标，不展开时显示，当展开时现在在左侧
        //                .setSmallIcon(R.mipmap.ic_launcher)
        //                // 设置状态栏的显示的信息
        //                .setTicker("这是一个音频播放器")
        //                // 设置标题
        //                .setContentTitle("ZMediaPlayer")
        //                // 设置内容
        //                .setContentText("内容")
        //                // 设置通知时间，默认为系统发出通知的时间，通常不用设置
        //                .setWhen(System.currentTimeMillis())
        //                // 设置是否显示时间
        //                .setShowWhen(true)
        //                // 设置大图标-展开时一些手机上大图标会替换掉小图标
        //                .setLargeIcon(largeIcon)
        //                // 点击通知后自动清除
        //                .setAutoCancel(false)
        //                // 设置点击通知效果
        //                .setContentIntent(contentPendingIntent)
        //                // 设置删除时候出发的动作
        //                .setDeleteIntent(delPendingIntent)
        //                // 自定义视图
        //                .setContent(RemoteViews views)
        //                // 设置额外信息，一般显示在右下角
        //                .setContentInfo("额外信息")
        //                // 向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合
        //                .setDefaults(Notification.DEFAULT_VIBRATE);

        //        Notification notification = builder.build();
        //        // 获取NotificationManager实例
        //        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //        // 发送通知，并设置id
        //        notificationManager.notify(id, notification);


        // 自定义布局
        views = RemoteViews(packageName, R.layout.layout_mediaplayer)

        // 下一首
        val intentNext = Intent("nextMusic")
        val nextPendingIntent = PendingIntent.getBroadcast(this, NEXT_PENDINGINTENT_REQUESTCODE, intentNext, PendingIntent.FLAG_CANCEL_CURRENT)
        views!!.setOnClickPendingIntent(R.id.tv_next, nextPendingIntent)

        // 暂停/播放
        val intentPlay = Intent("playMusic")
        val playPendingIntent = PendingIntent.getBroadcast(this, PLAY_PENDINGINTENT_REQUESTCODE, intentPlay, PendingIntent.FLAG_CANCEL_CURRENT)
        views!!.setOnClickPendingIntent(R.id.tv_pause, playPendingIntent)

        // 停止
        val intentStop = Intent("stopMusic")
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
        //        // 发送通知，并设置id
        //        notificationManager.notify(NOTIFICATION_PENDINGINTENT_ID, builder.build());

        startForeground(NOTIFICATION_PENDINGINTENT_ID, builder!!.build())


        //        // 更新Notification
        //        notification = builder.setContentTitle("ZMediaPlayer111")
        //                // 设置内容
        //                .setContentText("内容111")
        //                .build();
        //        notificationManager.notify(id, notification);

        // 注册广播
        playerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("action", intent.action)
                when (intent.action) {
                    "nextMusic"// 下一首
                    -> nextMusic()
                    "playMusic"// 暂停/播放
                    -> if (mediaPlayer != null) {
                        if (!isPause) {
                            mediaPlayer!!.pause()
                            isPause = true
                        } else {
                            mediaPlayer!!.start()
                            isPause = false
                        }
                    }
                    "stopMusic"// 停止
                    -> onDestroy()
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction("nextMusic")
        intentFilter.addAction("playMusic")
        intentFilter.addAction("stopMusic")
        registerReceiver(playerReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        if (wifiLock != null && wifiLock!!.isHeld)
            wifiLock!!.release()
        try {
            if (playerReceiver != null)
                unregisterReceiver(playerReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 取消Notification
        if (notificationManager != null)
            notificationManager!!.cancel(NOTIFICATION_PENDINGINTENT_ID)
        stopForeground(true)
        // 停止服务
        stopSelf()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // 得到键值对
        val playing = intent.getBooleanExtra("playing", false)
        if (playing)
            play()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(arg0: Intent): IBinder? {
        // TODO Auto-generated method stub
        return null
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
    private fun play() {
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
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    // 下一首
    private fun nextMusic() {
        current_item++
        if (current_item >= musics.size)
            current_item = 0
        play()
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

}