package com.cedu.yggdrasilservice

import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths

data class User(val id: String, val login: String, val passwordHash: String, val token: String)

@Component
class UserRepository(val csvPath: String = "/data/users.csv") {

    fun findByLoginAndPasswordHash(login: String, passwordHash: String): User? =
        loadUsers().find { it.login == login && it.passwordHash == passwordHash }

    fun findByToken(token: String): User? =
        loadUsers().find { it.token == token }

    private fun loadUsers(): List<User> {
        val path = Paths.get(csvPath)
        if (!Files.exists(path)) return emptyList()

        return Files.readAllLines(path)
            .drop(1)
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(",")
                if (parts.size >= 4) {
                    User(
                        id = parts[0].trim(),
                        login = parts[1].trim(),
                        passwordHash = parts[2].trim(),
                        token = parts[3].trim()
                    )
                } else null
            }
    }
}
