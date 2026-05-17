package com.cedu.yggdrasilservice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files

class CalendarControllerAuthTest {

    private fun createTempUserCsv(): String {
        val csvFile = Files.createTempFile("users-test", ".csv").toFile()
        csvFile.writeText(
            "id,login,passwordHash,token\n" +
            "1,alice,hash-alice,static-token-alice\n" +
            "2,bob,hash-bob,static-token-bob\n"
        )
        return csvFile.absolutePath
    }

    @Test
    fun `login returns static token for known user`() {
        val csvPath = createTempUserCsv()
        val controller = CalendarController(CalendarService(), AuthService(UserRepository(csvPath)))

        val response = controller.login(CalendarController.LoginRequest("alice", "hash-alice"))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("static-token-alice", (response.body as CalendarController.LoginResponse).token)
    }

    @Test
    fun `login returns 401 for wrong password`() {
        val csvPath = createTempUserCsv()
        val controller = CalendarController(CalendarService(), AuthService(UserRepository(csvPath)))

        val response = controller.login(CalendarController.LoginRequest("alice", "wrong-hash"))

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `calendar endpoint requires token`() {
        val csvPath = createTempUserCsv()
        val controller = CalendarController(CalendarService(), AuthService(UserRepository(csvPath)))

        val response = controller.getCalendar(null)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `calendar endpoints provide user isolated data by token`() {
        val csvPath = createTempUserCsv()
        val tmp = Files.createTempDirectory("calendar-controller-auth-test")
        val service = CalendarService(tmp.toAbsolutePath().toString() + "/")
        val controller = CalendarController(service, AuthService(UserRepository(csvPath)))

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

        // Verify data is stored in separate per-user directories
        val aliceDir = tmp.resolve("1")
        val bobDir = tmp.resolve("2")
        assertTrue(Files.exists(aliceDir.resolve("yggdrasil-calendar.json")))
        assertTrue(Files.exists(bobDir.resolve("yggdrasil-calendar.json")))

        Files.walk(tmp).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
}

