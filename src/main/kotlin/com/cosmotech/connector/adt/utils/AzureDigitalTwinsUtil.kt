// copyright (c) cosmo tech corporation.
// licensed under the mit license.

package com.cosmotech.connector.adt.utils

import com.azure.digitaltwins.core.BasicDigitalTwin
import com.azure.digitaltwins.core.BasicRelationship
import com.beust.klaxon.Klaxon
import com.cosmotech.connector.adt.constants.*
import com.cosmotech.connector.adt.extensions.getModelNameFromModelId
import com.cosmotech.connector.adt.pojos.DTDLModelInformation
import com.cosmotech.connector.commons.pojo.CsvData
import com.fasterxml.jackson.databind.util.RawValue
import net.minidev.json.JSONObject
import org.eclipse.microprofile.config.Config
import org.eclipse.microprofile.config.ConfigProvider
import java.util.*

/**
 * Utility class to handle Azure Digital Twins objects
 */
object AzureDigitalTwinsUtil {


    private val configuration: Config = ConfigProvider.getConfig()

    /**
     * Construct a digital twin row to print into a CSV
     * @param defaultHeaderValues the default row values
     * @param headers the list of header cell name
     * @param digitalTwin a digital twin
     * @return the data row to print
     */
    @JvmStatic
    fun constructDigitalTwinRowValue(
        defaultHeaderValues: MutableList<String>,
        headers: MutableMap<String,String>,
        digitalTwin: BasicDigitalTwin
    ): MutableList<String> {

        val digitalTwinRowValues = mutableListOf<String>()

        headers.forEach { (headerCellName, _) ->
            if (!modelDefaultProperties.contains(headerCellName)) {
                val specificCustomPropertiesValue : String =
                    when (val specificCustomProperties = digitalTwin.contents[headerCellName]) {
                        is Map<*, *> -> {
                            JSONObject(specificCustomProperties as MutableMap<String, *>?).toJSONString()
                        }
                        is RawValue -> {
                            specificCustomProperties.rawValue().toString()
                        }
                        else -> {
                            specificCustomProperties.toString()
                        }
                    }
                digitalTwinRowValues.add(specificCustomPropertiesValue)
            }
        }
        digitalTwinRowValues.addAll(defaultHeaderValues)
        return digitalTwinRowValues
    }

    /**
     * Construct a relation data row
     * @param relation a relation
     * @return a relationship data row
     *
     */
    @JvmStatic
    fun constructRelationshipRowValue(relation: BasicRelationship): ArrayList<String> {
        val rowValues = ArrayList<String>()
        rowValues.add(relation.id)
        rowValues.add(relation.sourceId)
        rowValues.add(relation.targetId)
        rowValues.add(relation.name)
        relation.properties.values.forEach {
            val stringValue : String =
                when (it) {
                    is Map<*, *> -> {
                        JSONObject(it as MutableMap<String, *>?).toJSONString()
                    }
                    is RawValue -> {
                        it.rawValue().toString()
                    }
                    else -> {
                        it.toString()
                    }
                }
            rowValues.add(stringValue)
        }
        return rowValues
    }

    /**
     * Construct a digital twin row to print into a CSV
     * @param digitalTwin a digital twin
     * @param digitalTwinsToExport digital twin data rows
     * @param digitalTwinHeaderNameAndType CSV digital twin header names
     * @return the list of CsvData containing digital twins information
     */
    @JvmStatic
    fun constructDigitalTwinInformation(
        digitalTwin: BasicDigitalTwin,
        digitalTwinHeaderNameAndType: MutableMap<String,String>,
        dtHeaderDefaultValues: MutableList<String>,
        digitalTwinsToExport: MutableList<CsvData>
    ): MutableList<CsvData> {
        val modelName = digitalTwin.metadata.modelId.getModelNameFromModelId()
        val csvData = digitalTwinsToExport.find { it.fileName == modelName }
        if (null != csvData) {
            val rowValue =
                constructDigitalTwinRowValue(dtHeaderDefaultValues, csvData.headerNameAndType, digitalTwin)
            csvData.rows.add(rowValue)
        } else {
            val sortedHeaders = digitalTwinHeaderNameAndType.toSortedMap()
            val digitalTwinRowValue = constructDigitalTwinRowValue(
                dtHeaderDefaultValues,
                sortedHeaders,
                digitalTwin
            )
            val rows = mutableListOf<MutableList<String>>()
            rows.add(digitalTwinRowValue)
            digitalTwinsToExport.add(CsvData(modelName, sortedHeaders, rows))
        }
        return digitalTwinsToExport
    }

    /**
     * Construct and fill the list of CsvData containing all information for relations
     * @param relationships a map containing relationships information (relationName, basicRelationships)
     * @param relationshipsToExport the Triple object to fill
     * @return the list of CsvData containing relationship information
     */
    @JvmStatic
    fun constructRelationshipInformation(
        relationships: Map<String, List<BasicRelationship>>,
        relationshipsToExport: MutableList<CsvData>
    ): MutableList<CsvData> {
        relationships.entries.forEach { (relationName, basicRelationships) ->
            val relationInformation = relationshipsToExport.find { it.fileName == relationName }
            if (null != relationInformation) {
                basicRelationships.forEach { relation ->
                    relationInformation.rows.add(constructRelationshipRowValue(relation))
                }
            } else {
                val relationshipHeaderName = relationshipDefaultHeader.toMutableMap()
                if (basicRelationships.isNotEmpty()) {
                    //TODO Handle relationship properties correctly
                    basicRelationships[0].properties.keys.forEach {
                        relationshipHeaderName[it] = "string"
                    }
                    val rows = mutableListOf<MutableList<String>>()
                    basicRelationships.forEach { relation ->
                        rows.add(constructRelationshipRowValue(relation))
                    }
                    relationshipsToExport.add(CsvData(relationName, relationshipHeaderName,rows))
                }
            }
        }
        return relationshipsToExport
    }

    /**
     * Recursively add properties from base model to all the derived models
     * @param baseModel
     * @param modelsList all existing models
     */
    private fun fillExtendingModelsProperties(baseModel: DTDLModelInformation, modelsList: MutableList<DTDLModelInformation>) {
        modelsList
            .filter { it.baseModels?.contains(baseModel.id) ?: false }
            .forEach { extendingModel ->
                baseModel.properties.forEach { (key, value) ->
                    extendingModel.properties.putIfAbsent(key, value)
                }
                fillExtendingModelsProperties(extendingModel, modelsList)
            }
    }

    /**
     * Retrieve and Fill the missing properties for all extension model
     * @param modelInformationList existing Digital Twin Model list
     * @return the modelInformationList completed
     */
    @JvmStatic
    fun retrievePropertiesFromBaseModels(modelInformationList: MutableList<DTDLModelInformation>):MutableList<DTDLModelInformation> {
        modelInformationList
            .filter { it.baseModels == null }
            .forEach { baseModel ->
                fillExtendingModelsProperties(baseModel, modelInformationList)
            }
        return modelInformationList
    }

    /** Get the ADT instance URL*/
    @JvmStatic
    fun getInstanceUrl(): String {
        return configuration.getValue(AZURE_DIGITAL_TWINS_URL_KEY,String::class.java)
    }

    /** Get the ADT instance URL*/
    @JvmStatic
    fun getExportCsvFilesPath(): Optional<String>? {
        return configuration.getOptionalValue(CSM_FETCH_ABSOLUTE_PATH,String::class.java)
    }

    /** Get the number of thread to use*/
    @JvmStatic
    fun getNumberOfThread(): Int{
        val numberOfThreadOptional = configuration.getOptionalValue(CSM_NUMBER_OF_THREAD,Int::class.java)
        // By default, no multithreading
        var numberOfThread = 1
        if (numberOfThreadOptional?.isPresent == true) {
            numberOfThread = numberOfThreadOptional.get()
        }
        return numberOfThread
    }

    /** Get the Azure Tenant Id*/
    @JvmStatic
    fun getAzureTenantId(): String {
        return configuration.getValue(AZURE_TENANT_ID_KEY,String::class.java)
    }

    /** Get the Azure Client Id*/
    @JvmStatic
    fun getAzureClientId(): String {
        return configuration.getValue(AZURE_CLIENT_ID_KEY,String::class.java)
    }

    /** Get the Azure Secret Id*/
    @JvmStatic
    fun getAzureClientSecret(): String {
        return configuration.getValue(AZURE_CLIENT_SECRET_KEY,String::class.java)
    }

    /** Get the twin filters */
    @JvmStatic
    fun getTwinFilters(): Optional<String>? {
        return configuration.getOptionalValue(TWIN_FILTERS,String::class.java)
    }

    /** Get the relationships filters */
    @JvmStatic
    fun getRelationFilters(): Optional<String>? {
        return configuration.getOptionalValue(RELATION_FILTERS,String::class.java)
    }

    /**
     * Construct twin query with filters if specified
     */
    @JvmStatic
    fun constructTwinQuery(filtersConfiguration: Optional<String>?): String {
        var query = "SELECT * FROM digitaltwins"
        if (filtersConfiguration?.isPresent == true) {
            val filters = Klaxon().parse<MutableMap<String, String>>(filtersConfiguration.get()) ?: mutableMapOf()
            if (filters.isNotEmpty()) {
                query = filters.map{ (key, value) -> "T.$key = '$value'"}
                    .joinToString(prefix = "$query T WHERE ", separator = " AND ")
            }
        }
        return query
    }

    /**
     * Construct relationship query with filters if specified
     */
    @JvmStatic
    fun constructRelationshipQuery(filtersConfiguration: Optional<String>?): String {
        var query = "SELECT * FROM RELATIONSHIPS"
        if (filtersConfiguration?.isPresent == true) {
            val filters = Klaxon().parse<MutableMap<String, String>>(filtersConfiguration.get()) ?: mutableMapOf()
            if (filters.isNotEmpty()) {
                query = filters.map{ (key, value) -> "R.$key = '$value'"}
                    .joinToString(prefix = "$query R WHERE ", separator = " AND ")
            }
        }
        return query
    }

}

