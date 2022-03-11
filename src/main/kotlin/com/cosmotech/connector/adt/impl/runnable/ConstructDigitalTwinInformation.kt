package com.cosmotech.connector.adt.impl.runnable

import com.azure.digitaltwins.core.BasicDigitalTwin
import com.cosmotech.connector.adt.utils.AzureDigitalTwinsUtil
import com.cosmotech.connector.commons.pojo.CsvData
import java.util.concurrent.locks.ReentrantLock

class ConstructDigitalTwinInformation(private val dtInstance: BasicDigitalTwin,
                                          private val properties: MutableMap<String, String>,
                                          private val dataToExport: MutableList<CsvData>,
                                          private val constructDigitalTwinInformationMutex: ReentrantLock) : Runnable{

    public override fun run(){
        val dtHeaderDefaultValues = mutableListOf<String>(dtInstance.id)
        constructDigitalTwinInformationMutex.lock()
        AzureDigitalTwinsUtil
                .constructDigitalTwinInformation(
                        dtInstance,
                        properties,
                        dtHeaderDefaultValues,
                        dataToExport
                )
        constructDigitalTwinInformationMutex.unlock()
    }
}