package musictracker.Service;

import musictracker.DTO.ArtistRequest;
import musictracker.DTO.ArtistResponse;
import musictracker.Entity.Artist;
import musictracker.Entity.Genre;
import musictracker.Exception.ResourceNotFoundException;
import musictracker.Repository.ArtistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ArtistService {

    private final ArtistRepository artistRepository;

    public ArtistService(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    public ArtistResponse create(ArtistRequest request) {
        Artist artist = new Artist(request.name(), request.genre());
        return toResponse(artistRepository.save(artist));
    }

    @Transactional(readOnly = true)
    public ArtistResponse findById(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artista não encontrado com o ID: " + id));
        return toResponse(artist);
    }

    @Transactional(readOnly = true)
    public Page<ArtistResponse> findAll(Pageable pageable) {
        return artistRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ArtistResponse> findByGenre(Genre genre, Pageable pageable) {
        return artistRepository.findByGenre(genre, pageable).map(this::toResponse);
    }

    public ArtistResponse update(Long id, ArtistRequest request) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artista não encontrado com o ID: " + id));

        artist.setName(request.name());
        artist.setGenre(request.genre());

        return toResponse(artistRepository.save(artist));
    }

    public void delete(Long id) {
        if (!artistRepository.existsById(id)) {
            throw new ResourceNotFoundException("Artista não encontrado com o ID: " + id);
        }
        artistRepository.deleteById(id);
    }

    // Só pra outros Services (TrackService) resolverem um ID de artista.
    @Transactional(readOnly = true)
    public Artist getEntityOrThrow(Long id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artista não encontrado com o ID: " + id));
    }

    public static ArtistResponse toResponseStatic(Artist artist) {
        return new ArtistResponse(artist.getId(), artist.getName(), artist.getGenre());
    }

    private ArtistResponse toResponse(Artist artist) {
        return toResponseStatic(artist);
    }
}
