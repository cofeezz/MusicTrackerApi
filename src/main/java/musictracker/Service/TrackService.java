package musictracker.Service;

import musictracker.DTO.ArtistResponse;
import musictracker.DTO.TrackRequest;
import musictracker.DTO.TrackResponse;
import musictracker.Entity.Artist;
import musictracker.Entity.Track;
import musictracker.Exception.ResourceNotFoundException;
import musictracker.Repository.TrackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TrackService {

    private final TrackRepository trackRepository;
    private final ArtistService artistService;

    public TrackService(TrackRepository trackRepository, ArtistService artistService) {
        this.trackRepository = trackRepository;
        this.artistService = artistService;
    }

    public TrackResponse create(TrackRequest request) {
        Artist artist = artistService.getEntityOrThrow(request.artistId());
        Track track = new Track(request.name(), artist);
        return toResponse(trackRepository.save(track));
    }

    @Transactional(readOnly = true)
    public TrackResponse findById(Long id) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faixa não encontrada com o ID: " + id));
        return toResponse(track);
    }

    @Transactional(readOnly = true)
    public Page<TrackResponse> findAll(Pageable pageable) {
        return trackRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TrackResponse> findByName(String name, Pageable pageable) {
        return trackRepository.findByNameContainingIgnoreCase(name, pageable).map(this::toResponse);
    }

    public TrackResponse update(Long id, TrackRequest request) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faixa não encontrada com o ID: " + id));

        track.setName(request.name());
        track.setArtist(artistService.getEntityOrThrow(request.artistId()));

        return toResponse(trackRepository.save(track));
    }

    public void delete(Long id) {
        if (!trackRepository.existsById(id)) {
            throw new ResourceNotFoundException("Faixa não encontrada com o ID: " + id);
        }
        trackRepository.deleteById(id);
    }

    // Só pra outros Services (PlaylistService, ListenService) resolverem um ID de faixa.
    @Transactional(readOnly = true)
    public Track getEntityOrThrow(Long id) {
        return trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faixa não encontrada com o ID: " + id));
    }

    public static TrackResponse toResponseStatic(Track track) {
        ArtistResponse artistResponse = ArtistService.toResponseStatic(track.getArtist());
        return new TrackResponse(track.getId(), track.getName(), artistResponse);
    }

    private TrackResponse toResponse(Track track) {
        return toResponseStatic(track);
    }
}
