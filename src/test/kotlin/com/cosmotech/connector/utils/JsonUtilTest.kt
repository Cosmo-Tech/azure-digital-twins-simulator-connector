// copyright (c) cosmo tech corporation.
// licensed under the mit license.

package com.cosmotech.connector.utils

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.cosmotech.connector.AbstractUnitTest
import com.cosmotech.connector.adt.utils.JsonUtil
import java.io.StringReader
import kotlin.test.Test
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
    fun `readProperties Name And Type`() {
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
    fun `readExtension without extends`() {
        val extensionList = JsonUtil.Reader.readExtension(dtJsonWithoutExtendsModel)
        assertEquals(extensionList,null, "Check if the check for the 'extends' properties is correct")
    }

    @Test
    fun `readExtension with string type extends`() {
        val extensionList = JsonUtil.Reader.readExtension(dtJsonWithStringExtendsModel)
        assertFalse { extensionList is JsonArray }
        assertEquals(extensionList,
                     listOf("dtmi:com:cosmotech:supply:Operation;1"),
                     "Check if the check for the 'extends' properties is correct")
    }

    @Test
    fun `readExtension with list type extends`() {
        val extensionList = JsonUtil.Reader.readExtension(dtJsonWithListExtendsModel)
        assertTrue { extensionList is JsonArray }
        assertEquals(listOf(
                        "dtmi:com:cosmotech:supply:Operation;1",
                        "dtmi:com:cosmotech:supply:Transport;1"),
                    extensionList?.toList(),
                    "Check if the check for the 'extends' properties is correct")
    }
}
