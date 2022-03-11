package com.cosmotech.connector.adt.impl.runnable

import com.azure.digitaltwins.core.BasicRelationship
import com.azure.digitaltwins.core.DigitalTwinsClient
import java.util.concurrent.Callable

class QueryRelationships(private val client: DigitalTwinsClient) : Callable<Map<String, List<BasicRelationship>>> {
    override fun call(): Map<String, List<BasicRelationship>> {
        return client.query("SELECT * FROM RELATIONSHIPS", BasicRelationship::class.java)
                .groupBy { it.name }
    }
}