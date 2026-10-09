package musictracker.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// Documentação principal da API, no estilo de referência da Steam Web API
// (developer.valvesoftware.com/wiki/Steam_Web_API): formatos, depois cada
// "interface" (aqui, cada entidade) com seus métodos, argumentos e o layout
// do resultado. Isso tudo aparece na página inicial do Swagger UI, antes de
// qualquer endpoint — é a primeira coisa que quem abre a API vê.
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

                                Qualquer erro (400, 404 ou 500) é devolvido sempre no mesmo formato — o schema
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

                                ---

                                ## Requisitos do trabalho atendidos por este projeto

                                | Requisito pedido | Como foi atendido |
                                |---|---|
                                | Spring Boot mais recente, Java 17+, Maven, H2, Spring Data JPA | Spring Boot 4.1.1, Java 21, Maven, H2 em memória, Spring Data JPA |
                                | Mínimo 5 entidades relacionadas | 6 entidades: `User`, `UserProfile`, `Artist`, `Track`, `Playlist`, `Listen` |
                                | Um relacionamento de cada tipo (1-1, 1-N, N-N) | `User`↔`UserProfile` (One-to-One) · `Artist`→`Track` e `User`/`Track`→`Listen` (One-to-Many / Many-to-One) · `Playlist`↔`Track` (Many-to-Many) |
                                | Bean Validation em cada entidade | `@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@Positive`, `@PastOrPresent`, `@Pattern` em todas as entidades/DTOs |
                                | Pelo menos um enum | `Genre` (gênero musical do `Artist`) |
                                | Mínimo 5 endpoints REST por entidade, CRUD completo | Ver as interfaces abaixo — cada entidade tem 6 endpoints (create, get-by-id, list, search, update, delete) |
                                | Listagens paginadas (`Pageable`) | Todas as listagens, com tamanho de página limitado a 2 |
                                | Consulta personalizada por entidade | `search` por username/nome/gênero, `by-user` para perfis e escutas, e os rankings de `/stats` |
                                | Códigos HTTP apropriados | `200`, `201`, `204`, `400`, `404` conforme o caso (ver cada endpoint abaixo) |
                                | Documentação via Springdoc OpenAPI/Swagger | Esta própria página, gerada a partir das anotações `@Operation`/`@ApiResponses`/`@Schema` |
                                | HATEOAS com EntityModel/PagedModel e links relevantes | Implementado em todos os controllers (ver seção HATEOAS acima) |

                                ---

                                ## Interfaces e métodos

                                Cada seção abaixo corresponde a uma entidade (uma "interface", no sentido da
                                documentação da Steam Web API): os endpoints disponíveis, os argumentos
                                esperados e o formato (layout) da resposta.

                                ### Usuários (`/users`)

                                Conta de usuário do app: `username`, `email` e `password` (a senha nunca é
                                devolvida nas respostas — `UserResponse` é um DTO separado de `UserRequest`).

                                **`POST /users`** — cria um novo usuário.

                                *Argumentos (corpo JSON):* `username` *(string, 3–50 caracteres, obrigatório)*, `email` *(string, formato de e-mail válido, obrigatório)*, `password` *(string, mínimo 6 caracteres, obrigatório)*.

                                *Resposta `201`:* objeto `User` (`id`, `username`, `email`) + `_links`.

                                *Erros:* `400` dados inválidos ou JSON mal formado.

                                **`GET /users/{id}`** — busca um usuário pelo ID.

                                *Argumentos:* `id` *(Long, na URL)*.

                                *Resposta `200`:* objeto `User`.

                                *Erros:* `400` ID não numérico · `404` não encontrado.

                                **`GET /users`** — lista usuários, paginado (ver seção Paginação).

                                *Resposta `200`:* `PagedModel` de `User`.

                                **`GET /users/search?username=`** — consulta personalizada: usuários cujo `username` contém o texto informado (case-insensitive), paginado.

                                *Argumentos:* `username` *(string, obrigatório, query)*.

                                *Erros:* `400` parâmetro `username` ausente.

                                **`PUT /users/{id}`** — atualiza username/email/senha.

                                *Argumentos:* `id` *(Long, URL)* + mesmo corpo do `POST`.

                                *Erros:* `400` dados inválidos · `404` usuário não existe.

                                **`DELETE /users/{id}`** — remove um usuário.

                                *Resposta `204`* (sem conteúdo).

                                *Erros:* `400` ID inválido · `404` não encontrado.

                                ### Perfis (`/profiles`)

                                Perfil estendido do usuário (bio, avatar) — relação **One-to-One** com `User`.

                                **`POST /profiles`** — cria o perfil de um usuário.

                                *Argumentos:* `userId` *(Long, obrigatório — precisa existir e ainda não ter perfil)*, `bio` *(string, opcional, até 300 caracteres)*, `avatarUrl` *(string, opcional, precisa começar com `http://` ou `https://`)*.

                                *Resposta `201`:* `UserProfile` com o `User` resumido embutido.

                                *Erros:* `400` bio/avatar inválidos · `404` `userId` não existe.

                                **`GET /profiles/{id}`** — busca um perfil pelo ID do próprio perfil.

                                **`GET /profiles/by-user/{userId}`** — consulta personalizada: busca o perfil pelo ID do usuário dono, não pelo ID do perfil.

                                *Erros:* `404` usuário não tem perfil.

                                **`GET /profiles`** — lista perfis, paginado.

                                **`PUT /profiles/{id}`** — atualiza bio/avatar/vínculo de usuário.

                                **`DELETE /profiles/{id}`** — remove um perfil.

                                *Resposta `204`.*

                                ### Artistas (`/artists`)

                                Artista/banda, com o enum `Genre`.

                                **`POST /artists`** — cria um artista.

                                *Argumentos:* `name` *(string, obrigatório)*, `genre` *(enum `Genre`, obrigatório — um dos valores: `POP`, `ROCK`, `HIP_HOP`, `ELECTRONIC`, `INDIE`, `MPB`, `JAZZ`, `OTHER`)*.

                                *Erros:* `400` nome em branco ou gênero fora da lista do enum.

                                **`GET /artists/{id}`** — busca um artista pelo ID.

                                **`GET /artists`** — lista artistas, paginado.

                                **`GET /artists/search?genre=`** — consulta personalizada: artistas de um gênero exato.

                                *Argumentos:* `genre` *(enum `Genre`, obrigatório, query)*.

                                *Erros:* `400` parâmetro ausente ou fora dos valores do enum.

                                **`PUT /artists/{id}`** — atualiza nome/gênero.

                                **`DELETE /artists/{id}`** — remove um artista.

                                *Resposta `204`.*

                                ### Faixas (`/tracks`)

                                Faixa/música — relação **Many-to-One** com `Artist`.

                                **`POST /tracks`** — cria uma faixa.

                                *Argumentos:* `name` *(string, obrigatório)*, `artistId` *(Long, obrigatório — precisa já existir)*.

                                *Resposta `201`:* `Track` com o `Artist` resumido embutido.

                                *Erros:* `400` nome em branco/artistId inválido · `404` artista não existe.

                                **`GET /tracks/{id}`** — busca uma faixa pelo ID.

                                **`GET /tracks`** — lista faixas, paginado.

                                **`GET /tracks/search?name=`** — consulta personalizada: faixas cujo nome contém o texto informado (case-insensitive), paginado.

                                **`PUT /tracks/{id}`** — atualiza nome/artista da faixa.

                                **`DELETE /tracks/{id}`** — remove uma faixa.

                                *Resposta `204`.*

                                ### Playlists (`/playlists`)

                                Playlist — relação **Many-to-Many** com `Track`.

                                **`POST /playlists`** — cria uma playlist vazia.

                                *Argumentos:* `name` *(string, obrigatório)*.

                                **`GET /playlists/{id}`** — busca uma playlist (com a lista de faixas).

                                **`GET /playlists`** — lista playlists, paginado.

                                **`GET /playlists/search?name=`** — consulta personalizada por nome.

                                **`PUT /playlists/{id}`** — renomeia a playlist.

                                **`POST /playlists/{id}/tracks/{trackId}`** — adiciona uma faixa já existente à playlist (operação idempotente).

                                *Erros:* `404` playlist ou faixa não encontrada.

                                **`DELETE /playlists/{id}/tracks/{trackId}`** — remove uma faixa da playlist.

                                **`DELETE /playlists/{id}`** — remove a playlist inteira.

                                *Resposta `204`.*

                                ### Escutas (`/listens`)

                                Registro de escuta — liga `User` e `Track` (duas relações **Many-to-One**); é a base dos rankings de `/stats`.

                                **`POST /listens`** — registra uma escuta.

                                *Argumentos:* `userId` *(Long, obrigatório, precisa existir)*, `trackId` *(Long, obrigatório, precisa existir)*, `listenedAt` *(data/hora ISO-8601, opcional — se omitido usa o momento atual; não pode ser uma data futura)*, `durationSeconds` *(inteiro, opcional, precisa ser positivo)*.

                                *Erros:* `400` dados inválidos (data futura, duração ≤ 0, IDs ausentes ou em formato errado) · `404` usuário ou faixa não existe.

                                **`GET /listens/{id}`** — busca uma escuta pelo ID.

                                **`GET /listens`** — lista o histórico completo de escutas, paginado.

                                **`GET /listens/by-user/{userId}`** — consulta personalizada: histórico de escutas de um usuário específico, paginado.

                                **`PUT /listens/{id}`** — corrige uma escuta já registrada.

                                **`DELETE /listens/{id}`** — remove um registro de escuta.

                                *Resposta `204`.*

                                ### Stats (`/stats`)

                                Não é CRUD de uma entidade própria — são rankings agregados em cima da tabela `Listen`, recalculados a cada chamada (não existe um valor "salvo").

                                **`GET /stats/top-tracks?limit=`** — top N faixas mais ouvidas no histórico completo, ordenadas por número de vezes ouvida (critério de desempate: tempo total ouvido).

                                *Argumentos:* `limit` *(inteiro, opcional, padrão `10`, entre 1 e 50)*.

                                *Resposta `200`:* lista de `TopTrackDTO` — `trackId`, `trackName`, `artistName`, `timesListened`, `totalSeconds`.

                                *Erros:* `400` `limit` fora do intervalo 1–50.

                                **`GET /stats/top-tracks/monthly?limit=`** — mesmo ranking, mas só considerando escutas registradas a partir do primeiro dia do mês corrente (00:00) — a versão "resumo do mês", inspirada na Cápsula Sonora do Spotify.

                                *Argumentos:* `limit` *(inteiro, opcional, padrão `3`, entre 1 e 50)*.
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
        // Swagger UI fica disponível em /swagger-ui/index.html quando a aplicação está rodando.
    }
}