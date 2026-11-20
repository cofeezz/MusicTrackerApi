package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uma posição do top global da comunidade. Dado agregado e anônimo: só números, nenhum nome de usuário")
public record CommunityTopTrackResponse(
        @Schema(description = "Posição no top global (1 = faixa com mais ouvintes)", example = "1")
        int rank,
        @Schema(description = "ID da faixa", example = "7")
        long trackId,
        @Schema(description = "Nome da faixa", example = "Blinding Lights")
        String trackName,
        @Schema(description = "Nome do artista", example = "The Weeknd")
        String artistName,
        @Schema(description = "Quantas pessoas diferentes já ouviram a faixa (só a contagem, sem identificar ninguém)", example = "8")
        long listeners,
        @Schema(description = "Porcentagem dos ouvintes da comunidade que já ouviram a faixa", example = "66.7")
        double listenersPercent
) {
}
