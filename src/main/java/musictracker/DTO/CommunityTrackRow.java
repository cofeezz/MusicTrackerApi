package musictracker.DTO;

public record CommunityTrackRow(
        long trackId,
        String trackName,
        String artistName,
        long listeners
) {
}
