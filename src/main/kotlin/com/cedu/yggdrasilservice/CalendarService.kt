package com.cedu.yggdrasilservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant


@Service
class CalendarService(
    private val dataStorePath: String = "/data/calendar/"
) {

    val calendarName = "yggdrasil-calendar.json"

    fun getCalendarData(fileName: String = calendarName): String {
        val fsPath = Paths.get(dataStorePath, fileName)
        if (Files.exists(fsPath)) return fsPath.toFile().readText()

        // Если файл не найден — создаём шаблонный JSON с пустым массивом events
        val dirPath: Path = Paths.get(dataStorePath)
        Files.createDirectories(dirPath)

        val jsonStr = emptyCalendarJson()

        try {
            Files.write(fsPath, jsonStr.toByteArray(), StandardOpenOption.CREATE_NEW)
        } catch (e: IOException) {
            throw IOException("Failed to create calendar file at $fsPath", e)
        }

        return jsonStr
    }

    fun getCalendarDataForUser(userId: String): String {
        val userDir = Paths.get(dataStorePath, userId)
        val filePath = userDir.resolve(calendarName)

        if (Files.exists(filePath)) return filePath.toFile().readText()

        Files.createDirectories(userDir)
        val jsonStr = emptyCalendarJson()

        try {
            Files.write(filePath, jsonStr.toByteArray(), StandardOpenOption.CREATE_NEW)
        } catch (e: IOException) {
            throw IOException("Failed to create calendar file at $filePath", e)
        }

        return jsonStr
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

    fun updateCalendarDataForUser(file: MultipartFile, userId: String): File {
        if (file.isEmpty) throw IllegalArgumentException("file can not be empty")

        val bytes = try {
            file.bytes
        } catch (e: IOException) {
            throw IOException("Failed to read uploaded file", e)
        }

        try {
            ObjectMapper().readTree(bytes.inputStream())
        } catch (e: Exception) {
            throw IllegalArgumentException("Uploaded file is not valid JSON", e)
        }

        val userDir = Paths.get(dataStorePath, userId)
        Files.createDirectories(userDir)

        val filePath = userDir.resolve(calendarName)
        Files.write(filePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        return filePath.toFile()
    }

    private fun emptyCalendarJson(): String {
        val mapper = ObjectMapper()
        val root: ObjectNode = mapper.createObjectNode()
        root.put("version", 1)
        root.put("generatedAt", Instant.now().toString())
        val events: ArrayNode = mapper.createArrayNode()
        root.set<ArrayNode>("events", events)
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root)
    }
}

