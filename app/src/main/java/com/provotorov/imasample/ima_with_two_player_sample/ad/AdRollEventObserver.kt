package com.provotorov.imasample.ima_with_two_player_sample.ad

import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.provotorov.imasample.ima_with_two_player_sample.ad.data.AdBreak
import com.provotorov.imasample.ima_with_two_player_sample.ad.data.RollType
import java.util.ArrayList
import java.util.Timer
import java.util.TimerTask

/**
 * Сущность для отслеживания времени когда следует показать рекламу
 */
class AdRollEventObserver(
    private val contentPlayer: Player,
    adCuePoint: List<AdBreak>,
    private val preloadingAdsTime: Long = DEFAULT_PRELOADING_ADS_TIME,
    private val onAdBreakReady: ((AdBreak) -> Unit)? = null,
    private val onAllAdBreakComplete: (() -> Unit)? = null
) {

    private var adBreakTimer: Timer? = null

    private val adBreaks = ArrayList<AdBreak>(adCuePoint)

    private var checkAdPlayerListener: Player.Listener = createAdPlayerListener()

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        contentPlayer.addListener(
            checkAdPlayerListener
        )
        val preRoll = adBreaks.find { it.rollType is RollType.PreRoll }
        if (preRoll != null){
            notifyAboutAdBreak(preRoll)
        }
    }

    val isAllAdsComplete: Boolean
        get() = adBreaks.isEmpty()

    fun startTimer() {
        if (adBreakTimer != null) {
            return
        }

        adBreakTimer = Timer().apply {
            val updateTimerTask: TimerTask = object : TimerTask() {
                override fun run() {
                    mainHandler.post { checkIfAdReady() }
                }
            }
            schedule(updateTimerTask, 0, preloadingAdsTime)
        }
    }

    fun stopTimer() {
        adBreakTimer?.cancel()
        adBreakTimer = null
    }

    fun checkIfAdReady() {
        val currentContentDuration = contentPlayer.currentPosition
        val isPostRoll = contentPlayer.playbackState == Player.STATE_ENDED

        val pendingAdBreak = when {
            isPostRoll -> {
                adBreaks.find { it.rollType is RollType.PostRoll }
            }

            else -> {
                val rangeWhenShouldShowAd = (currentContentDuration - preloadingAdsTime)..(currentContentDuration + preloadingAdsTime)
                adBreaks.find { adBreak ->
                    adBreak.rollType is RollType.MidRoll
                        && adBreak.rollType.timeMilliseconds in rangeWhenShouldShowAd
                }
            }
        }

        if (pendingAdBreak != null){
            notifyAboutAdBreak(pendingAdBreak)
        }
    }

    private fun notifyAboutAdBreak(adBreak: AdBreak) {
        adBreaks.remove(adBreak)
        onAdBreakReady?.invoke(adBreak)
        if (adBreaks.isEmpty()) {
            release()
        }
    }

    private fun release() {
        onAllAdBreakComplete?.invoke()
        adBreakTimer?.cancel()
        contentPlayer.removeListener(checkAdPlayerListener)
    }

    private fun createAdPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    startTimer()
                } else {
                    stopTimer()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    checkIfAdReady()
                }
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                checkIfAdReady()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                checkIfAdReady()
            }
        }
    }

    companion object {
        private const val DEFAULT_PRELOADING_ADS_TIME = 500L
    }
}