package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Comparação anônima do seu ranking com a comunidade, no estilo do Spotify: só porcentagens, sem citar nenhum usuário")
public record CommunityComparisonResponse(
        @Schema(description = "Quantas pessoas, no total, já registraram ao menos uma escuta na API (só a contagem)", example = "12")
        long totalListeners,
        @Schema(description = "As suas faixas mais ouvidas, cada uma comparada com a comunidade")
        List<CommunityTrackComparison> tracks
) {
}
