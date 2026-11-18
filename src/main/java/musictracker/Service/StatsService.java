package musictracker.Service;

import musictracker.DTO.CommunityComparisonResponse;
import musictracker.DTO.CommunityTopTrackResponse;
import musictracker.DTO.CommunityTrackComparison;
import musictracker.DTO.CommunityTrackRow;
import musictracker.DTO.TopTrackDTO;
import musictracker.Repository.ListenRepository;
import musictracker.Security.AuthenticatedUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final ListenRepository listenRepository;

    private final UserService userService;

    public StatsService(ListenRepository listenRepository, UserService userService) {
        this.listenRepository = listenRepository;
        this.userService = userService;
    }

    public List<TopTrackDTO> topTracks(Long userId, int limit) {
        AuthenticatedUser.requireSame(userId, "Você só pode ver o seu próprio ranking.");
        userService.getEntityOrThrow(userId);
        return listenRepository.findTopTracksSince(userId, null, PageRequest.of(0, limit));
    }

    public List<TopTrackDTO> topTracksMonthly(Long userId, int limit) {
        AuthenticatedUser.requireSame(userId, "Você só pode ver o seu próprio ranking.");
        userService.getEntityOrThrow(userId);
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return listenRepository.findTopTracksSince(userId, startOfMonth, PageRequest.of(0, limit));
    }

    public CommunityComparisonResponse communityComparison(Long userId, int limit) {
        AuthenticatedUser.requireSame(userId, "Você só pode comparar o seu próprio ranking.");
        userService.getEntityOrThrow(userId);
        List<TopTrackDTO> ranking = listenRepository.findTopTracksSince(userId, null, PageRequest.of(0, limit));
        long totalListeners = listenRepository.countListeners();
        List<CommunityTrackComparison> tracks = IntStream.range(0, ranking.size())
                .mapToObj(index -> {
                    TopTrackDTO item = ranking.get(index);
                    List<Long> counts = listenRepository.countListensPerListener(item.trackId());
                    return new CommunityTrackComparison(
                            index + 1,
                            item.trackId(),
                            item.trackName(),
                            item.artistName(),
                            item.timesListened(),
                            counts.size(),
                            CommunityMath.percent(counts.size(), totalListeners),
                            CommunityMath.topPercent(counts, item.timesListened()));
                })
                .toList();
        return new CommunityComparisonResponse(totalListeners, tracks);
    }

    public List<CommunityTopTrackResponse> communityTopTracks(int limit) {
        long totalListeners = listenRepository.countListeners();
        List<CommunityTrackRow> rows = listenRepository.findCommunityTopTracks(PageRequest.of(0, limit));
        return IntStream.range(0, rows.size())
                .mapToObj(index -> {
                    CommunityTrackRow row = rows.get(index);
                    return new CommunityTopTrackResponse(
                            index + 1,
                            row.trackId(),
                            row.trackName(),
                            row.artistName(),
                            row.listeners(),
                            CommunityMath.percent(row.listeners(), totalListeners));
                })
                .toList();
    }
}
