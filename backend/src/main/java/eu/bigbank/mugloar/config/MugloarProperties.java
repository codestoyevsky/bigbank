package eu.bigbank.mugloar.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mugloar")
public record MugloarProperties(@Valid @NotNull Api api, @Valid @NotNull Web web, @Valid @NotNull Bot bot) {

    public record Api(
            @NotBlank String baseUrl,
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout,
            @Min(0) int maxRetries) {}

    public record Web(@NotEmpty List<String> allowedOrigins) {}

    public record Bot(@Min(1) int maxTurns) {}
}
