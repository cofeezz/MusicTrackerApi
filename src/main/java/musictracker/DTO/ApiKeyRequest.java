package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciais do usuário para gerar uma nova chave de API")
public record ApiKeyRequest(

        @NotBlank(message = "O nome de usuário não pode estar em branco")
        @Size(max = 50, message = "O nome de usuário deve ter no máximo 50 caracteres")
        @Schema(description = "Nome de usuário da conta", example = "cofeezz")
        String username,

        @NotBlank(message = "A senha não pode estar em branco")
        @Schema(description = "Senha da conta", example = "senha123")
        String password,

        @Size(max = 50, message = "O nome da chave deve ter no máximo 50 caracteres")
        @Schema(description = "Nome para identificar a chave (opcional; padrão: default)", example = "app-celular")
        String name
) {
}
