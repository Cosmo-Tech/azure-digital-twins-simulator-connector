// copyright (c) cosmo tech corporation.
// licensed under the mit license.

package com.cosmotech.connector.adt.utils

import com.beust.klaxon.JsonObject
import com.cosmotech.connector.adt.constants.*

/**
 * Utility class for JSON object
 */
class JsonUtil {

    companion object Reader {

        /**
         * Read the 'contents' array of a model data
         * @param jsonModel the JSON Object representing a model
         * @return the list of property names
         */
        @JvmStatic
        fun readPropertiesNameAndType(jsonModel: JsonObject): MutableMap<String,String> {
            val result = mutableMapOf<String, String>()
            jsonModel.array<JsonObject>(DTDL_CONTENTS_KEY)
                ?.filter { it.getOrDefault(DTDL_TYPE_KEY, "")!! == DTDL_PROPERTY_KEY }
                ?.forEach {
                    if( it[DTDL_SCHEMA_KEY] !is JsonObject )
                        result[it[DTDL_NAME_KEY].toString()] = it[DTDL_SCHEMA_KEY].toString()
                    else
                        result[it[DTDL_NAME_KEY].toString()] = "CompositeType"
                }

            return result
        }

        /**
         * Read the key "extends" of a model data
         * If exist returns "extends" as a list of values which are base models of model
         * If not returns null
         * @return null or List<String> containing minimal extension information
         */
        @JvmStatic
        fun readExtension(jsonModel: JsonObject):List<String>? {
            val extends = jsonModel[DTDL_EXTENDS_KEY]
            return if(null != extends) {
                if (extends is String) {
                    listOf(extends)
                } else {
                    jsonModel.array(DTDL_EXTENDS_KEY)!!
                }
            }
            else {
                null
            }
        }

    }

}
