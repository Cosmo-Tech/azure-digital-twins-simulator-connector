package com.cosmotech.connector.adt.impl.callable

import com.azure.digitaltwins.core.BasicDigitalTwin
import com.azure.digitaltwins.core.DigitalTwinsClient
import com.cosmotech.connector.adt.pojos.DTDLModelInformation
import com.cosmotech.connector.adt.utils.AzureDigitalTwinsUtil
import com.cosmotech.connector.commons.pojo.CsvData
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class ManageDigitalTwins(private val modelId: String,
                         private val modelInformationList : MutableList<DTDLModelInformation>,
                         private val client: DigitalTwinsClient,
                         private val filters: Optional<String>?
    ) : Supplier<CsvData?> {

    companion object {
        val LOGGER: Logger = LogManager.getLogger(ManageDigitalTwins::class.java.name)
    }


    override fun get(): CsvData?  {

        LOGGER.info("Fetching Digital Twin Instances Information for $modelId ...")
        val fetchDTInstanceStart = System.nanoTime()
        var queryString = StringBuilder("SELECT * FROM DIGITALTWINS WHERE IS_OF_MODEL('")
            .append("$modelId')").toString()

        if (filters != null && filters.isPresent) {
            val twinQuery = AzureDigitalTwinsUtil.constructTwinQuery(filters)
            queryString = StringBuilder("$twinQuery AND IS_OF_MODEL('")
                .append("$modelId')").toString()
        }

        val digitalTwinsTyped = client.query(queryString, BasicDigitalTwin::class.java).toList()
        val dataToExport : MutableList<CsvData> = mutableListOf()

        modelInformationList
            .filter { it.id === modelId }
            .forEach {
                digitalTwinsTyped.forEach{ dtInstance ->
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


        val fetchDTInstanceTiming = System.nanoTime() - fetchDTInstanceStart
        LOGGER.info("Fetching Digital Twin Instances Information for $modelId Operation took {} ns ({} ms)",
            fetchDTInstanceTiming,
            TimeUnit.NANOSECONDS.toMillis(fetchDTInstanceTiming))
        LOGGER.info("End Fetching Digital Twin Instances Information for $modelId ...")

        if( dataToExport.size == 1){
            return dataToExport[0]
        }
        return null
    }
}