package musictracker.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApi {

    @Bean
    public OpenAPI musicTrackerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Music Tracker API")
                        .version("v1")
                        .description("""
                                API de rastreamento pessoal de músicas/artistas ouvidos, com ranking pessoal
                                tipo "cápsula sonora" (top 10 geral + top 3 do mês). Projeto acadêmico —
                                Tecnologia em Desenvolvimento de Sistemas para a Internet.

                                ---

                                ## Formatos

                                Esta API só retorna e aceita um formato de dados: **JSON**. Toda requisição
                                com corpo (`POST`/`PUT`) deve enviar o cabeçalho `Content-Type: application/json`
                                e um JSON válido conforme o schema do endpoint.

                                ## Autenticação

                                Esta API **não exige autenticação** (é um projeto acadêmico de escopo didático,
                                sem login/sessão). Qualquer cliente pode chamar qualquer endpoint diretamente.

                                ## Rate limit

                                Cada IP tem um "balde de fichas" (algoritmo *token bucket*): cada requisição
                                gasta 1 ficha e o balde recebe fichas novas continuamente. Isso permite uma
                                rajada curta, mas limita a média de requisições por segundo.

                                Toda resposta traz os cabeçalhos:

                                - `X-RateLimit-Limit` — tamanho do balde (rajada máxima)

                                - `X-RateLimit-Remaining` — fichas que ainda restam

                                Quando o balde esvazia, a API responde **`429 Too Many Requests`** (no mesmo
                                formato `APIError`) com o cabeçalho `Retry-After`, que informa em quantos
                                segundos o cliente pode tentar de novo. A documentação (Swagger UI) não conta no limite.

                                ## Paginação

                                Todos os endpoints de listagem (`GET` sem `/{id}`) são paginados via `Pageable`
                                do Spring Data e aceitam os parâmetros de query:

                                - `page` — número da página, começando em `0` (padrão: `0`)

                                - `size` — itens por página (padrão e **máximo: 10**, mesmo se um valor maior for enviado)

                                - `sort` — campo e direção de ordenação, ex: `sort=name,asc`

                                A resposta de uma listagem é sempre um `PagedModel`, contendo:

                                - `_embedded` — objeto com a lista de itens da página atual

                                - `page` — objeto com `size`, `totalElements`, `totalPages` e `number` (página atual)

                                - `_links` — links de navegação entre páginas (`self`, `next`, `prev`, `first`, `last`)

                                ## HATEOAS

                                Toda resposta de um único recurso (`EntityModel`) traz um bloco `_links` com as
                                URLs que o cliente pode seguir a partir dali — por exemplo, a resposta de uma
                                faixa (`Track`) traz `self`, `update`, `delete`, `tracks` (voltar à coleção) e
                                `artist` (navegar até o artista dono da faixa), sem o cliente precisar montar
                                essas URLs manualmente.

                                ## Formato de erro

                                Qualquer erro (400, 404, 429 ou 500) é devolvido sempre no mesmo formato — o schema
                                **`APIError`**:

                                - `timestamp` — momento do erro

                                - `status` — código HTTP (ex: `400`)

                                - `error` — nome do status (ex: `BAD_REQUEST`)

                                - `message` — explicação legível do que deu errado

                                - `path` — rota que foi chamada

                                Isso vale tanto para erros de validação (campo obrigatório vazio, enum
                                inválido, data futura, duração negativa) quanto para erros de formato
                                (JSON mal formado, tipo errado no corpo ou na URL, parâmetro obrigatório
                                faltando) e para recursos não encontrados (404).

                                ## Como começar (ordem recomendada de chamadas)

                                1. `POST /artists` — cadastrar um artista

                                2. `POST /tracks` (com o `artistId` do passo 1) — cadastrar uma faixa

                                3. `POST /users` — criar um usuário

                                4. `POST /profiles` (com o `userId` do passo 3) — criar o perfil do usuário

                                5. `POST /listens` (com `userId` e `trackId`) — registrar uma escuta

                                6. `POST /playlists`, depois `POST /playlists/{id}/tracks/{trackId}` — montar uma playlist

                                7. `GET /stats/top-tracks` e `GET /stats/top-tracks/monthly` — ver os rankings
                                """))
                .tags(List.of(
                        new Tag().name("Usuários"),
                        new Tag().name("Perfis"),
                        new Tag().name("Artistas"),
                        new Tag().name("Faixas"),
                        new Tag().name("Playlists"),
                        new Tag().name("Escutas"),
                        new Tag().name("Stats")
                ));
    }
    @Bean
    public OpenApiCustomizer rateLimitResponseCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation ->
                            operation.getResponses().addApiResponse("429", new ApiResponse()
                                    .description("Limite de requisições excedido (rate limit). "
                                            + "Aguarde o tempo informado em Retry-After antes de tentar de novo.")
                                    .addHeaderObject("Retry-After", new Header()
                                            .description("Segundos até a próxima requisição ser aceita")
                                            .schema(new IntegerSchema())))));
        };
    }
}