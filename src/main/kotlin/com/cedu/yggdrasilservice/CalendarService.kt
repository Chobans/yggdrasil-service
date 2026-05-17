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
import java.security.MessageDigest
import java.time.Instant


@Service
class CalendarService(
    private val dataStorePath: String = "/data/calendar/"
) {

    val calendarName = "yggdrasil-data.json"

    fun getCalendarData(fileName: String = calendarName): String {
        val fsPath = Paths.get(dataStorePath, fileName)
        if (Files.exists(fsPath)) return fsPath.toFile().readText()

        // Если файл не найден — создаём шаблонный JSON с пустым массивом events
        val dirPath: Path = Paths.get(dataStorePath)
        Files.createDirectories(dirPath)

        val mapper = ObjectMapper()
        val root: ObjectNode = mapper.createObjectNode()
        root.put("version", 1)
        root.put("generatedAt", Instant.now().toString())
        val events: ArrayNode = mapper.createArrayNode()
        root.set<ArrayNode>("events", events)

        val jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root)

        try {
            Files.write(fsPath, jsonStr.toByteArray(), StandardOpenOption.CREATE_NEW)
        } catch (e: IOException) {
            throw IOException("Failed to create calendar file at $fsPath", e)
        }

        return jsonStr
    }

    fun getCalendarDataForUser(userId: String): String {
        return getCalendarData(fileName = userCalendarFileName(userId))
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
        return updateCalendarData(file = file, fileName = userCalendarFileName(userId))
    }

    private fun userCalendarFileName(userId: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(userId.toByteArray())
        val hashedUserId = digest.joinToString("") { "%02x".format(it) }.take(24)
        return "yggdrasil-data-$hashedUserId.json"
    }
}
