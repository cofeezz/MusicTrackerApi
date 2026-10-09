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
git clone <url-do-repositorio>
cd MusicTrackerApi
./mvnw spring-boot:run
```

Ou pelo IntelliJ: abra o projeto como Maven, aguarde o indexar as dependências e rode `MusicTrackerApiApplication`.

A aplicação sobe em `http://localhost:8080`. O banco é H2 em memória (zera os dados a cada reinício).

**Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

