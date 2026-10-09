package musictracker.Service;

import musictracker.DTO.PlaylistRequest;
import musictracker.DTO.PlaylistResponse;
import musictracker.DTO.TrackResponse;
import musictracker.Entity.Playlist;
import musictracker.Entity.Track;
import musictracker.Exception.ResourceNotFoundException;
import musictracker.Repository.PlaylistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final TrackService trackService;

    public PlaylistService(PlaylistRepository playlistRepository, TrackService trackService) {
        this.playlistRepository = playlistRepository;
        this.trackService = trackService;
    }

    public PlaylistResponse create(PlaylistRequest request) {
        Playlist playlist = new Playlist(request.name());
        return toResponse(playlistRepository.save(playlist));
    }

    @Transactional(readOnly = true)
    public PlaylistResponse findById(Long id) {
        return toResponse(getEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<PlaylistResponse> findAll(Pageable pageable) {
        return playlistRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PlaylistResponse> findByName(String name, Pageable pageable) {
        return playlistRepository.findByNameContainingIgnoreCase(name, pageable).map(this::toResponse);
    }

    public PlaylistResponse update(Long id, PlaylistRequest request) {
        Playlist playlist = getEntityOrThrow(id);
        playlist.setName(request.name());
        return toResponse(playlistRepository.save(playlist));
    }

    public PlaylistResponse addTrack(Long id, Long trackId) {
        Playlist playlist = getEntityOrThrow(id);
        Track track = trackService.getEntityOrThrow(trackId);
        playlist.getTracks().add(track);
        return toResponse(playlistRepository.save(playlist));
    }

    public PlaylistResponse removeTrack(Long id, Long trackId) {
        Playlist playlist = getEntityOrThrow(id);
        playlist.getTracks().removeIf(t -> t.getId().equals(trackId));
        return toResponse(playlistRepository.save(playlist));
    }

    public void delete(Long id) {
        if (!playlistRepository.existsById(id)) {
            throw new ResourceNotFoundException("Playlist não encontrada com o ID: " + id);
        }
        playlistRepository.deleteById(id);
    }

    private Playlist getEntityOrThrow(Long id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist não encontrada com o ID: " + id));
    }

    private PlaylistResponse toResponse(Playlist playlist) {
        List<TrackResponse> tracks = playlist.getTracks().stream()
                .map(TrackService::toResponseStatic)
                .toList();
        return new PlaylistResponse(playlist.getId(), playlist.getName(), tracks);
    }
}
