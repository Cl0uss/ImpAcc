package com.example.impacc.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class ForegroundApplicationDetector(
    context: Context
) {

    private val usageStatsManager =
        context.getSystemService(
            UsageStatsManager::class.java
        )

    fun getForegroundPackageName(): String? {
        val currentTime =
            System.currentTimeMillis()

        val usageEvents =
            usageStatsManager.queryEvents(
                currentTime - EVENT_LOOKBACK_MILLISECONDS,
                currentTime
            )

        val event =
            UsageEvents.Event()

        var latestTimestamp = 0L
        var latestPackageName: String? = null

        while (usageEvents.hasNextEvent()) {

            usageEvents.getNextEvent(event)

            if (
                event.eventType ==
                UsageEvents.Event.ACTIVITY_RESUMED &&
                event.timeStamp >= latestTimestamp
            ) {
                latestTimestamp =
                    event.timeStamp

                latestPackageName =
                    event.packageName
            }
        }

        return latestPackageName
    }

    companion object {

        private const val EVENT_LOOKBACK_MILLISECONDS =
            10_000L
    }
}