package com.cedu.yggdrasilservice

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption


@Service
class CalendarService {

    val dataStorePath = "/data/calendar/"
    val calendarName = "yggdrasil-data.json"

    fun getCalendarData(fileName: String = calendarName): String {
        val fsPath = Paths.get(dataStorePath, fileName)
        if (Files.exists(fsPath)) return fsPath.toFile().readText()
        throw FileNotFoundException(fileName)
    }

    fun updateCalendarData(file: MultipartFile, fileName: String = calendarName): File {
        if (file.isEmpty) throw IllegalArgumentException("file can not be empty")

        if (!fileName.endsWith(".json", ignoreCase = true)) {
            throw IllegalArgumentException("File name must have .json extension")
        }

        val bytes = try {
            file.bytes
        } catch (e: IOException) {
            throw IOException("Failed to read uploaded file", e)
        }

        // Валидация JSON
        try {
            ObjectMapper().readTree(bytes.inputStream())
        } catch (e: Exception) {
            throw IllegalArgumentException("Uploaded file is not valid JSON", e)
        }

        val dirPath: Path = Paths.get(dataStorePath)
        Files.createDirectories(dirPath)

        val targetPath = dirPath.resolve(fileName)
        Files.write(targetPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        return targetPath.toFile()
    }
}