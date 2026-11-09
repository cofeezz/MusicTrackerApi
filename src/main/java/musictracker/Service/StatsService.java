package musictracker.Service;

import musictracker.DTO.TopTrackDTO;
import musictracker.Repository.ListenRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        userService.getEntityOrThrow(userId);
        return listenRepository.findTopTracksSince(userId, null, PageRequest.of(0, limit));
    }

    public List<TopTrackDTO> topTracksMonthly(Long userId, int limit) {
        userService.getEntityOrThrow(userId);
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return listenRepository.findTopTracksSince(userId, startOfMonth, PageRequest.of(0, limit));
    }
}
