// copyright (c) cosmo tech corporation.
// licensed under the mit license.

package com.cosmotech.connector.utils

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.cosmotech.connector.AbstractUnitTest
import com.cosmotech.connector.adt.constants.DTDL_EXTENDS_KEY
import com.cosmotech.connector.adt.utils.JsonUtil
import org.junit.Test
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for JsonUtil methods
 */
class JsonUtilTest: AbstractUnitTest() {

    private val dtJsonModel: JsonObject = Klaxon()
        .parseJsonObject(
            StringReader(
                getResourceFile("utils/dt-model-template.json")
                    .toFile()
                    .readText(Charsets.UTF_8)
            )
        )

    private val dtJsonWithStringExtendsModel: JsonObject = Klaxon()
        .parseJsonObject(
            StringReader(
                getResourceFile("utils/dt-with-string-extends.json")
                    .toFile()
                    .readText(Charsets.UTF_8)
            )
        )

    private val dtJsonWithListExtendsModel: JsonObject = Klaxon()
        .parseJsonObject(
            StringReader(
                getResourceFile("utils/dt-with-list-extends.json")
                    .toFile()
                    .readText(Charsets.UTF_8)
            )
        )

    private val dtJsonWithoutExtendsModel: JsonObject = Klaxon()
        .parseJsonObject(
            StringReader(
                getResourceFile("utils/dt-with-no-extends.json")
                    .toFile()
                    .readText(Charsets.UTF_8)
            )
        )

    @Test
    fun test_readPropertiesNameAndType() {
        val readPropertiesNameAndType = JsonUtil.Reader.readPropertiesNameAndType(dtJsonModel)
        assertEquals(
            readPropertiesNameAndType,
            mapOf(
                "TimeStepDuration" to "string",
                "TimeStepPerCycle" to "integer",
                "NumberOfCycle" to "integer",
                "StartingDate" to "dateTime",
                "ManageBacklogQuantities" to "boolean",
                "EnforceProductionPlan" to "boolean",
                "OptimizationObjective" to "CompositeType",
                "Demands" to "CompositeType"
            ),
            "Check that the list of Pair if the expected one"
        )
    }

    @Test
    fun test_isExtension_without_extends() {
        val extensionPair = JsonUtil.Reader.isExtension(dtJsonWithoutExtendsModel)
        assertFalse(extensionPair.first, "Check if the check for the '$DTDL_EXTENDS_KEY' properties is correct")
        assertEquals(extensionPair.second,listOf(), "Check if the check for the 'extends' properties is correct")
    }

    @Test
    fun test_isExtension_with_string_type_extends() {
        val extensionPair = JsonUtil.Reader.isExtension(dtJsonWithStringExtendsModel)
        assertTrue(extensionPair.first, "Check if the check for the '$DTDL_EXTENDS_KEY' properties is correct")
        assertEquals(extensionPair.second,
                     listOf("dtmi:com:cosmotech:supply:Operation;1"),
                     "Check if the check for the 'extends' properties is correct")
    }

    @Test
    fun test_isExtension_with_list_type_extends() {
        val extensionPair = JsonUtil.Reader.isExtension(dtJsonWithListExtendsModel)
        assertTrue(extensionPair.first, "Check if the check for the '$DTDL_EXTENDS_KEY' properties is correct")
        assertEquals(extensionPair.second,
                     listOf("dtmi:com:cosmotech:supply:Operation;1","dtmi:com:cosmotech:supply:Transport;1"),
                     "Check if the check for the 'extends' properties is correct")
    }
}
