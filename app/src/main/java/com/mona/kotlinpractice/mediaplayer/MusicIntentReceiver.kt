package com.mona.kotlinpractice.mediaplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MusicIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            // signal your service to stop playback
            // (via an Intent, for instance)
        }
    }
}