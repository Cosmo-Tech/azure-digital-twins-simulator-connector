package com.cosmotech.connector.adt.impl.callable

import com.cosmotech.connector.commons.pojo.CsvData
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class WriteFiles(private val csvDataToWrite:CsvData,
                 private var exportPath:String) : Supplier<String> {

    companion object {
        val LOGGER: Logger = LogManager.getLogger(WriteFiles::class.java.name)
    }

    override fun get(): String {
        LOGGER.debug("Short Model: ${csvDataToWrite.fileName} , " +
                "CSV Headers: ${csvDataToWrite.headerNameAndType} , " +
                "rows : ${csvDataToWrite.rows}")

        LOGGER.info("Writing file ${csvDataToWrite.fileName} ...")
        val writeFileStart = System.nanoTime()
        if (!exportPath.endsWith("/") ) {
            exportPath = exportPath.plus("/")
        }
        csvDataToWrite.exportDirectory = exportPath
        val directory = File(csvDataToWrite.exportDirectory)
        directory.mkdirs()
        val filePath = csvDataToWrite.writeFile()

        val writeFileTiming = System.nanoTime() - writeFileStart
        LOGGER.info("Writing file ${csvDataToWrite.fileName} Operation took {} ns ({} ms)",
            writeFileTiming,
            TimeUnit.NANOSECONDS.toMillis(writeFileTiming))
        return filePath
    }

}