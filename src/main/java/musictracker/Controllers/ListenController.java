package musictracker.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import musictracker.DTO.ListenRequest;
import musictracker.DTO.ListenResponse;
import musictracker.Service.ListenService;
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
@RequestMapping("/listens")
@Tag(name = "Escutas", description = "Registros de escuta — ligam User e Track (duas relações Many-to-One)")
public class ListenController {

    private final ListenService listenService;
    private final PagedResourcesAssembler<ListenResponse> pagedAssembler;

    public ListenController(ListenService listenService, PagedResourcesAssembler<ListenResponse> pagedAssembler) {
        this.listenService = listenService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(
            summary = "Registrar uma nova escuta",
            description = "'userId' e 'trackId' precisam apontar para registros existentes. "
                    + "'listenedAt' é opcional (se omitido, usa o momento atual) e não pode ser uma "
                    + "data futura. 'durationSeconds' é opcional e, se enviado, precisa ser positivo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Escuta registrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos: userId/trackId ausentes, "
                    + "data futura em 'listenedAt', duração zero/negativa, ou JSON mal formado "
                    + "(ex: texto em campo numérico, data em formato incorreto)"),
            @ApiResponse(responseCode = "404", description = "userId ou trackId informado não existe")
    })
    public ResponseEntity<EntityModel<ListenResponse>> create(@Valid @RequestBody ListenRequest request) {
        ListenResponse response = listenService.create(request);
        return ResponseEntity.created(URI.create("/listens/" + response.id())).body(toModel(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar uma escuta por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Escuta encontrada"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Não existe escuta com esse ID")
    })
    public EntityModel<ListenResponse> getById(
            @Parameter(description = "ID numérico do registro de escuta", example = "1") @PathVariable Long id) {
        return toModel(listenService.findById(id));
    }

    @GetMapping
    @Operation(summary = "Listar o histórico de escutas",
            description = "Lista paginada de todos os registros de escuta (no máximo 2 por página).")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public PagedModel<EntityModel<ListenResponse>> getAll(@ParameterObject Pageable pageable) {
        Page<ListenResponse> page = listenService.findAll(pageable);
        return pagedAssembler.toModel(page, this::toModel);
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "Listar as escutas de um usuário específico",
            description = "Consulta personalizada: histórico de escutas filtrado por usuário, paginado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "O userId informado não é um número válido")
    })
    public PagedModel<EntityModel<ListenResponse>> getByUser(
            @Parameter(description = "ID numérico do usuário", example = "1") @PathVariable Long userId,
            @ParameterObject Pageable pageable) {
        Page<ListenResponse> page = listenService.findByUser(userId, pageable);
        return pagedAssembler.toModel(page, this::toModel);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Corrigir uma escuta já registrada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Escuta atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou ID em formato incorreto"),
            @ApiResponse(responseCode = "404", description = "Escuta, usuário ou faixa não encontrados")
    })
    public EntityModel<ListenResponse> update(
            @Parameter(description = "ID numérico do registro de escuta", example = "1") @PathVariable Long id,
            @Valid @RequestBody ListenRequest request) {
        return toModel(listenService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover um registro de escuta")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Escuta removida"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Não existe escuta com esse ID")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID numérico do registro de escuta", example = "1") @PathVariable Long id) {
        listenService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<ListenResponse> toModel(ListenResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(ListenController.class).getById(response.id())).withSelfRel(),
                linkTo(methodOn(ListenController.class).delete(response.id())).withRel("delete"),
                linkTo(methodOn(ListenController.class).getAll(null)).withRel("listens"),
                linkTo(methodOn(TrackController.class).getById(response.track().id())).withRel("track"));
    }
}
