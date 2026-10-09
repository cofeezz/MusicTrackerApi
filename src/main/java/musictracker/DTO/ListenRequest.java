package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

@Schema(description = "Dados para registrar uma escuta")
public record ListenRequest(

        @NotNull(message = "O ID do usuário é obrigatório")
        @Schema(description = "ID de quem ouviu", example = "1")
        Long userId,

        @NotNull(message = "O ID da faixa é obrigatório")
        @Schema(description = "ID da faixa ouvida", example = "1")
        Long trackId,

        // @PastOrPresent: não faz sentido registrar uma escuta "no futuro".
        // Continua opcional (não tem @NotNull) — se omitido, o Service usa
        // LocalDateTime.now() (check-in rápido).
        @PastOrPresent(message = "A data da escuta não pode estar no futuro")
        @Schema(description = "Quando a escuta aconteceu. Se omitido, usa o momento atual (check-in rápido). "
                + "Não pode ser uma data futura.",
                example = "2026-10-06T20:30:00")
        LocalDateTime listenedAt,

        @Positive(message = "A duração deve ser maior que zero")
        @Schema(description = "Duração ouvida em segundos (opcional)", example = "180")
        Integer durationSeconds
) {
}
