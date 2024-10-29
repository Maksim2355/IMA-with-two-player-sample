package com.provotorov.imasample.ima_with_two_player_sample.ad

import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.google.ads.interactivemedia.v3.api.AdPodInfo
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.provotorov.imasample.ima_with_two_player_sample.ad.core.AdPlayerCallbackNotifier
import java.util.Timer
import java.util.TimerTask

/**
 * Адаптер для взаимодействия IMA sdk и плеером из media3
 *
 * Помимо этого реализует интерфейс [AdPlayerCallbackNotifier] для того, чтобы плеер из media3 мог уведомлять рекламный SDK об изменеии состояния
 */
class VideoAdPlayerWrapper(
    private val adPlayer: Player
) : VideoAdPlayer, AdPlayerCallbackNotifier {

    override var currentAdMediaInfo: AdMediaInfo? = null
        private set

    private var timer: Timer? = null

    private val adPlayerCallbacks = mutableListOf<VideoAdPlayer.VideoAdPlayerCallback>()

    override fun loadAd(adMediaInfo: AdMediaInfo, podInfo: AdPodInfo) {
        val newAdMediaItem = MediaItem.fromUri(adMediaInfo.url)
        adPlayer.addMediaItem(newAdMediaItem)
        adPlayer.prepare()
    }

    override fun pauseAd(adMediaInfo: AdMediaInfo) {
        adPlayer.pause()
    }

    override fun playAd(adMediaInfo: AdMediaInfo) {
        currentAdMediaInfo = adMediaInfo
        startTracking()
        adPlayer.play()
    }

    override fun stopAd(adMediaInfo: AdMediaInfo) {
        currentAdMediaInfo = null
        adPlayer.stop()
        stopTimer()
    }

    override fun getVolume(): Int {
        return adPlayer.volume.toInt()
    }

    override fun release() {
        //Освобождаем ресурсы при необходимости
    }

    override fun addCallback(callback: VideoAdPlayer.VideoAdPlayerCallback) {
        adPlayerCallbacks.add(callback)
    }

    override fun removeCallback(callback: VideoAdPlayer.VideoAdPlayerCallback) {
        adPlayerCallbacks.remove(callback)
    }

    override fun getAdProgress(): VideoProgressUpdate {
        return VideoProgressUpdate(
            adPlayer.currentPosition, adPlayer.duration
        )
    }

    override fun notifyVideoAdPlayersCallback(block: VideoAdPlayer.VideoAdPlayerCallback.() -> Unit) {
        adPlayerCallbacks.forEach {
            block(it)
        }
    }

    fun hasLoadedAds(): Boolean {
        return currentAdMediaInfo != null
    }

    private fun startTracking() {
        if (timer != null) {
            return
        }

        timer = Timer().apply {
            val updateTimerTask: TimerTask =
                object : TimerTask() {
                    override fun run() {
                        Handler(Looper.getMainLooper()).post {
                            notifyVideoAdPlayersCallback {
                                val currentAdMediaInfo = currentAdMediaInfo ?: return@notifyVideoAdPlayersCallback
                                onAdProgress(currentAdMediaInfo, adProgress)
                            }
                        }
                    }
                }
            val initialDelayMs = 250
            //Рекомендуется проверять прогресс каждые 100мс в документации
            val pollingTimeMs = 100
            schedule(updateTimerTask, pollingTimeMs.toLong(), initialDelayMs.toLong())
        }
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }
}