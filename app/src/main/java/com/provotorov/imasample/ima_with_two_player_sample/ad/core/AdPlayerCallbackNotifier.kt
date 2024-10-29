package com.provotorov.imasample.ima_with_two_player_sample.ad.core

import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer

/**
 * Интерфейс для уведомления рекламного плеера IMA [VideoAdPlayer] о событиях проигрывания рекламы
 */
interface AdPlayerCallbackNotifier {
    val currentAdMediaInfo: AdMediaInfo?

    fun notifyVideoAdPlayersCallback(block: VideoAdPlayer.VideoAdPlayerCallback.() -> Unit)
}