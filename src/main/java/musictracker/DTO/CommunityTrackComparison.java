package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uma das suas faixas comparada com a comunidade. Só números agregados: nenhum nome, e-mail ou ID de outro usuário aparece")
public record CommunityTrackComparison(
        @Schema(description = "Posição da faixa no SEU ranking (1 = a que você mais ouviu)", example = "1")
        int rank,
        @Schema(description = "ID da faixa", example = "7")
        long trackId,
        @Schema(description = "Nome da faixa", example = "Blinding Lights")
        String trackName,
        @Schema(description = "Nome do artista", example = "The Weeknd")
        String artistName,
        @Schema(description = "Quantas vezes VOCÊ ouviu a faixa", example = "12")
        long yourTimesListened,
        @Schema(description = "Quantas pessoas diferentes ouviram a faixa (incluindo você)", example = "8")
        long listeners,
        @Schema(description = "Porcentagem dos ouvintes da comunidade que ouviram esta faixa", example = "66.7")
        double listenersPercent,
        @Schema(description = "Você está entre os X% que mais ouviram esta faixa (quanto menor, melhor; 10.0 = top 10%)", example = "12.5")
        double yourTopPercent
) {
}
