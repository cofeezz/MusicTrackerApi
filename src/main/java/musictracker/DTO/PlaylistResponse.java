package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Dados de uma playlist, com as faixas que ela contém")
public record PlaylistResponse(
        Long id,
        String name,
        List<TrackResponse> tracks
) {
}
