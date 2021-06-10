// copyright (c) cosmo tech corporation.
// licensed under the mit license.

package com.cosmotech.connector.adt.constants

// ############################
// ## Azure Digital Twins
// ############################

/** Environment variable in which the Azure tenant is stored */
const val AZURE_TENANT_ID_KEY = "azure.tenant.id"

/** Environment variable in which the Azure tenant is stored */
const val AZURE_CLIENT_ID_KEY = "azure.client.id"

/** Environment variable in which the Azure client secret is stored */
const val AZURE_CLIENT_SECRET_KEY = "azure.client.secret"

/** Environment variable in which the ADT endpoint is stored */
const val AZURE_DIGITAL_TWINS_URL_KEY = "azure.digital.twins.url"

/** Environment variable in which the absolute export path for CSVs is stored */
const val CSM_FETCH_ABSOLUTE_PATH = "csm.fetch.absolute.path"

/**
 * Default header cell name for digital twins
 */
val modelDefaultProperties = mutableMapOf("id" to "string")

/**
 * Default header cell name for relations
 */
val relationshipDefaultHeader = mutableMapOf("id" to "string", "source" to "string","target" to "string","name" to "string")

// ############################
// ## DTDL
// ############################

/**
 * Name of the DTDL extends key
 */
const val DTDL_EXTENDS_KEY = "extends"

/**
 * Name of the DTDL name key
 */
const val DTDL_NAME_KEY = "name"

/**
 * Name of the DTDL schema key
 */
const val DTDL_SCHEMA_KEY = "schema"

/**
 * Name of the DTDL contents key
 */
const val DTDL_CONTENTS_KEY = "contents"

/**
 * Name of the DTDL type key
 */
const val DTDL_TYPE_KEY = "@type"

/**
 * Name of the DTDL Property name definition
 */
const val DTDL_PROPERTY_KEY = "Property"
