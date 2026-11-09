package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uma posição no ranking de faixas mais ouvidas")
public record TopTrackDTO(
        long trackId,
        String trackName,
        String artistName,
        long timesListened,
        long totalSeconds
) {
}
