package com.provotorov.imasample.ima_with_two_player_sample.ad

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.provotorov.imasample.ima_with_two_player_sample.ad.core.AdPlayerCallbackNotifier

/**
 * Листенер для передачи события плеера рекламной обертки [VideoAdPlayer]
 *
 * Необходим для того, чтобы уведомлять рекламный плеер [VideoAdPlayer] об изменениях в проигрывании и состоянии плеера [Player]
 */
class AdPlayerEventListener(
    private val videoAdPlayer: AdPlayerCallbackNotifier
) : Player.Listener {

    private var playWhenReady: Boolean = false

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val currentAdMediaInfo = videoAdPlayer.currentAdMediaInfo ?: return
        if (isPlaying) {
            videoAdPlayer.notifyVideoAdPlayersCallback {
                onPlay(currentAdMediaInfo)
            }
        } else {
            videoAdPlayer.notifyVideoAdPlayersCallback {
                onPause(currentAdMediaInfo)
            }
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)
        this.playWhenReady = playWhenReady
    }

    // Событие onPause и onResume можно отследить через изменение состояния
    override fun onPlaybackStateChanged(playbackState: Int) {
        val adMediaInfo = videoAdPlayer.currentAdMediaInfo ?: return
        when (playbackState) {
            Player.STATE_READY -> {
                if (playWhenReady) {
                    videoAdPlayer.notifyVideoAdPlayersCallback {
                        onResume(adMediaInfo)
                    }
                } else {
                    videoAdPlayer.notifyVideoAdPlayersCallback {
                        onPause(adMediaInfo)
                    }
                }
            }

            Player.STATE_ENDED -> {
                videoAdPlayer.notifyVideoAdPlayersCallback {
                    onEnded(adMediaInfo)
                }
            }

            Player.STATE_BUFFERING -> {
                videoAdPlayer.notifyVideoAdPlayersCallback {
                    onBuffering(adMediaInfo)
                }
            }
            else -> Unit
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        val adMediaInfo = videoAdPlayer.currentAdMediaInfo ?: return
        videoAdPlayer.notifyVideoAdPlayersCallback {
            onEnded(adMediaInfo)
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        val adMediaInfo = videoAdPlayer.currentAdMediaInfo ?: return
        videoAdPlayer.notifyVideoAdPlayersCallback {
            onError(adMediaInfo)
        }
    }
}