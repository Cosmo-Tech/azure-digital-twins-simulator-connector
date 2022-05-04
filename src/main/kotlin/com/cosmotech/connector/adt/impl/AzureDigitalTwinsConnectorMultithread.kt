// copyright (c) cosmo tech corporation.
// licensed under the mit license.

package com.cosmotech.connector.adt.impl

import com.azure.core.util.Context
import com.azure.digitaltwins.core.*
import com.azure.digitaltwins.core.models.ListModelsOptions
import com.azure.identity.ClientSecretCredentialBuilder
import com.beust.klaxon.Klaxon
import com.cosmotech.connector.adt.constants.modelDefaultProperties
import com.cosmotech.connector.adt.impl.callable.ManageDigitalTwins
import com.cosmotech.connector.adt.impl.callable.WriteFiles
import com.cosmotech.connector.adt.pojos.DTDLModelInformation
import com.cosmotech.connector.adt.utils.AzureDigitalTwinsUtil
import com.cosmotech.connector.adt.utils.JsonUtil
import com.cosmotech.connector.commons.Connector
import com.cosmotech.connector.commons.pojo.CsvData
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.StringReader
import java.util.concurrent.*
import java.util.stream.Collectors

/**
 * Connector for Azure Digital Twin
 */
class AzureDigitalTwinsConnectorMultithread : Connector<DigitalTwinsClient,List<CsvData>,List<CsvData>> {

    companion object {
        val LOGGER: Logger = LogManager.getLogger(AzureDigitalTwinsConnectorMultithread::class.java.name)
        val executor: ExecutorService = Executors.newFixedThreadPool(AzureDigitalTwinsUtil.getNumberOfThread())
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
        val modelInformationList = mutableListOf<DTDLModelInformation>()
        // Retrieve model Information
        listModels
            .forEach { modelData ->
                val modelId = modelData.modelId
                val dtdlModel = modelData.dtdlModel
                val jsonModel = Klaxon().parseJsonObject(StringReader(dtdlModel))
                val propertiesModel = HashMap(modelDefaultProperties)
                val propertiesName = JsonUtil.readPropertiesNameAndType(jsonModel)
                propertiesModel.putAll(propertiesName)
                modelInformationList.add(
                    DTDLModelInformation(modelId,
                        JsonUtil.readExtension(jsonModel),propertiesModel, dtdlModel
                    )
                )
            }

        LOGGER.info("Fetching Digital Twin Instances Information...")
        val fetchDTInstancesStart = System.nanoTime()

        val completableFutureList : MutableList<CompletableFuture<CsvData?>> = mutableListOf()

        for(model in modelInformationList){
            completableFutureList.add(CompletableFuture.supplyAsync(
                ManageDigitalTwins(
                    model.id,
                    modelInformationList,
                    client
                ), executor
            ))
        }

        val csvDataList =
            completableFutureList.stream().map(CompletableFuture<CsvData?>::join).collect(
                Collectors.toList())
        csvDataList.removeAll(listOf(null))

        val fetchDTInstancesTiming = System.nanoTime() - fetchDTInstancesStart
        LOGGER.debug("Fetching Digital Twin Instances Information Operation took {} ns ({} ms)",
            fetchDTInstancesTiming,
            TimeUnit.NANOSECONDS.toMillis(fetchDTInstancesTiming))


        LOGGER.info("Fetching Digital Twin Relationships...")
        val fetchRelationshipInstancesStart = System.nanoTime()
        AzureDigitalTwinsUtil.constructRelationshipInformation(
            client.query("SELECT * FROM RELATIONSHIPS", BasicRelationship::class.java)
                .groupBy { it.name },
            csvDataList as MutableList<CsvData>
        )
        val fetchRelationshipTiming = System.nanoTime() - fetchRelationshipInstancesStart
        LOGGER.info("Fetching Digital Twin Relationships Operation took {} ns ({} ms)",
            fetchRelationshipTiming,
            TimeUnit.NANOSECONDS.toMillis(fetchRelationshipTiming))

        return csvDataList
    }

    override fun process(): List<CsvData> {
        val client = this.createClient()
        val preparedData = this.prepare(client)
        val exportCsvFilesPath = AzureDigitalTwinsUtil.getExportCsvFilesPath()
        var exportPath ="/"
        if (exportCsvFilesPath?.isPresent == true){
            exportPath = exportCsvFilesPath.get()
        }
        LOGGER.info("Exporting Digital Twins Data to '{}'",
                exportCsvFilesPath?.orElse("???"))
        val completableFutureList : MutableList<CompletableFuture<String>> = mutableListOf()
        for(csvData in preparedData){
            completableFutureList.add(CompletableFuture.supplyAsync(
                WriteFiles(
                    csvData,
                    exportPath
                ), executor
            ))
        }
        completableFutureList.stream().map(CompletableFuture<String>::join).collect(
                Collectors.toList())

        executor.shutdown()
        return preparedData
    }
}
