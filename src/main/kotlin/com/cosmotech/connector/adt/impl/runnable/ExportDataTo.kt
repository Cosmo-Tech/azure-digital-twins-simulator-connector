package com.cosmotech.connector.adt.impl.runnable

import com.cosmotech.connector.adt.impl.AzureDigitalTwinsConnectorMultithread
import com.cosmotech.connector.commons.pojo.CsvData
import java.io.File
import java.util.Optional
import java.util.concurrent.locks.ReentrantLock

class ExportDataTo(private val csvData: CsvData,
                       private val csvFilesPath: Optional<String>?,
                       private val exportDataToMutex: ReentrantLock) : Runnable{

    public override fun run(){
        AzureDigitalTwinsConnectorMultithread.LOGGER.debug("Short Model: ${csvData.fileName} , " +
                "CSV Headers: ${csvData.headerNameAndType} , " +
                "rows : ${csvData.rows}")
        if (csvFilesPath?.isPresent == true) {
            var exportDirectory = csvFilesPath.get()
            if (!exportDirectory.endsWith("/") ) {
                exportDirectory = exportDirectory.plus("/")
            }
            csvData.exportDirectory = exportDirectory
        }
        val directory = File(csvData.exportDirectory)
        exportDataToMutex.lock()
        directory.mkdirs()
        csvData.writeFile()
        exportDataToMutex.unlock()
    }
}