// copyright (c) cosmo tech corporation.
// licensed under the mit license.

package com.cosmotech.connector.adt.pojos

/**
 * Represent a DTDL Model
 * @property id represents the modelId
 * @property baseModels tells us which DTDL models are extended by the current model
 * @property properties contains properties map (name, type) for this specific model
 * @property dtdlModel store the definition of the model in DTDL format
 */
data class DTDLModelInformation(val id:String,
                                val baseModels:List<String>?,
                                val properties:MutableMap<String,String>,
                                val dtdlModel:String) {
}
