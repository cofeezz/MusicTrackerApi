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
@Tag(name = "Usuários", description = "Contas de usuário (username, email, senha). **Privado:** o e-mail só é visível ao próprio dono; não existe lista pública de usuários. Ao criar a conta, você concorda que suas escutas poderão ser usadas de forma agregada e anônima para comparar rankings (veja a seção Privacidade).")
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
                    + "menos 6 caracteres. A senha nunca é devolvida nas respostas da API. "
                    + "**Aviso de privacidade:** o e-mail, a senha e as escutas são privados (só você os vê), mas, "
                    + "ao criar a conta, você concorda que as suas escutas poderão ser usadas, de forma agregada e "
                    + "anônima, para comparar o seu ranking com o da comunidade (por exemplo, a porcentagem de "
                    + "ouvintes de cada faixa). Nenhum nome, e-mail ou escuta individual é mostrado a outros usuários."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos: campo obrigatório "
                    + "em branco, email em formato errado, senha curta ou longa demais, JSON mal formado "
                    + "ou com tipos incorretos"),
            @ApiResponse(responseCode = "409", description = "Já existe usuário com esse nome de usuário ou e-mail")
    })
    public ResponseEntity<EntityModel<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.create(request);
        EntityModel<UserResponse> model = toModel(response);
        return ResponseEntity.created(URI.create("/users/" + response.id())).body(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar a própria conta por ID",
            description = "**Privado:** o e-mail de uma conta só pode ser visto pelo próprio dono. "
                    + "Pedir o ID de outro usuário resulta em 403.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta encontrada"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido "
                    + "(ex: /users/abc)"),
            @ApiResponse(responseCode = "403", description = "A conta pertence a outro usuário"),
            @ApiResponse(responseCode = "404", description = "Não existe usuário com esse ID")
    })
    public EntityModel<UserResponse> getById(
            @Parameter(description = "ID numérico do usuário", example = "1") @PathVariable Long id) {
        return toModel(userService.findById(id));
    }

    @GetMapping
    @Operation(
            summary = "Listar a própria conta",
            description = "**Privado:** não há lista pública de usuários. Esta rota devolve apenas a conta do "
                    + "dono da chave de API (os e-mails de outras pessoas nunca aparecem), no formato paginado "
                    + "padrão das listagens."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public PagedModel<EntityModel<UserResponse>> getAll(@ParameterObject Pageable pageable) {
        Page<UserResponse> page = userService.findAll(pageable);
        return pagedAssembler.toModel(page, this::toModel);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Buscar a própria conta por nome de usuário",
            description = "**Privado:** a busca considera apenas a conta do dono da chave de API. Devolve a "
                    + "conta se o seu 'username' contiver o texto informado (case-insensitive); caso contrário, "
                    + "a lista vem vazia. Não é possível procurar outras pessoas."
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
            @ApiResponse(responseCode = "403", description = "A conta pertence a outro usuário"),
            @ApiResponse(responseCode = "404", description = "Não existe usuário com esse ID")
    })
    public EntityModel<UserResponse> update(
            @Parameter(description = "ID numérico do usuário", example = "1") @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        return toModel(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover a própria conta",
            description = "Apaga a sua conta e, junto com ela, as suas chaves de API (a chave usada nesta "
                    + "chamada deixa de funcionar), o seu perfil, as suas escutas e as playlists que você criou. "
                    + "Artistas e faixas que você cadastrou continuam no catálogo. Não dá para desfazer.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário removido com sucesso (sem conteúdo na resposta)"),
            @ApiResponse(responseCode = "400", description = "O ID informado não é um número válido"),
            @ApiResponse(responseCode = "403", description = "A conta pertence a outro usuário"),
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
