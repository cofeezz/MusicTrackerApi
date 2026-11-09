package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Chave de API recém-criada. O valor completo em 'key' só aparece nesta resposta")
public record ApiKeyCreatedResponse(

        @Schema(description = "ID da chave", example = "1")
        Long id,

        @Schema(description = "Nome dado à chave", example = "app-celular")
        String name,

        @Schema(description = "Início da chave, só para identificá-la depois", example = "mt_Ab3dEf9x")
        String keyPrefix,

        @Schema(description = "A chave completa. Guarde agora: ela não poderá ser consultada novamente",
                example = "mt_Ab3dEf9xKq2LmN8pRsTuVwXyZ0123456789abcdefg")
        String key,

        @Schema(description = "Quando a chave foi criada", example = "2026-11-06T14:32:10")
        LocalDateTime createdAt
) {
}
