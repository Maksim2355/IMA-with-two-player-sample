package com.provotorov.imasample.ima_with_two_player_sample

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.PlayerView
import com.provotorov.imasample.ima_with_two_player_sample.ad.AdController
import com.provotorov.imasample.ima_with_two_player_sample.ad.ConsoleLogEventListener
import com.provotorov.imasample.ima_with_two_player_sample.ad.data.AdRepository

class MyActivity : Activity() {

    private lateinit var contentPlayerView: PlayerView
    private lateinit var adPlayerView: PlayerView

    private lateinit var logText: TextView

    private var contentPlayer: Player? = null
    private var adPlayer: Player? = null

    private var adController: AdController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my)

        contentPlayerView = findViewById(R.id.content_player_view)
        adPlayerView = findViewById(R.id.ad_player_view)

        logText = findViewById(R.id.logText)
        logText.movementMethod = ScrollingMovementMethod()
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
        contentPlayerView.onResume()
        adPlayerView.onResume()
    }

    override fun onStop() {
        super.onStop()
        contentPlayerView.onPause()
        adPlayerView!!.onPause()
        releasePlayer()
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(this)

        val mediaSourceFactory: MediaSource.Factory =
            DefaultMediaSourceFactory(dataSourceFactory)

        contentPlayer = ExoPlayer.Builder(this).setMediaSourceFactory(mediaSourceFactory).build()
        contentPlayerView.player = contentPlayer

        val contentUri = Uri.parse(SAMPLE_VIDEO_URL)
        val mediaItem =
            MediaItem.Builder()
                .setUri(contentUri)
                .build()

        contentPlayer!!.setMediaItem(mediaItem)
        contentPlayer!!.prepare()

        contentPlayer!!.playWhenReady = false

        initAdPlayer()
    }

    @OptIn(UnstableApi::class)
    private fun initAdPlayer() {
        adPlayer = ExoPlayer.Builder(this)
            .build()
        adPlayerView.player = adPlayer!!
        adPlayerView.useController = false
        adController = AdController(
            this,
            adPlayer!!,
            AdRepository.getAdBreaks(),
            adPlayerView,
            contentPlayer!!,
            buildAdEventListener(),
        )
    }

    private fun releasePlayer() {
        contentPlayerView.player = null
        contentPlayer?.release()
        contentPlayer = null
        adPlayer?.release()
        adPlayer = null
        adController?.release()
        adController = null
    }

    private fun buildAdEventListener(): ConsoleLogEventListener {
        return { adEvent: String ->
            val log = "IMA event: $adEvent"
            logText.append(log + "\n")
        }
    }

    companion object {
        private const val SAMPLE_VIDEO_URL = "https://storage.googleapis.com/gvabox/media/samples/stock.mp4"
    }
}
