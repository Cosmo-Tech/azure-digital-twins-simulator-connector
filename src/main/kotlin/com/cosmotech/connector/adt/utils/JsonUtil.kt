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
         * Read the key "extends" in an JSON Object
         * If exist returns a pair of { true, "extends_value"}
         * where "extends_value" contains list of root models
         * If not returns a pair of {false, empty list}
         * @return a Pair(Boolean,List<String>) containing minimal extension information
         */
        @JvmStatic
        fun isExtension(jsonModel: JsonObject):Pair<Boolean,List<String>> {
            val extends = jsonModel[DTDL_EXTENDS_KEY]
            return if(null != extends) {
                val extendsAsList = if (extends is String) {
                    listOf(extends)
                } else {
                    jsonModel.array(DTDL_EXTENDS_KEY)!!
                }
                Pair(true, extendsAsList)
            }
            else
                Pair(false, listOf())
        }

    }

}
