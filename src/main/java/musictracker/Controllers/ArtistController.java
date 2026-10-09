package musictracker.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import musictracker.DTO.ArtistRequest;
import musictracker.DTO.ArtistResponse;
import musictracker.Entity.Genre;
import musictracker.Service.ArtistService;
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
@RequestMapping("/artists")
@Tag(name = "Artistas", description = "Artistas/bandas, com o enum Genre")
public class ArtistController {

    private final ArtistService artistService;
    private final PagedResourcesAssembler<ArtistResponse> pagedAssembler;

    public ArtistController(ArtistService artistService, PagedResourcesAssembler<ArtistResponse> pagedAssembler) {
        this.artistService = artistService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(
            summary = "Criar um novo artista",
            description = "'name' é obrigatório. 'genre' precisa ser exatamente um dos valores do enum "
                    + "Genre (POP, ROCK, HIP_HOP, ELECTRONIC, INDIE, MPB, JAZZ, OTHER) — qualquer outro "
                    + "texto, número ou valor ausente é rejeitado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Artista criado"),
            @ApiResponse(responseCode = "400", description = "Nome em branco, gênero ausente ou gênero "
                    + "fora dos valores aceitos pelo enum Genre")
    })
    public ResponseEntity<EntityModel<ArtistResponse>> create(@Valid @RequestBody ArtistRequest request) {
        ArtistResponse response = artistService.create(request);
        return ResponseEntity.created(URI.create("/artists/" + response.id())).body(toModel(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar um artista por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artista encontrado"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Não existe artista com esse ID")
    })
    public EntityModel<ArtistResponse> getById(
            @Parameter(description = "ID numérico do artista", example = "1") @PathVariable Long id) {
        return toModel(artistService.findById(id));
    }

    @GetMapping
    @Operation(
            summary = "Listar artistas",
            description = "Lista paginada de artistas (no máximo 2 por página)."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public PagedModel<EntityModel<ArtistResponse>> getAll(@ParameterObject Pageable pageable) {
        Page<ArtistResponse> page = artistService.findAll(pageable);
        return pagedAssembler.toModel(page, this::toModel);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Buscar artistas por gênero",
            description = "Consulta personalizada: retorna artistas que pertencem exatamente ao gênero "
                    + "informado. O valor precisa bater com um dos nomes do enum Genre."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "O parâmetro 'genre' não foi informado "
                    + "ou não corresponde a nenhum valor do enum Genre")
    })
    public PagedModel<EntityModel<ArtistResponse>> search(
            @Parameter(description = "Gênero musical (POP, ROCK, HIP_HOP, ELECTRONIC, INDIE, MPB, JAZZ, OTHER)",
                    example = "ROCK")
            @RequestParam Genre genre,
            @ParameterObject Pageable pageable) {
        Page<ArtistResponse> page = artistService.findByGenre(genre, pageable);
        return pagedAssembler.toModel(page, this::toModel);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar um artista")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artista atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou ID em formato incorreto"),
            @ApiResponse(responseCode = "404", description = "Não existe artista com esse ID")
    })
    public EntityModel<ArtistResponse> update(
            @Parameter(description = "ID numérico do artista", example = "1") @PathVariable Long id,
            @Valid @RequestBody ArtistRequest request) {
        return toModel(artistService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover um artista")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Artista removido"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Não existe artista com esse ID")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID numérico do artista", example = "1") @PathVariable Long id) {
        artistService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<ArtistResponse> toModel(ArtistResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(ArtistController.class).getById(response.id())).withSelfRel(),
                linkTo(methodOn(ArtistController.class).update(response.id(), null)).withRel("update"),
                linkTo(methodOn(ArtistController.class).delete(response.id())).withRel("delete"),
                linkTo(methodOn(ArtistController.class).getAll(null)).withRel("artists"));
    }
}
