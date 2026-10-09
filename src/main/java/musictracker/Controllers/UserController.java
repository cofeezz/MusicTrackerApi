package musictracker.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import musictracker.DTO.UserRequest;
import musictracker.DTO.UserResponse;
import musictracker.Service.UserService;
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
@RequestMapping("/users")
@Tag(name = "Usuários", description = "Contas de usuário (username, email, senha)")
public class UserController {

    private final UserService userService;
    private final PagedResourcesAssembler<UserResponse> pagedAssembler;

    public UserController(UserService userService, PagedResourcesAssembler<UserResponse> pagedAssembler) {
        this.userService = userService;
        this.pagedAssembler = pagedAssembler;
    }

    @PostMapping
    @Operation(
            summary = "Criar um novo usuário",
            description = "Cria uma conta de usuário. 'username' precisa ter entre 3 e 50 caracteres, "
                    + "'email' precisa ter um formato de e-mail válido, e 'password' precisa ter pelo "
                    + "menos 6 caracteres. A senha nunca é devolvida nas respostas da API."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos: campo obrigatório "
                    + "em branco, email em formato errado, senha curta demais, JSON mal formado "
                    + "ou com tipos incorretos")
    })
    public ResponseEntity<EntityModel<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.create(request);
        EntityModel<UserResponse> model = toModel(response);
        return ResponseEntity.created(URI.create("/users/" + response.id())).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar um usuário por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido "
                    + "(ex: /users/abc)"),
            @ApiResponse(responseCode = "404", description = "Não existe usuário com esse ID")
    })
    public EntityModel<UserResponse> getById(
            @Parameter(description = "ID numérico do usuário", example = "1") @PathVariable Long id) {
        return toModel(userService.findById(id));
    }

    @GetMapping
    @Operation(
            summary = "Listar usuários",
            description = "Lista paginada de usuários. Por configuração do projeto, cada página "
                    + "devolve no máximo 2 itens, mesmo que 'size' seja maior na URL."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public PagedModel<EntityModel<UserResponse>> getAll(@ParameterObject Pageable pageable) {
        Page<UserResponse> page = userService.findAll(pageable);
        return pagedAssembler.toModel(page, this::toModel);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Buscar usuários por nome de usuário",
            description = "Consulta personalizada: retorna usuários cujo 'username' contém o texto "
                    + "informado (case-insensitive), paginado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "O parâmetro 'username' não foi informado")
    })
    public PagedModel<EntityModel<UserResponse>> search(
            @Parameter(description = "Parte do nome de usuário a buscar (case-insensitive)", example = "cof")
            @RequestParam String username,
            @ParameterObject Pageable pageable) {
        Page<UserResponse> page = userService.findByUsername(username, pageable);
        return pagedAssembler.toModel(page, this::toModel);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar um usuário", description = "Substitui username, email e senha do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou ID em formato incorreto"),
            @ApiResponse(responseCode = "404", description = "Não existe usuário com esse ID")
    })
    public EntityModel<UserResponse> update(
            @Parameter(description = "ID numérico do usuário", example = "1") @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        return toModel(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover um usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário removido com sucesso (sem conteúdo na resposta)"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "404", description = "Não existe usuário com esse ID")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID numérico do usuário", example = "1") @PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<UserResponse> toModel(UserResponse response) {
        return EntityModel.of(response,
                linkTo(methodOn(UserController.class).getById(response.id())).withSelfRel(),
                linkTo(methodOn(UserController.class).update(response.id(), null)).withRel("update"),
                linkTo(methodOn(UserController.class).delete(response.id())).withRel("delete"),
                linkTo(methodOn(UserController.class).getAll(null)).withRel("users"));
    }
}
