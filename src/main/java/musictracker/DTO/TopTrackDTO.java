package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

// Resultado agregado de uma consulta de ranking (top 10 geral / top 3 do mes).
@Schema(description = "Uma posição no ranking de faixas mais ouvidas")
public record TopTrackDTO(
        long trackId,
        String trackName,
        String artistName,
        long timesListened,
        long totalSeconds
) {
}
