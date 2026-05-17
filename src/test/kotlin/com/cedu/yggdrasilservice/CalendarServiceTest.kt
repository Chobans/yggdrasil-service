package com.cedu.yggdrasilservice

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class CalendarServiceTest {

    @Test
    fun `getCalendarData creates template when missing`() {
        val tmp = Files.createTempDirectory("calendar-test")
        val dataStore = tmp.toAbsolutePath().toString() + "/"
        val svc = CalendarService(dataStore)

        val content = svc.getCalendarData()
        assertNotNull(content)
        assertTrue(content.contains("\"version\""))
        assertTrue(content.contains("\"generatedAt\""))
        assertTrue(content.contains("\"events\""))

        val filePath = Path.of(dataStore).resolve("yggdrasil-data.json")
        assertTrue(Files.exists(filePath))

        // cleanup
        Files.deleteIfExists(filePath)
        Files.deleteIfExists(tmp)
    }
}
