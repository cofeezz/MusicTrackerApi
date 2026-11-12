package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uma posição do ranking na versão 2: acrescenta a posição e o tempo em minutos")
public record TopTrackV2Response(

        @Schema(description = "Posição no ranking (1 = mais ouvida)", example = "1")
        int rank,

        @Schema(description = "ID da faixa", example = "7")
        long trackId,

        @Schema(description = "Nome da faixa", example = "Blinding Lights")
        String trackName,

        @Schema(description = "Nome do artista", example = "The Weeknd")
        String artistName,

        @Schema(description = "Quantas vezes a faixa foi ouvida", example = "12")
        long timesListened,

        @Schema(description = "Tempo total ouvido, em segundos", example = "2400")
        long totalSeconds,

        @Schema(description = "Tempo total ouvido, em minutos (uma casa decimal)", example = "40.0")
        double totalMinutes
) {

    public static TopTrackV2Response of(int rank, TopTrackDTO track) {
        return new TopTrackV2Response(
                rank,
                track.trackId(),
                track.trackName(),
                track.artistName(),
                track.timesListened(),
                track.totalSeconds(),
                Math.round(track.totalSeconds() / 6.0) / 10.0);
    }
}
