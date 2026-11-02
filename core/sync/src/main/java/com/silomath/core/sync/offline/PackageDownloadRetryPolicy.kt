package com.silomath.core.sync.offline

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object PackageDownloadRetryPolicy {
    fun delayMillis(attempt: Int,retryAfter: String?,nowMillis: Long=System.currentTimeMillis(),jitter: Double=Random.nextDouble(0.8,1.2)): Long {
        val serverDelay=retryAfter?.toLongOrNull()?.takeIf { it>=0 }?.let { seconds ->
            if(seconds>Long.MAX_VALUE/1000) Long.MAX_VALUE else seconds*1000
        } ?: retryAfter?.let { value -> runCatching {
            (ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()-nowMillis).coerceAtLeast(0)
        }.getOrNull() }
        if(serverDelay!=null) return serverDelay
        val schedule=longArrayOf(2000,5000,15000,30000,60000,300000)
        return (schedule[attempt.coerceIn(0,schedule.lastIndex)]*jitter).toLong().coerceIn(1,300000)
    }
}
