package com.cedu.yggdrasilservice

import org.springframework.stereotype.Service

@Service
class AuthService {

    private val userToToken = mapOf(
        "alice" to "static-token-alice",
        "bob" to "static-token-bob"
    )
    private val tokenToUser = userToToken.entries.associate { (user, token) -> token to user }

    fun authenticate(username: String): String? = userToToken[username]

    fun resolveUserId(authorizationHeader: String?): String? {
        val token = authorizationHeader
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()
            ?: return null

        return tokenToUser[token]
    }
}
