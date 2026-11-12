package musictracker.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import musictracker.DTO.TopTrackDTO;
import musictracker.DTO.TopTrackV2Response;
import musictracker.Service.StatsService;
import musictracker.Versioning.ApiVersion;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.IntStream;

@Validated
@RestController
@Tag(name = "Stats", description = "Rankings pessoais de faixas mais ouvidas (histórico completo e mês corrente)")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats/top-tracks")
    @Operation(
            summary = "Top N faixas mais ouvidas de um usuário (histórico completo)",
            description = "Considera todas as escutas do usuário informado e ordena por número de "
                    + "vezes ouvida; em empate, por tempo total ouvido e depois pelo ID da faixa. "
                    + "É recalculado a cada chamada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking retornado com sucesso. A versão 1 devolve "
                    + "TopTrackDTO; a versão 2 devolve TopTrackV2Response (com rank e totalMinutes). "
                    + "O cabeçalho X-API-Version da resposta informa a versão usada",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(oneOf = {TopTrackDTO.class, TopTrackV2Response.class})))),
            @ApiResponse(responseCode = "400", description = "'userId' ausente, 'limit' inválido "
                    + "(inteiro entre 1 e 50) ou versão da API não suportada"),
            @ApiResponse(responseCode = "404", description = "Não existe usuário com esse ID")
    })
    public ResponseEntity<?> topTracks(
            @Parameter(in = ParameterIn.HEADER, name = "X-API-Version",
                    description = "Versão do endpoint: 1 (padrão) ou 2",
                    schema = @Schema(allowableValues = {"1", "2"}, defaultValue = "1"))
            @RequestHeader(name = "X-API-Version", required = false) String apiVersion,
            @Parameter(description = "ID do usuário dono do ranking", example = "1")
            @RequestParam Long userId,
            @Parameter(description = "Quantas faixas retornar no ranking (mínimo 1, máximo 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        ApiVersion version = ApiVersion.parse(apiVersion);
        return respond(version, statsService.topTracks(userId, limit));
    }

    @GetMapping("/stats/top-tracks/monthly")
    @Operation(
            summary = "Top N faixas mais ouvidas de um usuário no mês corrente",
            description = "Mesma lógica do ranking completo, considerando apenas escutas a partir "
                    + "do primeiro dia do mês atual (00:00)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking retornado com sucesso. A versão 1 devolve "
                    + "TopTrackDTO; a versão 2 devolve TopTrackV2Response (com rank e totalMinutes). "
                    + "O cabeçalho X-API-Version da resposta informa a versão usada",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(oneOf = {TopTrackDTO.class, TopTrackV2Response.class})))),
            @ApiResponse(responseCode = "400", description = "'userId' ausente, 'limit' inválido "
                    + "(inteiro entre 1 e 50) ou versão da API não suportada"),
            @ApiResponse(responseCode = "404", description = "Não existe usuário com esse ID")
    })
    public ResponseEntity<?> topTracksMonthly(
            @Parameter(in = ParameterIn.HEADER, name = "X-API-Version",
                    description = "Versão do endpoint: 1 (padrão) ou 2",
                    schema = @Schema(allowableValues = {"1", "2"}, defaultValue = "1"))
            @RequestHeader(name = "X-API-Version", required = false) String apiVersion,
            @Parameter(description = "ID do usuário dono do ranking", example = "1")
            @RequestParam Long userId,
            @Parameter(description = "Quantas faixas retornar no ranking do mês (mínimo 1, máximo 50)", example = "3")
            @RequestParam(defaultValue = "3") @Min(1) @Max(50) int limit) {
        ApiVersion version = ApiVersion.parse(apiVersion);
        return respond(version, statsService.topTracksMonthly(userId, limit));
    }

    private ResponseEntity<?> respond(ApiVersion version, List<TopTrackDTO> ranking) {
        Object body = ranking;
        if (version == ApiVersion.V2) {
            body = IntStream.range(0, ranking.size())
                    .mapToObj(index -> TopTrackV2Response.of(index + 1, ranking.get(index)))
                    .toList();
        }
        return ResponseEntity.ok().header(ApiVersion.HEADER, version.value()).body(body);
    }
}
