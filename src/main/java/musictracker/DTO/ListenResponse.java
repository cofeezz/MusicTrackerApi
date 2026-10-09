package musictracker.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Dados de um registro de escuta")
public record ListenResponse(
        Long id,
        UserResponse user,
        TrackResponse track,
        LocalDateTime listenedAt,
        Integer durationSeconds
) {
}
