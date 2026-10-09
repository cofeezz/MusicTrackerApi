package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// Formato padrao de erro devolvido pelo GlobalExceptionHandler.
// Todo erro da API (400, 404, 500...) usa exatamente este formato,
// então o cliente só precisa saber ler um único "shape" de erro.
@Schema(description = "Formato padrão de erro usado por toda a API")
public record APIError(

        @Schema(description = "Momento em que o erro ocorreu", example = "2026-10-08T14:32:10")
        LocalDateTime timestamp,

        @Schema(description = "Código de status HTTP", example = "400")
        int status,

        @Schema(description = "Nome do status HTTP", example = "BAD_REQUEST")
        String error,

        @Schema(description = "Mensagem explicando o que deu errado", example = "name: O nome não pode estar em branco")
        String message,

        @Schema(description = "Caminho da requisição que gerou o erro", example = "/artists")
        String path
) {
}
