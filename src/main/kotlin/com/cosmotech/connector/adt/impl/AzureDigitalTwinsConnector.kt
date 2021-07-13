// copyright (c) cosmo tech corporation.
// licensed under the mit license.

package com.cosmotech.connector.adt.impl

import com.azure.digitaltwins.core.*
import com.azure.identity.ClientSecretCredentialBuilder
import com.beust.klaxon.Klaxon
import com.cosmotech.connector.adt.constants.modelDefaultProperties
import com.cosmotech.connector.adt.pojos.DTDLModelInformation
import com.cosmotech.connector.adt.utils.AzureDigitalTwinsUtil
import com.cosmotech.connector.adt.utils.JsonUtil
import com.cosmotech.connector.commons.Connector
import com.cosmotech.connector.commons.pojo.CsvData
import org.apache.logging.log4j.LogManager
import java.io.StringReader
import java.io.File


/**
 * Connector for Azure Digital Twin
 */
class AzureDigitalTwinsConnector : Connector<DigitalTwinsClient,List<CsvData>,List<CsvData>> {

    companion object {
        val LOGGER = LogManager.getLogger(AzureDigitalTwinsConnector::class.java.name)
    }

    override fun createClient(): DigitalTwinsClient {
        LOGGER.info("Create Digital Twins Client")
        return DigitalTwinsClientBuilder()
            .credential(
                ClientSecretCredentialBuilder()
                    .clientId(AzureDigitalTwinsUtil.getAzureClientId())
                    .tenantId(AzureDigitalTwinsUtil.getAzureTenantId())
                    .clientSecret(AzureDigitalTwinsUtil.getAzureClientSecret())
                    .build()
            )
            .endpoint(AzureDigitalTwinsUtil.getInstanceUrl())
            .serviceVersion(DigitalTwinsServiceVersion.getLatest())
            .buildClient()
    }

    override fun prepare(client: DigitalTwinsClient): List<CsvData> {
        LOGGER.info("Start preparing Digital Twins Data")
        val listModels = client.listModels()
        val dataToExport = mutableListOf<CsvData>()
        var modelInformationList = mutableListOf<DTDLModelInformation>()
        // Retrieve model Information
        listModels
            .sortedBy { it.modelId }
            .forEach { modelData ->
                // DTDL Model Information
                val modelId = modelData.modelId
                val model = client.getModel(modelId).dtdlModel
                val jsonModel = Klaxon().parseJsonObject(StringReader(model))
                // DT Information
                val propertiesModel = HashMap(modelDefaultProperties)
                val propertiesName = JsonUtil.readPropertiesNameAndType(jsonModel)
                propertiesModel.putAll(propertiesName)
                modelInformationList.add(
                    DTDLModelInformation(modelId,JsonUtil.readExtension(jsonModel),propertiesModel,model)
                )
            }

        modelInformationList = AzureDigitalTwinsUtil.retrievePropertiesFromBaseModels(modelInformationList)

        val digitalTwinInstances = constructDigitalTwinInstances(modelInformationList, client)

        val digitalTwinInformation = mutableListOf<Pair<DTDLModelInformation,BasicDigitalTwin>>()
        digitalTwinInstances
            .sortedBy { it.id }
            .forEach { dtInstance ->
            val modelMatched = modelInformationList.first { it.id == dtInstance.metadata.modelId }
            digitalTwinInformation.add(Pair(modelMatched,dtInstance))
        }

        digitalTwinInformation.forEach { (modelInformation,dtInstance) ->
            val dtHeaderDefaultValues = mutableListOf<String>(dtInstance.id)
            AzureDigitalTwinsUtil
                .constructDigitalTwinInformation(
                    dtInstance,
                    modelInformation.properties,
                    dtHeaderDefaultValues,
                    dataToExport
                )

            val currentRelationships =
                client
                    .listRelationships(dtInstance.id, BasicRelationship::class.java)
                    .toList()
                    .groupBy { it.name }

            AzureDigitalTwinsUtil
                .constructRelationshipInformation(
                    currentRelationships,
                    dataToExport
                )
        }

        return dataToExport
    }

    override fun process(): List<CsvData> {
        val client = this.createClient()
        val preparedData = this.prepare(client)
        val exportCsvFilesPath = AzureDigitalTwinsUtil.getExportCsvFilesPath()
        LOGGER.info("Exported Digital Twins Data")
        preparedData.forEach {
            LOGGER.debug("Short Model: ${it.fileName} , " +
                    "CSV Headers: ${it.headerNameAndType} , " +
                    "rows : ${it.rows}")
            if (exportCsvFilesPath?.isPresent == true) {
              var exportDirectory = exportCsvFilesPath.get()
              if (!exportDirectory.endsWith("/") ) {
                  exportDirectory = exportDirectory.plus("/")
              }
              it.exportDirectory = exportDirectory
            }
            val directory = File(it.exportDirectory)
            directory.mkdirs()
            it.writeFile()
        }
        return preparedData
    }

    /**
     * Fetch and regroup all existing digital twin instances
     * @param modelInformationList the existing DT model list
     * @param client the Azure Digital Twin client
     * @return the list of Digital Twin instances
     */
    private fun constructDigitalTwinInstances(
        modelInformationList: MutableList<DTDLModelInformation>,
        client: DigitalTwinsClient
    ): MutableList<BasicDigitalTwin> {
        val digitalTwinInstances = mutableListOf<BasicDigitalTwin>()
        // Construct DT information list
        modelInformationList.forEach { modelInformation ->
            val digitalTwinInModel = client.query(
                "SELECT * FROM DIGITALTWINS WHERE IS_OF_MODEL('${modelInformation.id}')",
                BasicDigitalTwin::class.java
            )
            digitalTwinInModel
                .forEach {
                    digitalTwinInstances.add(it)
                }
        }
        return digitalTwinInstances
    }
}
