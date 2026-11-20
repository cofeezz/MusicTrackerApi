# Music Tracker

Um "diário" das músicas que você ouve. Você registra o que escutou e a API mostra quais são as suas faixas favoritas, no mês ou em todo o tempo, parecido com o Last.fm ou com o resumo anual do Spotify, só que seu.

Este repositório é a **API** (o servidor que guarda e calcula tudo). O aplicativo de celular que usa essa API fica em um repositório separado.

**Experimente agora:** [Swagger no ar](https://musictrackerapi-hgj1.onrender.com/swagger-ui/index.html)

> O servidor gratuito dorme depois de 15 minutos sem uso e pode levar cerca de 1 minuto para acordar. Os dados ficam em memória: quando o servidor reinicia, tudo é apagado e é preciso cadastrar o usuário de novo.

## O que dá para fazer

- Criar uma conta e um perfil
- Cadastrar artistas e faixas
- Registrar que você ouviu uma música (quando e por quantos segundos)
- Ver o seu ranking de faixas mais ouvidas, de sempre ou do mês
- Comparar o seu ranking com a comunidade, só com porcentagens anônimas
- Montar playlists e ver as de outras pessoas

## Como funciona, em 4 passos

1. **Cadastre-se.** Você cria um usuário com nome, e-mail e senha.
2. **Pegue a sua chave de acesso.** Com usuário e senha, a API entrega uma chave que começa com `mt_`. Ela funciona como uma senha para o app: você a mostra a cada pedido. Ela aparece **uma única vez**, então guarde.
3. **Registre o que ouviu.** Cada escuta é um registro: quem ouviu, qual faixa, quando e por quanto tempo.
4. **Veja o ranking.** A API conta as escutas e devolve as faixas mais ouvidas. Se quiser, compare com a comunidade: a API mostra só porcentagens, nunca nomes.

### Quem pode ver e mexer em quê

- **Catálogo (artistas, faixas):** qualquer pessoa vê, mesmo sem chave. Só quem cadastrou altera ou apaga.
- **Playlists:** todo mundo vê (com o nome de usuário do dono), mas só quem criou pode editar ou apagar.
- **Seus dados (e-mail, perfil, escutas, ranking, chaves):** só você vê e altera. Outra pessoa recebe `403`.
- **Todo o resto:** exige a chave de acesso.

### Privacidade e uso das suas escutas

- **Seu e-mail é privado.** Não existe lista pública de usuários: `GET /users` e a busca devolvem só a sua própria conta.
- **Suas escutas e seu ranking são privados.** Só você os vê.
- **Aviso importante:** ao criar a conta, você concorda que as suas escutas poderão ser usadas, de forma **agregada e anônima**, para comparar o seu ranking com o da comunidade, como o Spotify faz ao dizer "você está entre os 5% que mais ouvem esta música". A comparação mostra apenas **porcentagens e contagens**, nunca nomes, e-mails, IDs ou escutas individuais de outras pessoas.
- Com poucos usuários, uma porcentagem é mais fácil de associar a alguém (com 2 usuários, 50% já indica que a outra pessoa ouviu a faixa).
- **Apagar a conta** (`DELETE /users/{id}`) remove também as suas chaves, o seu perfil, as suas escutas e as suas playlists. Os artistas e as faixas que você cadastrou continuam no catálogo.

## Teste em 2 minutos, sem instalar nada

Abra o [Swagger](https://musictrackerapi-hgj1.onrender.com/swagger-ui/index.html). É uma página onde você testa a API clicando.

1. Em `POST /users`, clique em **Try it out**, preencha nome, e-mail e senha e execute. Anote o `id` que voltar.
2. Em `POST /api-keys`, informe o mesmo usuário e a senha. Copie o valor de `key` da resposta (o `userId` também vem aí).
3. Clique em **Authorize** no topo, cole a chave e confirme.
4. Cadastre um artista (`POST /artists`) e uma faixa (`POST /tracks`).
5. Registre uma escuta em `POST /listens`, usando o seu `userId` e o `id` da faixa.
6. Veja o resultado em `GET /stats/top-tracks`, informando o seu `userId`.
7. Compare com a comunidade em `GET /stats/community/comparison` (suas faixas) e `GET /stats/community/top-tracks` (top global). Só aparecem porcentagens.

Em todo `POST` o campo `X-Idempotency-Key` é **obrigatório**: digite qualquer texto único (por exemplo `teste-001`) e troque o valor a cada novo pedido. Repetir o mesmo valor no mesmo pedido devolve a resposta anterior sem criar nada novo (explicado abaixo).

## Rodar na sua máquina

Escolha um dos caminhos.

**Com Docker** (não precisa instalar Java):

```bash
git clone https://github.com/cofeezz/MusicTrackerApi.git
cd MusicTrackerApi
docker build -t music-tracker-api .
docker run --rm -p 8080:8080 music-tracker-api
```

**Com Java 21 e Maven:**

```bash
git clone https://github.com/cofeezz/MusicTrackerApi.git
cd MusicTrackerApi
mvn spring-boot:run
```

Depois abra `http://localhost:8080/swagger-ui/index.html`. O banco é em memória: cada vez que a API reinicia, os dados voltam do zero.

## Colocar no ar (Render)

A API roda no [Render](https://render.com) a partir do `Dockerfile` deste repositório.

1. Crie um **Web Service** no Render ligado a este repositório, na branch `develop`, com o ambiente **Docker** e o plano **Free**.
2. Em **Health Check Path**, informe `/v3/api-docs`.
3. Em **Environment**, cadastre `RATELIMIT_TRUST_FORWARDED_FOR` com o valor `true`. Isso faz o limite de requisições enxergar o IP real de cada pessoa atrás do proxy do Render.
4. Se houver um site (front web) usando a API, cadastre também `CORS_ALLOWED_ORIGINS` com o endereço dele.

Cada alteração enviada para a `develop` gera um novo deploy automaticamente.

## Referência técnica

A documentação completa e interativa de cada rota (parâmetros, exemplos e erros) fica no **Swagger**. O resumo abaixo acompanha ela.

### Modelo de dados

| Entidade | Relacionamento |
|---|---|
| **User** (usuário) | 1:1 com `UserProfile`; 1:N com `Listen`; 1:N com `Playlist` (dono) |
| **UserProfile** (perfil) | 1:1 com `User` |
| **Artist** (artista) | tem um `Genre` (enum); 1:N com `Track` |
| **Track** (faixa) | N:1 com `Artist`; N:N com `Playlist`; 1:N com `Listen` |
| **Playlist** | N:1 com `User` (dono); N:N com `Track` |
| **Listen** (escuta) | N:1 com `User` e N:1 com `Track`: o registro de "ouvi a faixa X em tal data" |

`Genre` é um enum: `POP`, `ROCK`, `HIP_HOP`, `ELECTRONIC`, `INDIE`, `MPB`, `JAZZ`, `OTHER`.

### Rotas

Listagens são paginadas. IDs são numéricos; enviar um valor não numérico devolve 400.

<details>
<summary><strong>Chaves de API</strong> — <code>/api-keys</code></summary>

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api-keys` | Gera uma chave (usuário + senha); rota aberta. A resposta traz também o seu `userId` |
| GET | `/api-keys` | Lista as suas chaves |
| GET | `/api-keys/{id}` | Busca uma das suas chaves |
| DELETE | `/api-keys/{id}` | Revoga uma chave |

</details>

<details>
<summary><strong>Usuários</strong> — <code>/users</code></summary>

| Método | Rota | Descrição |
|---|---|---|
| POST | `/users` | Cria um usuário (rota aberta) |
| GET | `/users/{id}` | Busca a sua conta (de outra pessoa: `403`) |
| GET | `/users` | Devolve só a sua conta (não há lista pública) |
| GET | `/users/search?username=` | Procura entre a sua conta apenas |
| PUT | `/users/{id}` | Atualiza a sua conta |
| DELETE | `/users/{id}` | Apaga a sua conta, chaves, perfil, escutas e playlists |

</details>

<details>
<summary><strong>Perfis</strong> — <code>/profiles</code></summary>

| Método | Rota | Descrição |
|---|---|---|
| POST | `/profiles` | Cria o seu perfil |
| GET | `/profiles/{id}` | Busca o seu perfil (de outra pessoa: `403`) |
| GET | `/profiles/by-user/{userId}` | Busca o seu perfil pelo seu `userId` |
| GET | `/profiles` | Devolve só o seu perfil |
| PUT | `/profiles/{id}` | Atualiza o seu perfil |
| DELETE | `/profiles/{id}` | Remove o seu perfil |

</details>

<details>
<summary><strong>Artistas</strong> — <code>/artists</code></summary>

| Método | Rota | Descrição |
|---|---|---|
| POST | `/artists` | Cria um artista |
| GET | `/artists/{id}` | Busca artista por ID |
| GET | `/artists` | Lista artistas |
| GET | `/artists/search?name=&genre=` | Busca artistas por nome e/ou gênero (filtros opcionais) |
| PUT | `/artists/{id}` | Atualiza artista (só quem cadastrou) |
| DELETE | `/artists/{id}` | Remove artista (só quem cadastrou, e se não tiver faixas) |

</details>

<details>
<summary><strong>Faixas</strong> — <code>/tracks</code></summary>

| Método | Rota | Descrição |
|---|---|---|
| POST | `/tracks` | Cria uma faixa (o artista precisa já existir: `artistId`) |
| GET | `/tracks/{id}` | Busca faixa por ID |
| GET | `/tracks` | Lista faixas |
| GET | `/tracks/search?name=` | Busca faixas por nome |
| PUT | `/tracks/{id}` | Atualiza faixa (só quem cadastrou) |
| DELETE | `/tracks/{id}` | Remove faixa (só quem cadastrou, e se não tiver escutas nem estiver em playlists) |

</details>

<details>
<summary><strong>Playlists</strong> — <code>/playlists</code></summary>

| Método | Rota | Descrição |
|---|---|---|
| POST | `/playlists` | Cria uma playlist (você vira o dono) |
| GET | `/playlists/{id}` | Busca playlist por ID |
| GET | `/playlists` | Lista playlists de todos |
| GET | `/playlists/search?name=` | Busca playlists por nome |
| PUT | `/playlists/{id}` | Atualiza playlist (só o dono) |
| POST | `/playlists/{id}/tracks/{trackId}` | Adiciona uma faixa (só o dono) |
| DELETE | `/playlists/{id}/tracks/{trackId}` | Remove uma faixa (só o dono) |
| DELETE | `/playlists/{id}` | Remove playlist (só o dono) |

</details>

<details>
<summary><strong>Escutas</strong> — <code>/listens</code></summary>

| Método | Rota | Descrição |
|---|---|---|
| POST | `/listens` | Registra que você ouviu uma faixa |
| GET | `/listens/{id}` | Busca uma das suas escutas |
| GET | `/listens` | Lista as suas escutas |
| GET | `/listens/by-user/{userId}` | Lista as escutas de um usuário (só as suas) |
| PUT | `/listens/{id}` | Atualiza uma das suas escutas |
| DELETE | `/listens/{id}` | Remove uma das suas escutas |

</details>

<details>
<summary><strong>Estatísticas</strong> — <code>/stats</code></summary>

| Método | Rota | Descrição |
|---|---|---|
| GET | `/stats/top-tracks?userId=&limit=` | Suas faixas mais ouvidas de sempre (privado; `limit` de 1 a 50, padrão 10) |
| GET | `/stats/top-tracks/monthly?userId=&limit=` | Suas faixas mais ouvidas do mês atual (privado; `limit` de 1 a 50, padrão 3) |
| GET | `/stats/community/comparison?userId=&limit=` | Compara as suas faixas com a comunidade: % de ouvintes da faixa e em que top % você está |
| GET | `/stats/community/top-tracks?limit=` | Top global: faixas com mais ouvintes e a % de ouvintes da comunidade |

Em caso de empate, vale quem tem mais tempo total ouvido. As rotas da comunidade devolvem só números agregados, sem nome, e-mail ou ID de ninguém.

</details>

### Autenticação (chave de API)

Rotas protegidas exigem o cabeçalho `X-API-Key`. A chave é gerada em `POST /api-keys`, aparece uma vez, é guardada apenas como hash e pode ser revogada em `DELETE /api-keys/{id}`.

- **Abertas:** `POST /users`, `POST /api-keys` e os `GET` de `/artists`, `/tracks` e `/playlists`.
- **Protegidas:** todo o resto.

### Versionamento

Alguns endpoints têm mais de uma versão, escolhida pelo cabeçalho `X-API-Version` (`1` ou `2`). Sem o cabeçalho vale a versão `1`; um valor desconhecido devolve `400`. A resposta traz o cabeçalho `X-API-Version` com a versão usada.

| Endpoint | Versão 1 | Versão 2 |
|---|---|---|
| `GET /stats/top-tracks` e `/monthly` | `trackId`, `trackName`, `artistName`, `timesListened`, `totalSeconds` | os mesmos campos, mais `rank` e `totalMinutes` |

### Limite de requisições (rate limit)

Cada cliente tem um "balde de fichas": cada pedido gasta uma ficha e o balde recebe fichas novas continuamente. Isso permite uma rajada curta, mas limita a média. Por padrão, **20 fichas, recarregando 1 a cada 5 segundos**:

```properties
ratelimit.enabled=true
ratelimit.capacity=20
ratelimit.refill-per-second=0.2
```

Toda resposta traz `X-RateLimit-Limit` e `X-RateLimit-Remaining`. Quando o balde esvazia, a API responde **429 Too Many Requests** com o cabeçalho `Retry-After` (em quantos segundos tentar de novo).

### Proteção contra pedidos repetidos (idempotência)

Todo `POST` exige o cabeçalho `X-Idempotency-Key`, uma chave única escolhida pelo cliente (por exemplo, um UUID). Ela evita que o mesmo pedido seja processado duas vezes por acidente, como num clique duplo ou numa nova tentativa depois de uma falha de rede.

| Situação | Resultado |
|---|---|
| Cabeçalho ausente ou vazio | `400 Bad Request` |
| Chave nova | Processa normalmente e guarda a resposta de sucesso |
| Mesma chave e mesmo pedido | Devolve a mesma resposta, sem criar nada novo, com `Idempotent-Replayed: true` |
| Mesma chave, rota ou corpo diferentes | `422 Unprocessable Entity` |
| Mesma chave enquanto o primeiro pedido ainda roda | `409 Conflict` |

As chaves ficam em memória por 24 horas (`idempotency.ttl-hours`).

### Paginação

Todas as listagens são paginadas, com no máximo **10 itens por página**. Parâmetros: `page` (começa em `0`), `size` e `sort` (por exemplo, `sort=name,asc`). A resposta traz os itens, os dados da página (`size`, `totalElements`, `totalPages`, `number`) e links para navegar (`self`, `next`, `prev`, `first`, `last`).

### Erros

Todo erro vem no mesmo formato:

```json
{
  "timestamp": "2026-10-08T21:39:44",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Usuário não encontrado com o ID: 1",
  "path": "/listens"
}
```

| Código | Quando acontece |
|---|---|
| 400 | Entrada inválida: `X-Idempotency-Key` ausente em um `POST`, campo faltando, tipo errado, JSON mal formado, data inválida, parâmetro fora do limite ou versão desconhecida |
| 401 | Chave ausente, inválida ou revogada; usuário ou senha errados |
| 403 | A chave é válida, mas o recurso é de outro usuário |
| 404 | O ID é válido, mas não existe, ou a rota não existe |
| 405 | Método não permitido na rota (por exemplo, `PATCH`) |
| 406 | `Accept` pede um formato que não seja JSON |
| 409 | Conflito: nome de usuário ou e-mail repetido, perfil já existente, faixa repetida no artista, artista ou faixa que ainda tem dependentes |
| 415 | `Content-Type` diferente de `application/json` |
| 422 | `X-Idempotency-Key` reaproveitada em outro pedido |
| 429 | Limite de requisições excedido |
| 500 | Erro inesperado |

### CORS

Navegadores só chamam a API a partir das origens configuradas em `cors.allowed-origins` (por padrão, o GitHub Pages do projeto e endereços locais de desenvolvimento). Para outro endereço, defina a variável `CORS_ALLOWED_ORIGINS`. O aplicativo de celular não é afetado.

### Tecnologias

- Java 21 e Spring Boot 4.1.1
- Spring Data JPA e banco H2 (em memória)
- Bean Validation
- Spring HATEOAS
- springdoc-openapi (Swagger UI)
- Maven e Docker
