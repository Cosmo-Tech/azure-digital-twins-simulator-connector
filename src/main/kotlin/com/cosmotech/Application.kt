package com.cosmotech

import com.cosmotech.connector.adt.impl.AzureDigitalTwinsConnector
import java.util.concurrent.TimeUnit.MILLISECONDS

fun main() {
    val start = System.currentTimeMillis()
    try {
        AzureDigitalTwinsConnector().process()
    } finally {
        val timing = System.currentTimeMillis() - start
        println("Done running connector in $timing ms (${MILLISECONDS.toSeconds(timing)} seconds)")
    }
}
