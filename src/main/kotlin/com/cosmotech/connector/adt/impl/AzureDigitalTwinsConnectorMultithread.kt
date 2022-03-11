// copyright (c) cosmo tech corporation.
// licensed under the mit license.

package com.cosmotech.connector.adt.impl

import com.azure.digitaltwins.core.*
import com.azure.digitaltwins.core.models.DigitalTwinsModelData
import com.azure.identity.ClientSecretCredentialBuilder
import com.cosmotech.connector.adt.impl.runnable.*
import com.cosmotech.connector.adt.pojos.DTDLModelInformation
import com.cosmotech.connector.adt.utils.AzureDigitalTwinsUtil
import com.cosmotech.connector.commons.Connector
import com.cosmotech.connector.commons.pojo.CsvData
import org.apache.logging.log4j.LogManager
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock

/**
 * Connector for Azure Digital Twin
 */
class AzureDigitalTwinsConnectorMultithread : Connector<DigitalTwinsClient,List<CsvData>,List<CsvData>> {

    companion object {
        val LOGGER = LogManager.getLogger(AzureDigitalTwinsConnectorMultithread::class.java.name)
        // Should we use Runtime.getRuntime().availableProcessors()
        val executor = Executors.newFixedThreadPool(AzureDigitalTwinsUtil.getNumberOfThread())
    }

    private val constructDigitalTwinInformationMutex = ReentrantLock()
    private val retrieveModelInformationMutex = ReentrantLock()
    private val exportDataToMutex = ReentrantLock()
    private val constructRelationshipInformationMutex = ReentrantLock()

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
        for (modelData: DigitalTwinsModelData in listModels){
            executor.execute(
                    RetrieveModelInformation(client, modelData, modelInformationList, retrieveModelInformationMutex))
        }

        modelInformationList = AzureDigitalTwinsUtil.retrievePropertiesFromBaseModels(modelInformationList)

        LOGGER.info("Fetching Digital Twin Instances Information...")
        val fetchDTInstancesStart = System.nanoTime()

        val future = executor.submit(QueryDigitalTwins(client))

        val digitalTwins = future.get()
        modelInformationList.forEach {
            val digitalTwinsByModel = digitalTwins[it.id]
            val properties = it.properties
            digitalTwinsByModel?.forEach { dtInstance ->
                executor.execute(ConstructDigitalTwinInformation(dtInstance, properties, dataToExport,constructDigitalTwinInformationMutex))
            }
        }

        val fetchDTInstancesTiming = System.nanoTime() - fetchDTInstancesStart
        LOGGER.debug("... Operation took {} ns ({} ms)",
                fetchDTInstancesTiming,
                TimeUnit.NANOSECONDS.toMillis(fetchDTInstancesTiming))

        LOGGER.info("Fetching Digital Twin Relationships...")
        val constructRelationshipStart = System.nanoTime()

        val futureRelationships = executor.submit(QueryRelationships(client))

        val relationships = futureRelationships.get()
        relationships.entries.forEach {(relationName, basicRelationships) ->
            executor.execute(ConstructRelationshipInformation(
                    relationName, basicRelationships, dataToExport,constructRelationshipInformationMutex))
        }

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

        for(data: CsvData in preparedData){
            executor.execute(ExportDataTo(data, exportCsvFilesPath, exportDataToMutex))
        }
        executor.shutdown()
        return preparedData
    }
}
