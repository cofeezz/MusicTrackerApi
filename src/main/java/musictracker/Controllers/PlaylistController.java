package musictracker.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import musictracker.DTO.PlaylistRequest;
import musictracker.DTO.PlaylistResponse;
import musictracker.Service.PlaylistService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/playlists")
@Tag(name = "Playlists", description = "Playlists — relação Many-to-Many com Track")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final PagedResourcesAssembler<PlaylistResponse> pagedAssembler;

    public PlaylistController(PlaylistService playlistService, PagedResourcesAssembler<PlaylistResponse> pagedAssembler) {
        this.playlistService = playlistService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(summary = "Criar uma nova playlist (vazia)",
            description = "Cria a playlist apenas com um nome; faixas são adicionadas depois via "
                    + "POST /playlists/{id}/tracks/{trackId}.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Playlist criada"),
            @ApiResponse(responseCode = "400", description = "Nome em branco ou ausente")
    })
    public ResponseEntity<EntityModel<PlaylistResponse>> create(@Valid @RequestBody PlaylistRequest request) {
        PlaylistResponse response = playlistService.create(request);
        return ResponseEntity.created(URI.create("/playlists/" + response.id())).body(toModel(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar uma playlist por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Playlist encontrada"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Não existe playlist com esse ID")
    })
    public EntityModel<PlaylistResponse> getById(
            @Parameter(description = "ID numérico da playlist", example = "1") @PathVariable Long id) {
        return toModel(playlistService.findById(id));
    }

    @GetMapping
    @Operation(summary = "Listar playlists", description = "Lista paginada (no máximo 2 por página).")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public PagedModel<EntityModel<PlaylistResponse>> getAll(@ParameterObject Pageable pageable) {
        Page<PlaylistResponse> page = playlistService.findAll(pageable);
        return pagedAssembler.toModel(page, this::toModel);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar playlists pelo nome",
            description = "Consulta personalizada: nome contém o texto informado (case-insensitive), paginado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "O parâmetro 'name' não foi informado")
    })
    public PagedModel<EntityModel<PlaylistResponse>> search(
            @Parameter(description = "Parte do nome da playlist a buscar (case-insensitive)", example = "foco")
            @RequestParam String name,
            @ParameterObject Pageable pageable) {
        Page<PlaylistResponse> page = playlistService.findByName(name, pageable);
        return pagedAssembler.toModel(page, this::toModel);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Renomear uma playlist")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Playlist atualizada"),
            @ApiResponse(responseCode = "400", description = "Nome inválido ou ID em formato incorreto"),
            @ApiResponse(responseCode = "404", description = "Não existe playlist com esse ID")
    })
    public EntityModel<PlaylistResponse> update(
            @Parameter(description = "ID numérico da playlist", example = "1") @PathVariable Long id,
            @Valid @RequestBody PlaylistRequest request) {
        return toModel(playlistService.update(id, request));
    }

    @PostMapping("/{id}/tracks/{trackId}")
    @Operation(summary = "Adicionar uma faixa à playlist",
            description = "Associa uma faixa já existente à playlist (relação Many-to-Many). "
                    + "Se a faixa já estiver na playlist, a operação é idempotente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Faixa adicionada"),
            @ApiResponse(responseCode = "400", description = "ID da playlist ou da faixa em formato inválido"),
            @ApiResponse(responseCode = "404", description = "Playlist ou faixa não encontrada")
    })
    public EntityModel<PlaylistResponse> addTrack(
            @Parameter(description = "ID numérico da playlist", example = "1") @PathVariable Long id,
            @Parameter(description = "ID numérico da faixa a adicionar", example = "3") @PathVariable Long trackId) {
        return toModel(playlistService.addTrack(id, trackId));
    }

    @DeleteMapping("/{id}/tracks/{trackId}")
    @Operation(summary = "Remover uma faixa da playlist")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Faixa removida"),
            @ApiResponse(responseCode = "400", description = "ID da playlist ou da faixa em formato inválido"),
            @ApiResponse(responseCode = "404", description = "Playlist não encontrada")
    })
    public EntityModel<PlaylistResponse> removeTrack(
            @Parameter(description = "ID numérico da playlist", example = "1") @PathVariable Long id,
            @Parameter(description = "ID numérico da faixa a remover", example = "3") @PathVariable Long trackId) {
        return toModel(playlistService.removeTrack(id, trackId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover uma playlist")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Playlist removida"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Não existe playlist com esse ID")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID numérico da playlist", example = "1") @PathVariable Long id) {
        playlistService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<PlaylistResponse> toModel(PlaylistResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(PlaylistController.class).getById(response.id())).withSelfRel(),
                linkTo(methodOn(PlaylistController.class).update(response.id(), null)).withRel("update"),
                linkTo(methodOn(PlaylistController.class).delete(response.id())).withRel("delete"),
                linkTo(methodOn(PlaylistController.class).getAll(null)).withRel("playlists"));
    }
}
