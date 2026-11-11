package musictracker.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import musictracker.DTO.ApiKeyCreatedResponse;
import musictracker.DTO.ApiKeyRequest;
import musictracker.DTO.ApiKeyResponse;
import musictracker.Service.ApiKeyService;
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
@RequestMapping("/api-keys")
@Tag(name = "Chaves de API", description = "Geração e gerenciamento das chaves usadas no cabeçalho X-API-Key")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final PagedResourcesAssembler<ApiKeyResponse> pagedAssembler;

    public ApiKeyController(ApiKeyService apiKeyService, PagedResourcesAssembler<ApiKeyResponse> pagedAssembler) {
        this.apiKeyService = apiKeyService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(
            summary = "Gerar uma chave de API",
            description = "Rota aberta: autentica com username e senha e devolve uma chave nova. "
                    + "O valor completo aparece apenas nesta resposta; guarde-o."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Chave criada"),
            @ApiResponse(responseCode = "400", description = "Campos obrigatórios ausentes ou inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuário ou senha inválidos")
    })
    public ResponseEntity<EntityModel<ApiKeyCreatedResponse>> create(@Valid @RequestBody ApiKeyRequest request) {
        ApiKeyCreatedResponse response = apiKeyService.create(request);
        EntityModel<ApiKeyCreatedResponse> model = EntityModel.of(response,
                linkTo(methodOn(ApiKeyController.class).getById(response.id())).withSelfRel(),
                linkTo(methodOn(ApiKeyController.class).revoke(response.id())).withRel("revoke"),
                linkTo(methodOn(ApiKeyController.class).getAll(null)).withRel("api-keys"));
        return ResponseEntity.created(URI.create("/api-keys/" + response.id())).body(model);
    }

    @GetMapping
    @Operation(summary = "Listar minhas chaves", description = "Lista paginada das chaves do dono da chave usada na requisição.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public PagedModel<EntityModel<ApiKeyResponse>> getAll(@ParameterObject Pageable pageable) {
        Page<ApiKeyResponse> page = apiKeyService.findMine(pageable);
        return pagedAssembler.toModel(page, this::toModel);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar uma das minhas chaves por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chave encontrada"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Chave inexistente ou de outro usuário")
    })
    public EntityModel<ApiKeyResponse> getById(
            @Parameter(description = "ID numérico da chave", example = "1") @PathVariable Long id) {
        return toModel(apiKeyService.findMineById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revogar uma chave", description = "A chave deixa de funcionar imediatamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Chave revogada"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Chave inexistente ou de outro usuário")
    })
    public ResponseEntity<Void> revoke(
            @Parameter(description = "ID numérico da chave", example = "1") @PathVariable Long id) {
        apiKeyService.revoke(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<ApiKeyResponse> toModel(ApiKeyResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(ApiKeyController.class).getById(response.id())).withSelfRel(),
                linkTo(methodOn(ApiKeyController.class).revoke(response.id())).withRel("revoke"),
                linkTo(methodOn(ApiKeyController.class).getAll(null)).withRel("api-keys"));
    }
}
