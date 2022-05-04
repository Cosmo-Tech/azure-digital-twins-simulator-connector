package com.cosmotech

import com.cosmotech.connector.adt.impl.AzureDigitalTwinsConnector
import com.cosmotech.connector.adt.impl.AzureDigitalTwinsConnectorMultithread
import com.cosmotech.connector.adt.utils.AzureDigitalTwinsUtil
import java.util.concurrent.TimeUnit.MILLISECONDS

fun main() {
    val start = System.currentTimeMillis()
    try {
        if(AzureDigitalTwinsUtil.getNumberOfThread() != 1){
            println("Run connector in MultiThread mode")
            AzureDigitalTwinsConnectorMultithread().process()
        }
        else {
            println("Run connector in SingleThread mode")
            AzureDigitalTwinsConnector().process()
        }
    } finally {
        val timing = System.currentTimeMillis() - start
        println("Done running connector in $timing ms (${MILLISECONDS.toSeconds(timing)} seconds)")
    }
}
