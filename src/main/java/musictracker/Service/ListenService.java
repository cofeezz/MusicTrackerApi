package musictracker.Service;

import musictracker.DTO.ListenRequest;
import musictracker.DTO.ListenResponse;
import musictracker.DTO.UserResponse;
import musictracker.Entity.Listen;
import musictracker.Entity.Track;
import musictracker.Entity.User;
import musictracker.Exception.ResourceNotFoundException;
import musictracker.Repository.ListenRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class ListenService {

    private final ListenRepository listenRepository;
    private final UserService userService;
    private final TrackService trackService;

    public ListenService(ListenRepository listenRepository, UserService userService, TrackService trackService) {
        this.listenRepository = listenRepository;
        this.userService = userService;
        this.trackService = trackService;
    }

    // Espera um JSON do tipo { "userId": 1, "trackId": 1, "durationSeconds": 180 }
    // "listenedAt" é opcional: se não vier, usamos o momento atual (é o
    // check-in rápido "acabei de ouvir essa música").
    public ListenResponse create(ListenRequest request) {
        User user = userService.getEntityOrThrow(request.userId());
        Track track = trackService.getEntityOrThrow(request.trackId());
        LocalDateTime listenedAt = request.listenedAt() != null ? request.listenedAt() : LocalDateTime.now();

        Listen listen = new Listen(user, track, listenedAt, request.durationSeconds());
        return toResponse(listenRepository.save(listen));
    }

    @Transactional(readOnly = true)
    public ListenResponse findById(Long id) {
        Listen listen = listenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escuta não encontrada com o ID: " + id));
        return toResponse(listen);
    }

    @Transactional(readOnly = true)
    public Page<ListenResponse> findAll(Pageable pageable) {
        return listenRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ListenResponse> findByUser(Long userId, Pageable pageable) {
        return listenRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    public ListenResponse update(Long id, ListenRequest request) {
        Listen listen = listenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escuta não encontrada com o ID: " + id));

        listen.setUser(userService.getEntityOrThrow(request.userId()));
        listen.setTrack(trackService.getEntityOrThrow(request.trackId()));
        listen.setListenedAt(request.listenedAt() != null ? request.listenedAt() : listen.getListenedAt());
        listen.setDurationSeconds(request.durationSeconds());

        return toResponse(listenRepository.save(listen));
    }

    public void delete(Long id) {
        if (!listenRepository.existsById(id)) {
            throw new ResourceNotFoundException("Escuta não encontrada com o ID: " + id);
        }
        listenRepository.deleteById(id);
    }

    private ListenResponse toResponse(Listen listen) {
        UserResponse userResponse = new UserResponse(
                listen.getUser().getId(), listen.getUser().getUsername(), listen.getUser().getEmail());
        return new ListenResponse(
                listen.getId(),
                userResponse,
                TrackService.toResponseStatic(listen.getTrack()),
                listen.getListenedAt(),
                listen.getDurationSeconds());
    }
}
