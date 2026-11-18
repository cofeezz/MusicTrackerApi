package musictracker.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import musictracker.Security.ProtectedRoutes;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApi {

    @Bean
    public OpenAPI musicTrackerOpenApi(
            @Value("${ratelimit.capacity:20}") long capacity,
            @Value("${ratelimit.refill-per-second:0.2}") double refillPerSecond) {
        return new OpenAPI()
                .info(new Info()
                        .title("Music Tracker API")
                        .version("v1")
                        .description("""
                                API de rastreamento pessoal de músicas/artistas ouvidos, com ranking pessoal
                                tipo "cápsula sonora" (mais ouvidas de sempre e do mês). Projeto acadêmico —
                                Tecnologia em Desenvolvimento de Sistemas para a Internet.

                                ---

                                ## Formatos

                                Esta API só retorna e aceita um formato de dados: **JSON**. Toda requisição
                                com corpo (`POST`/`PUT`) deve enviar o cabeçalho `Content-Type: application/json`
                                e um JSON válido conforme o schema do endpoint.

                                ## Autenticação

                                As rotas protegidas exigem uma **chave de API** no cabeçalho `X-API-Key`.
                                A chave é gerada em `POST /api-keys` com username e senha, aparece completa
                                uma única vez e pode ser revogada em `DELETE /api-keys/{id}`. No Swagger UI,
                                use o botão **Authorize** para informá-la.

                                - **Abertas:** `POST /users`, `POST /api-keys` e as consultas (`GET`) de artistas, faixas e playlists

                                - **Protegidas:** todo o resto — criar, alterar e apagar dados, e ler usuários, perfis, escutas, rankings e chaves

                                - **Playlists:** qualquer um vê, mas só o dono (quem a criou) pode alterá-la; outro usuário recebe `403 Forbidden`

                                - **Artistas e faixas:** qualquer um vê, mas só quem cadastrou pode alterar ou remover (`403`); um artista com faixas, ou uma faixa com escutas ou em playlists, não pode ser removido (`409`); o mesmo artista não pode ter duas faixas com o mesmo nome (`409`)

                                - **Dados pessoais (privados):** conta (inclusive o e-mail), perfil, escutas e rankings só podem ser vistos e alterados pelo próprio dono da chave; outro usuário recebe `403 Forbidden` (detalhes na seção *Privacidade e uso dos seus dados*)

                                Chave ausente, inválida ou revogada resulta em `401 Unauthorized`.

                                ## Privacidade e uso dos seus dados

                                **Só você vê (privado):**

                                - o seu **e-mail** e a sua senha (a senha nunca é devolvida e fica guardada só como hash)

                                - as suas **escutas** e o seu **ranking pessoal**: `/listens`, `/stats/top-tracks` e `/stats/top-tracks/monthly`

                                - o seu **perfil** (`/profiles`) e as suas **chaves de API** (`/api-keys`)

                                Quem tentar acessar esses dados de outra pessoa recebe `403 Forbidden`. **Não existe lista pública de usuários:** `GET /users` e `GET /users/search` devolvem apenas a sua própria conta, então o e-mail de ninguém é exposto.

                                **Aviso importante: comparação com a comunidade.** Ao criar a conta e usar a API, você concorda que as suas escutas poderão ser usadas, de forma **agregada e anônima**, para comparar o seu ranking com o da comunidade, como o Spotify faz ao mostrar "você está entre os 5% que mais ouvem esta música". Isso só aparece como **porcentagens e contagens**, em `GET /stats/community/comparison` e `GET /stats/community/top-tracks`. **Nunca** é mostrado nome, e-mail, ID ou escuta individual de outra pessoa. Observação: com poucos usuários cadastrados, uma porcentagem é mais fácil de associar a alguém (por exemplo, com 2 usuários, 50% já indica que a outra pessoa ouviu a faixa).

                                **Público (qualquer um vê):** o catálogo de artistas e faixas (nome e gênero) e as playlists, que mostram o **nome de usuário** do dono (nunca o e-mail).

                                **Apagar a conta:** `DELETE /users/{id}` apaga a conta, as chaves de API, o perfil, as escutas e as playlists do usuário. Os artistas e as faixas que ele cadastrou continuam no catálogo.


                                ## Rate limit

                                Cada cliente (identificado pelo IP) tem um "balde de fichas" (algoritmo *token bucket*):
                                cada requisição gasta 1 ficha.

                                - **Limite:** o balde comporta **{{CAPACITY}} fichas**, ou seja, até {{CAPACITY}} requisições em rajada

                                - **Reposição:** o balde recebe **1 ficha a cada {{SECONDS}} segundos**; depois da rajada, o ritmo sustentado é de 1 requisição a cada {{SECONDS}} segundos

                                - **Quando volta:** com o balde vazio, a API responde **`429 Too Many Requests`** (formato `APIError`) com o cabeçalho `Retry-After`, que diz em quantos segundos a próxima requisição será aceita

                                Toda resposta traz os cabeçalhos:

                                - `X-RateLimit-Limit` — tamanho do balde ({{CAPACITY}})

                                - `X-RateLimit-Remaining` — fichas que ainda restam

                                A documentação (Swagger UI) não conta no limite.

                                ## Idempotência

                                Todo `POST` aceita o cabeçalho opcional `X-Idempotency-Key`, com uma chave
                                única escolhida pelo cliente (ex: um UUID, até 100 caracteres). Ele evita que a
                                mesma operação seja processada duas vezes por acidente, como um clique duplo
                                ou uma nova tentativa automática do app após uma falha de rede.

                                Como a API se comporta quando o cabeçalho é enviado:

                                - **Chave nova** — a requisição é processada normalmente e a resposta de sucesso (2xx) fica guardada

                                - **Mesma chave e mesma requisição** — nada é criado de novo: a API devolve a mesma resposta da primeira vez, com o cabeçalho `Idempotent-Replayed: true`

                                - **Mesma chave, mas rota ou corpo diferentes** — `422 Unprocessable Entity`: a chave já foi usada para outra operação

                                - **Mesma chave enquanto a primeira ainda está sendo processada** — `409 Conflict`

                                As chaves ficam guardadas por 24 horas e valem por cliente (IP). Respostas de erro
                                não ficam guardadas: se a primeira tentativa falhou, o cliente pode corrigir e
                                repetir com a mesma chave. Sem o cabeçalho, o `POST` funciona como sempre.

                                ## Versionamento

                                Alguns endpoints têm mais de uma versão, escolhida pelo cabeçalho `X-API-Version`
                                (`1` ou `2`). Sem o cabeçalho, vale a versão `1`. Um valor desconhecido resulta em
                                `400 Bad Request`. A resposta devolve o cabeçalho `X-API-Version` com a versão usada.

                                - `GET /stats/top-tracks` e `GET /stats/top-tracks/monthly`: a versão `1` devolve
                                  `timesListened` e `totalSeconds`; a versão `2` acrescenta `rank` e `totalMinutes`

                                ## CORS

                                A API aceita chamadas de navegador apenas das origens configuradas em
                                `cors.allowed-origins`. Métodos permitidos: `GET`, `POST`, `PUT`, `DELETE` e `OPTIONS`.
                                Cabeçalhos aceitos: `Content-Type`, `Accept`, `X-API-Key`, `X-Idempotency-Key` e
                                `X-API-Version`. Os cabeçalhos `Location`, `Retry-After`, `X-RateLimit-*`,
                                `Idempotent-Replayed` e `WWW-Authenticate` ficam legíveis para o cliente.
                                Apps nativos (celular) não são afetados por CORS.

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

                                Qualquer erro (400, 401, 403, 404, 405, 406, 409, 415, 422, 429 ou 500) é devolvido sempre no mesmo formato — o schema
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

                                1. `POST /users` — criar um usuário

                                2. `POST /api-keys` — gerar a chave e informá-la em **Authorize**

                                3. `POST /artists` — cadastrar um artista

                                4. `POST /tracks` (com o `artistId`) — cadastrar uma faixa

                                5. `POST /profiles` (com o `userId` do passo 1) — criar o perfil

                                6. `POST /listens` (com `userId` e `trackId`; o `userId` vem na resposta do passo 2) — registrar uma escuta

                                7. `POST /playlists`, depois `POST /playlists/{id}/tracks/{trackId}` — montar uma playlist

                                8. `GET /stats/top-tracks?userId=` e `GET /stats/top-tracks/monthly?userId=` — ver os seus rankings (privados)

                                9. `GET /stats/community/comparison?userId=` e `GET /stats/community/top-tracks` — comparar com a comunidade (porcentagens anônimas)
                                """
                                .replace("{{CAPACITY}}", String.valueOf(capacity))
                                .replace("{{SECONDS}}", String.valueOf(Math.round(1 / refillPerSecond)))))
                .components(new Components().addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-API-Key")
                        .description("Chave gerada em POST /api-keys")))
                .tags(List.of(
                        new Tag().name("Chaves de API").description("Geração e gerenciamento das chaves usadas no cabeçalho X-API-Key. Só o dono vê as próprias chaves."),
                        new Tag().name("Usuários").description("Contas de usuário (username, email, senha). **Privado:** o e-mail só é visível ao próprio dono; não existe lista pública de usuários. Ao criar a conta, você concorda que suas escutas poderão ser usadas de forma agregada e anônima para comparar rankings (veja a seção Privacidade)."),
                        new Tag().name("Perfis").description("Perfis de usuário (bio, avatar) — relação One-to-One com User. **Privados:** só o dono vê e altera o próprio perfil."),
                        new Tag().name("Artistas").description("Artistas/bandas, com o enum Genre. Catálogo compartilhado: todos veem, só quem cadastrou altera ou remove."),
                        new Tag().name("Faixas").description("Faixas/músicas — relação Many-to-One com Artist. Catálogo compartilhado: todos veem, só quem cadastrou altera ou remove."),
                        new Tag().name("Playlists").description("Playlists — relação Many-to-Many com Track. Todos veem (com o nome de usuário do dono); só o dono altera."),
                        new Tag().name("Escutas").description("Registros de escuta **privados** — ligam User e Track. Só o dono vê e altera. Poderão ser usados, de forma agregada e anônima, na comparação com a comunidade."),
                        new Tag().name("Stats").description("Rankings pessoais e **privados** de faixas mais ouvidas (histórico completo e mês corrente). Só o dono vê o próprio ranking."),
                        new Tag().name("Comunidade").description("Estatísticas **agregadas e anônimas** (porcentagens e contagens, sem nomes, e-mails ou IDs de usuários) para comparar o seu ranking com o da comunidade, como no Spotify.")
                ));
    }

    @Bean
    public OpenApiCustomizer rateLimitResponseCustomizer(
            @Value("${ratelimit.capacity:20}") long capacity,
            @Value("${ratelimit.refill-per-second:0.2}") double refillPerSecond) {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation ->
                            operation.getResponses().addApiResponse("429", new ApiResponse()
                                    .description("Limite de requisições excedido (rate limit): o balde de " + capacity
                                            + " fichas esvaziou e recebe 1 ficha a cada " + Math.round(1 / refillPerSecond) + " segundos. "
                                            + "Aguarde o tempo informado em Retry-After antes de tentar de novo.")
                                    .addHeaderObject("X-RateLimit-Limit", new Header()
                                            .description("Tamanho do balde (requisições em rajada)")
                                            .schema(new IntegerSchema()))
                                    .addHeaderObject("X-RateLimit-Remaining", new Header()
                                            .description("Fichas restantes (0 quando bloqueado)")
                                            .schema(new IntegerSchema()))
                                    .addHeaderObject("Retry-After", new Header()
                                            .description("Segundos até a próxima requisição ser aceita")
                                            .schema(new IntegerSchema())))));
        };
    }

    @Bean
    public OpenApiCustomizer idempotencyCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem -> {
                if (pathItem.getPost() == null) {
                    return;
                }
                pathItem.getPost().addParametersItem(new HeaderParameter()
                        .name("X-Idempotency-Key")
                        .required(false)
                        .description("Chave única da operação (ex: um UUID, até 100 caracteres). Repetir o mesmo "
                                + "POST com a mesma chave devolve a resposta original sem criar nada de novo.")
                        .schema(new StringSchema()));
                pathItem.getPost().getResponses().addApiResponse("409", new ApiResponse()
                        .description("Já existe uma requisição em processamento com esta X-Idempotency-Key"));
                pathItem.getPost().getResponses().addApiResponse("422", new ApiResponse()
                        .description("A X-Idempotency-Key já foi usada em outra requisição (rota ou corpo diferentes)"));
            });
        };
    }

    @Bean
    public OpenApiCustomizer apiKeySecurityCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) ->
                    pathItem.readOperationsMap().forEach((method, operation) -> {
                        if (ProtectedRoutes.requiresKey(method.name(), path)) {
                            operation.addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"));
                            operation.getResponses().addApiResponse("401", new ApiResponse()
                                    .description("Chave de API ausente, inválida ou revogada"));
                        }
                    }));
        };
    }
}
