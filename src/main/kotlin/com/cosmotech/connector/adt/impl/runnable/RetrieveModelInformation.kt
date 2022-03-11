package com.cosmotech.connector.adt.impl.runnable

import com.azure.digitaltwins.core.DigitalTwinsClient
import com.azure.digitaltwins.core.models.DigitalTwinsModelData
import com.beust.klaxon.Klaxon
import com.cosmotech.connector.adt.constants.modelDefaultProperties
import com.cosmotech.connector.adt.pojos.DTDLModelInformation
import com.cosmotech.connector.adt.utils.JsonUtil
import java.io.StringReader
import java.util.concurrent.locks.ReentrantLock

class RetrieveModelInformation(private val client: DigitalTwinsClient,
                                   private val modelData: DigitalTwinsModelData,
                                   private val modelInformations: MutableList<DTDLModelInformation>,
                                   private val retrieveModelinformationMutex: ReentrantLock) : Runnable{
    public override fun run(){
        // DTDL Model Information
        val modelId = modelData.modelId
        val model = client.getModel(modelId).dtdlModel
        val jsonModel = Klaxon().parseJsonObject(StringReader(model))
        // DT Information
        val propertiesModel = HashMap(modelDefaultProperties)
        val propertiesName = JsonUtil.readPropertiesNameAndType(jsonModel)
        retrieveModelinformationMutex.lock()
        propertiesModel.putAll(propertiesName)
        modelInformations.add(
                DTDLModelInformation(modelId, JsonUtil.readExtension(jsonModel),propertiesModel,model)
        )
        retrieveModelinformationMutex.unlock()
    }
}