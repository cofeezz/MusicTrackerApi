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
            @ApiResponse(responseCode = "200", description = "Ranking retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "'userId' ausente ou 'limit' inválido "
                    + "(inteiro entre 1 e 50)"),
            @ApiResponse(responseCode = "404", description = "Não existe usuário com esse ID")
    })
    public List<TopTrackDTO> topTracks(
            @Parameter(description = "ID do usuário dono do ranking", example = "1")
            @RequestParam Long userId,
            @Parameter(description = "Quantas faixas retornar no ranking (mínimo 1, máximo 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return statsService.topTracks(userId, limit);
    }

    @GetMapping("/stats/top-tracks/monthly")
    @Operation(
            summary = "Top N faixas mais ouvidas de um usuário no mês corrente",
            description = "Mesma lógica do ranking completo, considerando apenas escutas a partir "
                    + "do primeiro dia do mês atual (00:00)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "'userId' ausente ou 'limit' inválido "
                    + "(inteiro entre 1 e 50)"),
            @ApiResponse(responseCode = "404", description = "Não existe usuário com esse ID")
    })
    public List<TopTrackDTO> topTracksMonthly(
            @Parameter(description = "ID do usuário dono do ranking", example = "1")
            @RequestParam Long userId,
            @Parameter(description = "Quantas faixas retornar no ranking do mês (mínimo 1, máximo 50)", example = "3")
            @RequestParam(defaultValue = "3") @Min(1) @Max(50) int limit) {
        return statsService.topTracksMonthly(userId, limit);
    }
}
