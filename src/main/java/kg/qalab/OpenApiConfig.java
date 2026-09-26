package kg.qalab;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "QA Lab API",
        version = "1.0.0",
        description = "Безопасная учебная песочница для практики HTTP, REST и API-тестирования."
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "opaque",
    description = "Вставьте токен, полученный после /api/auth/register или /api/auth/login."
)
public class OpenApiConfig {
}
