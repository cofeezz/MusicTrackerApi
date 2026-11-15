package musictracker.Service;

import musictracker.DTO.ArtistResponse;
import musictracker.DTO.TrackRequest;
import musictracker.DTO.TrackResponse;
import musictracker.Entity.Artist;
import musictracker.Entity.Track;
import musictracker.Exception.ConflictException;
import musictracker.Exception.ResourceNotFoundException;
import musictracker.Repository.ListenRepository;
import musictracker.Repository.PlaylistRepository;
import musictracker.Repository.TrackRepository;
import musictracker.Security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TrackService {

    private final TrackRepository trackRepository;
    private final ArtistService artistService;
    private final ListenRepository listenRepository;
    private final PlaylistRepository playlistRepository;

    public TrackService(TrackRepository trackRepository, ArtistService artistService,
                        ListenRepository listenRepository, PlaylistRepository playlistRepository) {
        this.trackRepository = trackRepository;
        this.artistService = artistService;
        this.listenRepository = listenRepository;
        this.playlistRepository = playlistRepository;
    }

    public TrackResponse create(TrackRequest request) {
        Artist artist = artistService.getEntityOrThrow(request.artistId());
        if (trackRepository.existsByNameIgnoreCaseAndArtistId(request.name(), artist.getId())) {
            throw new ConflictException("Este artista já possui uma faixa com esse nome.");
        }
        Track track = new Track(request.name(), artist);
        track.setCreatedById(AuthenticatedUser.requireId());
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

        AuthenticatedUser.requireSame(track.getCreatedById(), "Você só pode alterar faixas que você cadastrou.");

        Artist artist = artistService.getEntityOrThrow(request.artistId());
        boolean changed = !track.getName().equalsIgnoreCase(request.name())
                || !track.getArtist().getId().equals(artist.getId());
        if (changed && trackRepository.existsByNameIgnoreCaseAndArtistId(request.name(), artist.getId())) {
            throw new ConflictException("Este artista já possui uma faixa com esse nome.");
        }

        track.setName(request.name());
        track.setArtist(artist);

        return toResponse(trackRepository.save(track));
    }

    public void delete(Long id) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faixa não encontrada com o ID: " + id));
        AuthenticatedUser.requireSame(track.getCreatedById(), "Você só pode remover faixas que você cadastrou.");
        if (listenRepository.existsByTrackId(id) || playlistRepository.existsByTracksId(id)) {
            throw new ConflictException("Esta faixa possui escutas ou está em playlists e não pode ser removida.");
        }
        trackRepository.delete(track);
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
