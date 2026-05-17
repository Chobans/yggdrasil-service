package com.cedu.yggdrasilservice

import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

data class User(val id: String, val login: String, val passwordHash: String, val token: String)

@Component
class UserRepository(val csvPath: String = "/data/users/users.csv") {

    private val users: List<User> by lazy { loadUsers() }

    fun findByLoginAndPasswordHash(login: String, passwordHash: String): User? =
        users.find { it.login == login && it.passwordHash == passwordHash }

    fun findByToken(token: String): User? =
        users.find { it.token == token }

    private fun loadUsers(): List<User> {
        val path = Paths.get(csvPath)
        if (!Files.exists(path)) {
            Files.createDirectories(path.parent)
            Files.write(
                path,
                (
                    "id,login,passwordHash,token\n" +
                    "1,cedu,gfhjkm,cedu-token\n"
                ).toByteArray(),
                StandardOpenOption.CREATE_NEW
            )
        }

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
