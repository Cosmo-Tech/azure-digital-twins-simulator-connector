// copyright (c) cosmo tech corporation.
// licensed under the mit license.

package com.cosmotech.connector.adt.impl

import com.azure.core.util.Context
import com.azure.digitaltwins.core.*
import com.azure.digitaltwins.core.models.ListModelsOptions
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
import java.util.concurrent.TimeUnit


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
        val modelOptions = ListModelsOptions()
        modelOptions.includeModelDefinition = true;
        val listModels = client.listModels(modelOptions, Context.NONE)
        val dataToExport = mutableListOf<CsvData>()
        var modelInformationList = mutableListOf<DTDLModelInformation>()
        // Retrieve model Information
        listModels
                .forEach { modelData ->
                    // DTDL Model Information
                    val modelId = modelData.modelId
                    val model = modelData.dtdlModel
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

        LOGGER.info("Fetching Digital Twin Instances Information...")
        val fetchDTInstancesStart = System.nanoTime()

        val twinsFiltersConfiguration = AzureDigitalTwinsUtil.getTwinFilters()
        val twinQuery = AzureDigitalTwinsUtil.constructTwinQuery(twinsFiltersConfiguration)
        LOGGER.debug("Twin query: {} ",twinQuery)
        val digitalTwins = client.query(twinQuery, BasicDigitalTwin::class.java)
                .groupBy { it.metadata.modelId }
        modelInformationList.forEach {
            val digitalTwinsByModel = digitalTwins[it.id]
            digitalTwinsByModel?.forEach { dtInstance ->
                val dtHeaderDefaultValues = mutableListOf<String>(dtInstance.id)
                AzureDigitalTwinsUtil
                        .constructDigitalTwinInformation(
                                dtInstance,
                                it.properties,
                                dtHeaderDefaultValues,
                                dataToExport
                        )
            }
        }

        val fetchDTInstancesTiming = System.nanoTime() - fetchDTInstancesStart
        LOGGER.debug("... Operation took {} ns ({} ms)",
                fetchDTInstancesTiming,
                TimeUnit.NANOSECONDS.toMillis(fetchDTInstancesTiming))

        LOGGER.info("Fetching Digital Twin Relationships...")
        val constructRelationshipStart = System.nanoTime()

        val relsFiltersConfiguration = AzureDigitalTwinsUtil.getRelationFilters()
        val relQuery = AzureDigitalTwinsUtil.constructRelationshipQuery(relsFiltersConfiguration)
        LOGGER.debug("Relationship query: {} ",relQuery)
        AzureDigitalTwinsUtil.constructRelationshipInformation(
                client.query(relQuery, BasicRelationship::class.java)
                        .groupBy { it.name },
                dataToExport)
        val constructRelationshipTiming = System.nanoTime() - constructRelationshipStart
        LOGGER.debug("... Operation took {} ns ({} ms)",
                constructRelationshipTiming,
                TimeUnit.NANOSECONDS.toMillis(constructRelationshipTiming))

        return dataToExport
    }

    override fun process(): List<CsvData> {
        val client = this.createClient()
        val preparedData = this.prepare(client)
        val exportCsvFilesPath = AzureDigitalTwinsUtil.getExportCsvFilesPath()
        LOGGER.info("Exporting Digital Twins Data to '{}'",
                exportCsvFilesPath?.orElse("???"))
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
}
