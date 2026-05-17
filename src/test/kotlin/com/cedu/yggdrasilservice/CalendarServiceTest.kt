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

        val filePath = Path.of(dataStore).resolve("yggdrasil-data.json")
        assertTrue(Files.exists(filePath))

        // cleanup
        Files.deleteIfExists(filePath)
        Files.deleteIfExists(tmp)
    }

    @Test
    fun `calendar data is isolated by user`() {
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

        svc.updateCalendarDataForUser(aliceData, "alice")
        svc.updateCalendarDataForUser(bobData, "bob")

        val aliceCalendar = svc.getCalendarDataForUser("alice")
        val bobCalendar = svc.getCalendarDataForUser("bob")

        assertTrue(aliceCalendar.contains("\"owner\":\"alice\""))
        assertTrue(bobCalendar.contains("\"owner\":\"bob\""))

        Files.walk(tmp).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
}
