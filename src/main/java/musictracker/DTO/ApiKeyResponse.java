package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Dados de uma chave de API (o valor completo nunca é exibido de novo)")
public record ApiKeyResponse(

        @Schema(description = "ID da chave", example = "1")
        Long id,

        @Schema(description = "Nome dado à chave", example = "app-celular")
        String name,

        @Schema(description = "Início da chave, só para identificá-la", example = "mt_Ab3dEf9x")
        String keyPrefix,

        @Schema(description = "Quando a chave foi criada", example = "2026-11-06T14:32:10")
        LocalDateTime createdAt,

        @Schema(description = "Se a chave foi revogada (chaves revogadas deixam de funcionar)", example = "false")
        boolean revoked
) {
}
