package com.cedu.yggdrasilservice

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
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

        val filePath = Path.of(dataStore).resolve("yggdrasil-calendar.json")
        assertTrue(Files.exists(filePath))

        Files.deleteIfExists(filePath)
        Files.deleteIfExists(tmp)
    }

    @Test
    fun `calendar data is isolated by user in separate directories`() {
        val tmp = Files.createTempDirectory("calendar-user-test")
        val dataStore = tmp.toAbsolutePath().toString() + "/"
        val svc = CalendarService(dataStore)

        val aliceData = MockMultipartFile(
            "calendar_data",
            "calendar.json",
            MediaType.APPLICATION_JSON_VALUE,
            """{"owner":"alice","events":[]}""".toByteArray()
        )
        val bobData = MockMultipartFile(
            "calendar_data",
            "calendar.json",
            MediaType.APPLICATION_JSON_VALUE,
            """{"owner":"bob","events":[]}""".toByteArray()
        )

        svc.updateCalendarDataForUser(aliceData, "1")
        svc.updateCalendarDataForUser(bobData, "2")

        val aliceCalendar = svc.getCalendarDataForUser("1")
        val bobCalendar = svc.getCalendarDataForUser("2")

        assertTrue(aliceCalendar.contains("\"owner\":\"alice\""))
        assertTrue(bobCalendar.contains("\"owner\":\"bob\""))

        // Verify separate directories
        assertTrue(Files.exists(tmp.resolve("1").resolve("yggdrasil-calendar.json")))
        assertTrue(Files.exists(tmp.resolve("2").resolve("yggdrasil-calendar.json")))

        Files.walk(tmp).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
}

