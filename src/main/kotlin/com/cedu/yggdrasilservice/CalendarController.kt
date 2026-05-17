package com.cedu.yggdrasilservice

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.parameters.RequestBody as OasRequestBody
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController("/api/v1/yggdrasil")
class CalendarController(
    private val calendarService: CalendarService,
    private val authService: AuthService
) {
    data class LoginRequest(val login: String, val passwordHash: String)
    data class LoginResponse(val token: String)

    @PostMapping("/auth/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        val token = authService.authenticate(request.login, request.passwordHash)
            ?: return ResponseEntity("Неверные учетные данные", HttpStatus.UNAUTHORIZED)

        return ResponseEntity.ok(LoginResponse(token))
    }

    @Operation(summary = "Получить календарь", description = "Возвращает JSON-файл календаря как вложение")
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Файл календаря",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(type = "string", format = "binary"),
                    examples = [
                        ExampleObject(
                            name = "sample",
                            value = "{\"events\":[{\"id\":1,\"title\":\"Meeting\",\"time\":\"2026-01-01T10:00:00\"}]}"
                        )
                    ]
                )
            ]
        ),
        ApiResponse(responseCode = "404", description = "Календарь не найден"),
        ApiResponse(responseCode = "500", description = "Ошибка сервиса")
    ])
    @GetMapping("/calendar", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getCalendar(
        @RequestHeader(value = "Authorization", required = false) authorization: String?,
    ): ResponseEntity<Any> {
        val userId = authService.resolveUserId(authorization)
            ?: return ResponseEntity("Требуется валидный токен", HttpStatus.UNAUTHORIZED)

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(calendarService.getCalendarDataForUser(userId))
    }

    @Operation(summary = "Обновить календарь", description = "Загрузить JSON-файл (поле calendar_data) и сохранить его, перезаписывая существующий")
    @OasRequestBody(
        description = "Multipart/form-data с полем calendar_data (файл .json)",
        required = true,
        content = [
            Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = Schema(type = "string", format = "binary"),
                examples = [
                    ExampleObject(
                        name = "sample",
                        value = "{\"events\":[{\"id\":1,\"title\":\"Test event\",\"time\":\"2026-01-01T12:00:00\"}]}"
                    )
                ]
            )
        ]
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Календарь успешно обновлен"),
        ApiResponse(responseCode = "400", description = "Неверный файл / формат"),
        ApiResponse(responseCode = "500", description = "Ошибка сервиса")
    ])
    @PostMapping(
        "/calendar",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun updateCalendar(
        @RequestParam("calendar_data") calendarData: MultipartFile,
        @RequestHeader(value = "Authorization", required = false) authorization: String?,
    ): ResponseEntity<Any> {
        val userId = authService.resolveUserId(authorization)
            ?: return ResponseEntity("Требуется валидный токен", HttpStatus.UNAUTHORIZED)

        return try {
            val updatedFile = calendarService.updateCalendarDataForUser(calendarData, userId)
            ResponseEntity.ok("Календарь успешно обновлен: ${updatedFile.name}")
        } catch (ex: IllegalArgumentException) {
            ResponseEntity(ex.message ?: "Ошибка с файлом", HttpStatus.BAD_REQUEST)
        } catch (ex: Exception) {
            ResponseEntity(ex.message ?: "Ошибка логики сервиса", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}
