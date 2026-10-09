package musictracker.Service;

import musictracker.DTO.TopTrackDTO;
import musictracker.Repository.ListenRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Não é CRUD de uma entidade própria — é a "cápsula sonora": ranking
// agregado em cima dos registros de Listen.
@Service
@Transactional(readOnly = true)
public class StatsService {

    private final ListenRepository listenRepository;

    public StatsService(ListenRepository listenRepository) {
        this.listenRepository = listenRepository;
    }

    // Top N faixas de todo o histórico. Recalculado a cada chamada (sem job agendado).
    public List<TopTrackDTO> topTracks(int limit) {
        return listenRepository.findTopTracksSince(null, PageRequest.of(0, limit));
    }

    // Top N faixas do mês corrente.
    public List<TopTrackDTO> topTracksMonthly(int limit) {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return listenRepository.findTopTracksSince(startOfMonth, PageRequest.of(0, limit));
    }
}
