package com.cosmotech.connector.adt.impl.runnable

import com.azure.digitaltwins.core.BasicDigitalTwin
import com.azure.digitaltwins.core.DigitalTwinsClient
import java.util.concurrent.Callable

class QueryDigitalTwins(private val client: DigitalTwinsClient) : Callable<Map<String, List<BasicDigitalTwin>>> {
    override fun call(): Map<String, List<BasicDigitalTwin>> {
        return client.query("SELECT * FROM DIGITALTWINS", BasicDigitalTwin::class.java)
                .groupBy { it.metadata.modelId }
    }
}