package musictracker.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import musictracker.DTO.TopTrackDTO;
import musictracker.Service.StatsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Não é CRUD de uma entidade própria — é a "cápsula sonora": ranking
// agregado em cima dos registros de Listen.
//
// @Validated na classe é o que liga as anotações @Min/@Max colocadas
// diretamente no parâmetro "limit" (abaixo) — sem isso, elas seriam
// ignoradas silenciosamente pelo Spring MVC.
@Validated
@RestController
@Tag(name = "Stats", description = "Rankings agregados (top 10 geral / top 3 do mês)")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats/top-tracks")
    @Operation(
            summary = "Top N faixas mais ouvidas (histórico completo, recalculado a cada chamada)",
            description = "Percorre TODO o histórico de escutas (tabela Listen) e devolve as faixas "
                    + "mais ouvidas, ordenadas por número de vezes ouvida (e, em empate, por tempo "
                    + "total ouvido). É recalculado a cada chamada: não existe um valor 'salvo' de "
                    + "ranking, então o resultado muda assim que novas escutas são registradas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "O parâmetro 'limit' é inválido "
                    + "(deve ser um número inteiro entre 1 e 50; valores como texto, negativos, "
                    + "zero ou muito grandes são rejeitados)")
    })
    public List<TopTrackDTO> topTracks(
            @Parameter(description = "Quantas faixas retornar no ranking (mínimo 1, máximo 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return statsService.topTracks(limit);
    }

    @GetMapping("/stats/top-tracks/monthly")
    @Operation(
            summary = "Top N faixas mais ouvidas no mês corrente (padrão: top 3 detalhado)",
            description = "Mesma lógica do ranking geral, mas considerando apenas escutas registradas "
                    + "a partir do primeiro dia do mês atual (00:00). É a versão usada pela tela "
                    + "de 'resumo do mês', parecida com a Cápsula Sonora do Spotify."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "O parâmetro 'limit' é inválido "
                    + "(deve ser um número inteiro entre 1 e 50)")
    })
    public List<TopTrackDTO> topTracksMonthly(
            @Parameter(description = "Quantas faixas retornar no ranking do mês (mínimo 1, máximo 50)", example = "3")
            @RequestParam(defaultValue = "3") @Min(1) @Max(50) int limit) {
        return statsService.topTracksMonthly(limit);
    }
}
