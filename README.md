# MusicTracker API

API REST para um rastreador pessoal de músicas ouvidas — um mini "Last.fm": o usuário registra as faixas que escutou, monta playlists e consulta rankings de Top 10/Top 3 artistas e faixas mais ouvidos, no mês ou no geral.

Projeto acadêmico desenvolvido em Java com Spring Boot, cobrindo os requisitos de: múltiplas entidades relacionadas (1:1, 1:N e N:N), validação de dados (Bean Validation), paginação, consultas customizadas, HATEOAS e documentação via Swagger/OpenAPI.

>  A documentação completa e interativa de todas as rotas (parâmetros, exemplos, formatos de erro) fica disponível no **Swagger UI** depois que a aplicação é iniciada — veja [Como rodar](#como-rodar). Este README é um resumo a ela.

## Sumário

- [Modelo de dados](#modelo-de-dados)
- [Como rodar](#como-rodar)
- [Paginação](#paginação)
- [Formato de erro](#formato-de-erro)
- [Principais rotas](#principais-rotas)
- [Stack](#stack)

## Modelo de dados

| Entidade | Relacionamento |
|---|---|
| **User** (usuário) | 1:1 com `UserProfile`; 1:N com `Listen` |
| **UserProfile** (perfil) | 1:1 com `User` |
| **Artist** (artista) | tem um `Genre` (enum); 1:N com `Track` |
| **Track** (faixa) | N:1 com `Artist`; N:N com `Playlist`; 1:N com `Listen` |
| **Playlist** | N:N com `Track` |
| **Listen** (escuta) | N:1 com `User` e N:1 com `Track` — é o registro de "ouvi a faixa X em tal data" |

`Genre` é um enum (`POP`, `ROCK`, `HIP_HOP`, `ELECTRONIC`, `INDIE`, `MPB`, `JAZZ`, `OTHER`) usado no cadastro de artistas.

## Como rodar

**Pré-requisitos:** Java 21 e Maven (ou use o wrapper `./mvnw` incluso no projeto).

```bash
git clone https://github.com/cofeezz/MusicTrackerApi.git
cd MusicTrackerApi
./mvnw spring-boot:run
```

Ou pelo IntelliJ: abra o projeto como Maven, aguarde o indexar as dependências e rode `MusicTrackerApiApplication`.

A aplicação sobe em `http://localhost:8080`. O banco é H2 em memória (zera os dados a cada reinício).

**Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

## Paginação

Todas as listagens (`GET` de coleção) são paginadas. Por padrão e no máximo, **2 itens por página**, mesmo que o cliente peça um `size` maior na URL.

Parâmetros de query aceitos:

- `page` — número da página (começa em `0`)

- `size` — itens por página (limitado a 2)

- `sort` — campo e direção, ex: `sort=name,asc`

A resposta segue o formato `PagedModel` do Spring HATEOAS, com os itens em `_embedded`, metadados de página em `page` (`size`, `totalElements`, `totalPages`, `number`) e links de navegação (`self`, `next`, `prev`, `first`, `last`) em `_links`.

## Formato de erro

Todo erro (400, 404 ou 500) é retornado no mesmo formato:

```json
{
  "timestamp": "2026-10-08T21:39:44",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Usuário não encontrado com o ID: 1",
  "path": "/listens"
}
```

- **400 (BAD_REQUEST)** — entrada inválida: campo obrigatório faltando, tipo errado (texto onde se espera número, enum inexistente), JSON mal formado, data em formato inválido ou fora de regra (`@PastOrPresent`), ou parâmetro fora dos limites permitidos (ex: `limit` de stats).

- **404 (NOT_FOUND)** — o ID informado é válido (é um número), mas o recurso não existe no banco.

- **500 (INTERNAL_SERVER_ERROR)** — erro inesperado não tratado explicitamente.

## Principais rotas

Todos os endpoints de coleção (`GET` sem `/{id}`) são paginados conforme a seção acima. IDs são numéricos (`Long`); enviar um valor não numérico retorna 400.

### Usuários — `/users`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/users` | Cria um usuário |
| GET | `/users/{id}` | Busca usuário por ID |
| GET | `/users` | Lista usuários (paginado) |
| GET | `/users/search?name=` | Busca usuários por nome |
| PUT | `/users/{id}` | Atualiza usuário |
| DELETE | `/users/{id}` | Remove usuário |

### Perfis — `/profiles`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/profiles` | Cria o perfil de um usuário |
| GET | `/profiles/{id}` | Busca perfil por ID |
| GET | `/profiles/by-user/{userId}` | Busca o perfil de um usuário específico |
| GET | `/profiles` | Lista perfis (paginado) |
| PUT | `/profiles/{id}` | Atualiza perfil |
| DELETE | `/profiles/{id}` | Remove perfil |

### Artistas — `/artists`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/artists` | Cria um artista |
| GET | `/artists/{id}` | Busca artista por ID |
| GET | `/artists` | Lista artistas (paginado) |
| GET | `/artists/search?genre=` | Busca artistas por gênero |
| PUT | `/artists/{id}` | Atualiza artista |
| DELETE | `/artists/{id}` | Remove artista |

### Faixas — `/tracks`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/tracks` | Cria uma faixa (vinculada a um artista) |
| GET | `/tracks/{id}` | Busca faixa por ID |
| GET | `/tracks` | Lista faixas (paginado) |
| GET | `/tracks/search?title=` | Busca faixas por título |
| PUT | `/tracks/{id}` | Atualiza faixa |
| DELETE | `/tracks/{id}` | Remove faixa |

### Playlists — `/playlists`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/playlists` | Cria uma playlist |
| GET | `/playlists/{id}` | Busca playlist por ID |
| GET | `/playlists` | Lista playlists (paginado) |
| GET | `/playlists/search?name=` | Busca playlists por nome |
| PUT | `/playlists/{id}` | Atualiza playlist |
| POST | `/playlists/{id}/tracks/{trackId}` | Adiciona uma faixa à playlist |
| DELETE | `/playlists/{id}/tracks/{trackId}` | Remove uma faixa da playlist |
| DELETE | `/playlists/{id}` | Remove playlist |

### Escutas — `/listens`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/listens` | Registra que um usuário ouviu uma faixa |
| GET | `/listens/{id}` | Busca escuta por ID |
| GET | `/listens` | Lista escutas (paginado) |
| GET | `/listens/by-user/{userId}` | Lista escutas de um usuário |
| PUT | `/listens/{id}` | Atualiza uma escuta |
| DELETE | `/listens/{id}` | Remove uma escuta |

### Estatísticas — `/stats`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/stats/top-tracks?limit=` | Ranking geral das faixas mais ouvidas (`limit` entre 1 e 50, padrão Top 10) |
| GET | `/stats/top-tracks/monthly?limit=` | Ranking das faixas mais ouvidas no mês atual (`limit` entre 1 e 50) |

## Stack

- Java 21 + Spring Boot 4.1.1
- Spring Data JPA + H2 (banco em memória)
- Bean Validation (Jakarta Validation)
- Spring HATEOAS
- springdoc-openapi (Swagger UI)
- Maven
