package com.provotorov.imasample.ima_with_two_player_sample.ad.data

data class AdBreak(
    val id: String,
    val rollType: RollType,
    val adUrls: List<String>
)

sealed class RollType {
    data object PreRoll: RollType()

    data class MidRoll(val timeMilliseconds: Long): RollType()

    data object PostRoll: RollType()
}