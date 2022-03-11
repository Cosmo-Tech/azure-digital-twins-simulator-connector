package com.cosmotech.connector.adt.impl.runnable

import com.azure.digitaltwins.core.BasicRelationship
import com.cosmotech.connector.adt.utils.AzureDigitalTwinsUtil
import com.cosmotech.connector.commons.pojo.CsvData
import java.util.concurrent.locks.ReentrantLock

class ConstructRelationshipInformation(private val relationName: String,
                                           private val basicRelationships:  List<BasicRelationship>,
                                           private val relationshipsToExport: MutableList<CsvData>,
                                           private val constructRelationshipInformationMutex: ReentrantLock) : Runnable{

    public override fun run(){
        constructRelationshipInformationMutex.lock()
        AzureDigitalTwinsUtil.constructRelationshipInformation(
                relationName, basicRelationships, relationshipsToExport)
        constructRelationshipInformationMutex.unlock()
    }
}