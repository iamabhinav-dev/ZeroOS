package com.zeroos.core.logging

import java.io.File
import java.time.Clock
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Zips all log files and crash reports for sharing (F17.9). Old exports in [outputDir] are replaced. */
class LogExporter(private val store: LogFileStore, private val outputDir: File, private val clock: Clock) {
    fun export(): File {
        outputDir.mkdirs()
        outputDir.listFiles()?.forEach { it.delete() }
        val zip = File(outputDir, "zeroos-logs-${clock.millis()}.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            (store.logFiles() + store.crashReports()).forEach { file ->
                out.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(out) }
                out.closeEntry()
            }
        }
        return zip
    }
}
