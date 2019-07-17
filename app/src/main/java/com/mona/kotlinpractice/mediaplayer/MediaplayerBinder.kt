package com.mona.kotlinpractice.mediaplayer

import android.os.Binder

class MediaplayerBinder(service: MediaPlayerService) : Binder() {
    private var service: MediaPlayerService

    init {

        this.service = service
    }


    fun getService(): MediaPlayerService {
        return this.service
    }

    fun setService(service: MediaPlayerService) {
        this.service = service
    }
}