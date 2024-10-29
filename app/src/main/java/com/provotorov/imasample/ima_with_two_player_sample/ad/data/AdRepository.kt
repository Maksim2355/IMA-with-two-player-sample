package com.provotorov.imasample.ima_with_two_player_sample.ad.data

object AdRepository {

    fun getAdBreaks(): List<AdBreak> {
        return createFakeAdBreaks()
    }

    private fun createFakeAdBreaks(): List<AdBreak> {
        return listOf(
            AdBreak(
                id = "preroll",
                rollType = RollType.PreRoll,
                adUrls = listOf(
                    "https://basil79.github.io/vast-sample-tags/pg/vast.xml",
                )
            ),
            AdBreak(
                id = "midroll-1",
                rollType = RollType.MidRoll(timeMilliseconds = 3000L),
                adUrls = listOf(
                    "https://basil79.github.io/vast-sample-tags/pg/vast.xml",
                    "https://basil79.github.io/vast-sample-tags/rama/vast.xml",
                )
            ),
            AdBreak(
                id = "midroll-2",
                rollType = RollType.MidRoll(timeMilliseconds = 10000L),
                adUrls = listOf(
                    "https://basil79.github.io/vast-sample-tags/pg/vast.xml",
                    "https://basil79.github.io/vast-sample-tags/rama/vast.xml",
                )
            ),
            AdBreak(
                id = "postroll",
                rollType = RollType.PostRoll,
                adUrls = listOf(
                    "https://basil79.github.io/vast-sample-tags/pg/vast.xml",
                    "https://basil79.github.io/vast-sample-tags/rama/vast.xml",
                )
            ),
        )
    }
}