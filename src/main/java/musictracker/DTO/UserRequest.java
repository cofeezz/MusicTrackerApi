package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar ou atualizar um usuário")
public record UserRequest(

        @NotBlank(message = "O nome de usuário não pode estar em branco")
        @Size(min = 3, max = 50, message = "O nome de usuário deve ter entre 3 e 50 caracteres")
        @Schema(description = "Nome de usuário", example = "cofeezz")
        String username,

        @NotBlank(message = "O email não pode estar em branco")
        @Email(message = "Email inválido")
        @Schema(description = "Email do usuário", example = "cof@example.com")
        String email,

        @NotBlank(message = "A senha não pode estar em branco")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        @Schema(description = "Senha (mínimo 6 caracteres)", example = "senha123")
        String password
) {
}
