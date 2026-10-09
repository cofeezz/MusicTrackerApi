package musictracker.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import musictracker.DTO.UserProfileRequest;
import musictracker.DTO.UserProfileResponse;
import musictracker.Service.UserProfileService;
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
@RequestMapping("/profiles")
@Tag(name = "Perfis", description = "Perfis de usuário (bio, avatar) — relação One-to-One com User")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final PagedResourcesAssembler<UserProfileResponse> pagedAssembler;

    public UserProfileController(UserProfileService userProfileService,
                                  PagedResourcesAssembler<UserProfileResponse> pagedAssembler) {
        this.userProfileService = userProfileService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(
            summary = "Criar o perfil de um usuário",
            description = "'userId' precisa apontar para um usuário existente que ainda não tenha "
                    + "perfil (relação One-to-One). 'bio' é opcional (até 300 caracteres). "
                    + "'avatarUrl' é opcional, mas se enviado precisa começar com http:// ou https://."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Perfil criado"),
            @ApiResponse(responseCode = "400", description = "Bio maior que 300 caracteres, avatarUrl "
                    + "em formato inválido, ou userId ausente"),
            @ApiResponse(responseCode = "404", description = "O userId informado não existe")
    })
    public ResponseEntity<EntityModel<UserProfileResponse>> create(@Valid @RequestBody UserProfileRequest request) {
        UserProfileResponse response = userProfileService.create(request);
        return ResponseEntity.created(URI.create("/profiles/" + response.id())).body(toModel(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar um perfil por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Não existe perfil com esse ID")
    })
    public EntityModel<UserProfileResponse> getById(
            @Parameter(description = "ID numérico do perfil", example = "1") @PathVariable Long id) {
        return toModel(userProfileService.findById(id));
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "Buscar o perfil de um usuário específico",
            description = "Consulta personalizada: localiza o perfil pelo ID do usuário dono, em vez "
                    + "do ID do próprio perfil.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "400", description = "O userId informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Esse usuário não existe ou ainda não tem perfil")
    })
    public EntityModel<UserProfileResponse> getByUserId(
            @Parameter(description = "ID numérico do usuário", example = "1") @PathVariable Long userId) {
        return toModel(userProfileService.findByUserId(userId));
    }

    @GetMapping
    @Operation(summary = "Listar perfis", description = "Lista paginada (no máximo 2 por página).")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public PagedModel<EntityModel<UserProfileResponse>> getAll(@ParameterObject Pageable pageable) {
        Page<UserProfileResponse> page = userProfileService.findAll(pageable);
        return pagedAssembler.toModel(page, this::toModel);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar um perfil")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou ID em formato incorreto"),
            @ApiResponse(responseCode = "404", description = "Perfil ou usuário não encontrado")
    })
    public EntityModel<UserProfileResponse> update(
            @Parameter(description = "ID numérico do perfil", example = "1") @PathVariable Long id,
            @Valid @RequestBody UserProfileRequest request) {
        return toModel(userProfileService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover um perfil")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Perfil removido"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Não existe perfil com esse ID")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID numérico do perfil", example = "1") @PathVariable Long id) {
        userProfileService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<UserProfileResponse> toModel(UserProfileResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(UserProfileController.class).getById(response.id())).withSelfRel(),
                linkTo(methodOn(UserProfileController.class).update(response.id(), null)).withRel("update"),
                linkTo(methodOn(UserProfileController.class).delete(response.id())).withRel("delete"),
                linkTo(methodOn(UserProfileController.class).getAll(null)).withRel("profiles"),
                linkTo(methodOn(UserController.class).getById(response.user().id())).withRel("user"));
    }
}
