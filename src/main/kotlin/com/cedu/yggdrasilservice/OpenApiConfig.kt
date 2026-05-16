package com.cedu.yggdrasilservice

import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.OpenAPI
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Yggdrasil Service API")
                    .version("0.0.1")
                    .description("API для работы с календарями Yggdrasil")
                    .contact(Contact().name("Yggdrasil Team").email("dev@example.com"))
            )
    }
}
