package com.cedu.yggdrasilservice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files

class CalendarControllerAuthTest {

    @Test
    fun `login returns static token for known user`() {
        val controller = CalendarController(CalendarService(), AuthService())

        val response = controller.login(CalendarController.LoginRequest("alice"))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("static-token-alice", (response.body as CalendarController.LoginResponse).token)
    }

    @Test
    fun `calendar endpoint requires token`() {
        val controller = CalendarController(CalendarService(), AuthService())

        val response = controller.getCalendar(null)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `calendar endpoints provide user isolated data by token`() {
        val tmp = Files.createTempDirectory("calendar-controller-auth-test")
        val service = CalendarService(tmp.toAbsolutePath().toString() + "/")
        val controller = CalendarController(service, AuthService())

        val aliceFile = MockMultipartFile(
            "calendar_data",
            "calendar.json",
            MediaType.APPLICATION_JSON_VALUE,
            """{"owner":"alice","events":[1]}""".toByteArray()
        )
        val bobFile = MockMultipartFile(
            "calendar_data",
            "calendar.json",
            MediaType.APPLICATION_JSON_VALUE,
            """{"owner":"bob","events":[2]}""".toByteArray()
        )

        val updateAlice = controller.updateCalendar(aliceFile, "Bearer static-token-alice")
        val updateBob = controller.updateCalendar(bobFile, "Bearer static-token-bob")
        val getAlice = controller.getCalendar("Bearer static-token-alice")
        val getBob = controller.getCalendar("Bearer static-token-bob")

        assertEquals(HttpStatus.OK, updateAlice.statusCode)
        assertEquals(HttpStatus.OK, updateBob.statusCode)
        assertEquals(HttpStatus.OK, getAlice.statusCode)
        assertEquals(HttpStatus.OK, getBob.statusCode)
        assertTrue((getAlice.body as String).contains("\"owner\":\"alice\""))
        assertTrue((getBob.body as String).contains("\"owner\":\"bob\""))

        Files.walk(tmp).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
}
