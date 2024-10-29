package com.provotorov.imasample.ima_with_two_player_sample.ad

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.media3.common.Player
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventType
import com.google.ads.interactivemedia.v3.api.AdsLoader
import com.google.ads.interactivemedia.v3.api.AdsManager
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory
import com.provotorov.imasample.ima_with_two_player_sample.ad.data.AdBreak
import java.util.ArrayDeque
import java.util.Queue

typealias ConsoleLogEventListener = (String) -> Unit

/**
 * Инкапсулирует логику для работы с рекламой
 */
class AdController(
    context: Context,
    private val adPlayer: Player,
    adBreaks: List<AdBreak>,
    private val adViewGroup: ViewGroup,
    private val contentPlayer: Player,
    private val consoleLogEventListener: ConsoleLogEventListener,
) {
    private val imaSdkFactory = ImaSdkFactory.getInstance()

    private val adsLoader: AdsLoader

    private var adsManager: AdsManager? = null

    private val videoAdPlayer: VideoAdPlayerWrapper = VideoAdPlayerWrapper(adPlayer)

    private val pendingAdBreak: Queue<String> = ArrayDeque()

    private val adRollEventObserver: AdRollEventObserver

    private var isLoadingAdsRequest: Boolean = false

    init {
        val adDisplayContainer = ImaSdkFactory.createAudioAdDisplayContainer(context, videoAdPlayer)
        val settings = imaSdkFactory.createImaSdkSettings().apply { isDebugMode = true }
        adsLoader = imaSdkFactory.createAdsLoader(context, settings, adDisplayContainer).apply {
            addAdsLoadedListener(AdsLoadedListener())
            addAdErrorListener { consoleLogEventListener("Ima ads loader event ${it.error.message}") }
        }
        adPlayer.addListener(AdPlayerEventListener(videoAdPlayer))
        adRollEventObserver = AdRollEventObserver(
            contentPlayer,
            adBreaks,
            onAdBreakReady = ::showAdBreak
        )
    }

    private fun resumeContent() {
        //На эмулятор ARM встречается проблема, что после паузы плеер переходит в состояние IDLE
        if (contentPlayer.playbackState == Player.STATE_IDLE){
            contentPlayer.prepare()
        }
        contentPlayer.play()
    }

    private fun pauseContent() {
        contentPlayer.pause()
    }

    private fun requestAds() {
        val adTag = pendingAdBreak.poll()
            ?: error("Для текущего временного отрезка не предусмотрена реклама")
        isLoadingAdsRequest = true
        val adsRequest = imaSdkFactory.createAdsRequest().apply {
            adTagUrl = adTag
        }
        adsLoader.requestAds(adsRequest)
    }

    private fun hasNextPendingAds(): Boolean {
        return pendingAdBreak.isNotEmpty() || videoAdPlayer.hasLoadedAds() || isLoadingAdsRequest
    }

    private fun showAdBreak(adBreak: AdBreak) {
        consoleLogEventListener("Show ad break ${adBreak.id}")
        pendingAdBreak.addAll(adBreak.adUrls)
        requestAds()
    }

    fun release() {
        adsLoader.release()
        adsManager?.destroy()
    }

    private inner class AdsLoadedListener : AdsLoader.AdsLoadedListener {

        override fun onAdsManagerLoaded(adsManagerLoadedEvent: AdsManagerLoadedEvent) {
            val adsManager = adsManagerLoadedEvent.adsManager

            adsManager.prepareErrorListener(adsManager)
            adsManager.prepareEventListener(adsManager)

            val adsRenderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings()
                .apply {
                    enablePreloading = true
                }
            adsManager.init(adsRenderingSettings)
        }

        private fun AdsManager.prepareEventListener(adsManager: AdsManager) {
            addAdEventListener { adEvent ->
                logAdEvent(adEvent)

                when (adEvent.type) {
                    AdEventType.LOADED -> {
                        isLoadingAdsRequest = false
                        actionAfterPlayerReady {
                            adsManager.start()
                        }
                    }

                    AdEventType.CONTENT_PAUSE_REQUESTED -> {
                        pauseContent()
                        adViewGroup.isVisible = true
                    }

                    AdEventType.CONTENT_RESUME_REQUESTED -> {
                        if (!hasNextPendingAds()) {
                            adViewGroup.isVisible = false
                            resumeContent()
                        }
                    }

                    AdEventType.ALL_ADS_COMPLETED -> {
                        if (!hasNextPendingAds() && adRollEventObserver.isAllAdsComplete) {
                            adPlayer.release()
                            adsManager.destroy()
                        }else{
                            if (hasNextPendingAds()) {
                                requestAds()
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }

        private fun actionAfterPlayerReady(action: () -> Unit) {
            if (adPlayer.playbackState == Player.STATE_READY) {
                action()
                return
            }
            adPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_READY) {
                        action()
                        adPlayer.removeListener(this)
                    }
                }
            })
        }

        private fun AdsManager.prepareErrorListener(adsManager: AdsManager) {
            addAdErrorListener { adErrorEvent ->
                consoleLogEventListener(adErrorEvent.error.message)
                resumeContent()
                adsManager.destroy()
            }
        }

        private fun logAdEvent(adEvent: AdEvent) {
            if (adEvent.type != AdEventType.AD_PROGRESS) {
                consoleLogEventListener(adEvent.type.toString())
            }
        }
    }
}