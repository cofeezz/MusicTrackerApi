package musictracker.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import musictracker.DTO.TrackRequest;
import musictracker.DTO.TrackResponse;
import musictracker.Service.TrackService;
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
@RequestMapping("/tracks")
@Tag(name = "Faixas", description = "Faixas/músicas — relação Many-to-One com Artist")
public class TrackController {

    private final TrackService trackService;
    private final PagedResourcesAssembler<TrackResponse> pagedAssembler;

    public TrackController(TrackService trackService, PagedResourcesAssembler<TrackResponse> pagedAssembler) {
        this.trackService = trackService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(
            summary = "Criar uma nova faixa",
            description = "'name' é obrigatório. 'artistId' precisa ser o ID de um artista que já "
                    + "existe no banco — se não existir, a resposta é 404, não 400 (o formato do "
                    + "dado está correto, só o recurso referenciado não existe)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Faixa criada"),
            @ApiResponse(responseCode = "400", description = "Nome em branco, artistId ausente ou "
                    + "em formato inválido (ex: texto em vez de número)"),
            @ApiResponse(responseCode = "404", description = "O artistId informado não corresponde "
                    + "a nenhum artista existente")
    })
    public ResponseEntity<EntityModel<TrackResponse>> create(@Valid @RequestBody TrackRequest request) {
        TrackResponse response = trackService.create(request);
        return ResponseEntity.created(URI.create("/tracks/" + response.id())).body(toModel(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar uma faixa por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Faixa encontrada"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Não existe faixa com esse ID")
    })
    public EntityModel<TrackResponse> getById(
            @Parameter(description = "ID numérico da faixa", example = "1") @PathVariable Long id) {
        return toModel(trackService.findById(id));
    }

    @GetMapping
    @Operation(
            summary = "Listar faixas",
            description = "Lista paginada de faixas (no máximo 2 por página), cada uma com os dados "
                    + "resumidos do artista correspondente."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public PagedModel<EntityModel<TrackResponse>> getAll(@ParameterObject Pageable pageable) {
        Page<TrackResponse> page = trackService.findAll(pageable);
        return pagedAssembler.toModel(page, this::toModel);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Buscar faixas pelo nome",
            description = "Consulta personalizada: retorna faixas cujo nome contém o texto informado "
                    + "(case-insensitive), paginado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "O parâmetro 'name' não foi informado")
    })
    public PagedModel<EntityModel<TrackResponse>> search(
            @Parameter(description = "Parte do nome da faixa a buscar (case-insensitive)", example = "1901")
            @RequestParam String name,
            @ParameterObject Pageable pageable) {
        Page<TrackResponse> page = trackService.findByName(name, pageable);
        return pagedAssembler.toModel(page, this::toModel);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar uma faixa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Faixa atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou ID em formato incorreto"),
            @ApiResponse(responseCode = "404", description = "Faixa ou artista (novo artistId) não encontrado")
    })
    public EntityModel<TrackResponse> update(
            @Parameter(description = "ID numérico da faixa", example = "1") @PathVariable Long id,
            @Valid @RequestBody TrackRequest request) {
        return toModel(trackService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover uma faixa")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Faixa removida"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Não existe faixa com esse ID")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID numérico da faixa", example = "1") @PathVariable Long id) {
        trackService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<TrackResponse> toModel(TrackResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(TrackController.class).getById(response.id())).withSelfRel(),
                linkTo(methodOn(TrackController.class).update(response.id(), null)).withRel("update"),
                linkTo(methodOn(TrackController.class).delete(response.id())).withRel("delete"),
                linkTo(methodOn(TrackController.class).getAll(null)).withRel("tracks"),
                linkTo(methodOn(ArtistController.class).getById(response.artist().id())).withRel("artist"));
    }
}
