package com.cedu.yggdrasilservice

import org.springframework.stereotype.Service

@Service
class AuthService(private val userRepository: UserRepository) {

    fun authenticate(login: String, passwordHash: String): String? =
        userRepository.findByLoginAndPasswordHash(login, passwordHash)?.token

    fun resolveUserId(authorizationHeader: String?): String? {
        val token = authorizationHeader
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()
            ?: return null

        return userRepository.findByToken(token)?.id
    }
}
