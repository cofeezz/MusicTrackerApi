package musictracker.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import musictracker.DTO.CommunityComparisonResponse;
import musictracker.DTO.CommunityTopTrackResponse;
import musictracker.Service.StatsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@Tag(name = "Comunidade", description = "Estatísticas **agregadas e anônimas** (porcentagens e contagens, sem nomes, e-mails ou IDs de usuários) para comparar o seu ranking com o da comunidade, como no Spotify.")
public class CommunityStatsController {

    private final StatsService statsService;

    public CommunityStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats/community/comparison")
    @Operation(
            summary = "Comparar o seu ranking com a comunidade",
            description = "**Aviso de privacidade:** as escutas de todos os usuários podem ser usadas, de forma "
                    + "agregada e anônima, para comparar o seu ranking com o da comunidade, como no Spotify. "
                    + "A resposta traz apenas porcentagens e contagens: nunca o nome, o e-mail, o ID ou as "
                    + "escutas individuais de outro usuário. Para cada uma das suas faixas mais ouvidas, "
                    + "informa a porcentagem de ouvintes da comunidade que também a ouviram e em qual "
                    + "porcentagem (top X%) dos ouvintes da faixa você está. Só o dono da chave pode comparar o próprio ranking."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comparação retornada (lista vazia se você ainda não tem escutas)"),
            @ApiResponse(responseCode = "400", description = "'userId' ausente ou 'limit' inválido (inteiro entre 1 e 50)"),
            @ApiResponse(responseCode = "403", description = "O userId não é o dono da chave de API"),
            @ApiResponse(responseCode = "404", description = "Não existe usuário com esse ID")
    })
    public CommunityComparisonResponse comparison(
            @Parameter(description = "ID do usuário dono da chave de API", example = "1")
            @RequestParam Long userId,
            @Parameter(description = "Quantas das suas faixas mais ouvidas comparar (mínimo 1, máximo 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return statsService.communityComparison(userId, limit);
    }

    @GetMapping("/stats/community/top-tracks")
    @Operation(
            summary = "Top global da comunidade",
            description = "**Aviso de privacidade:** este ranking é calculado com as escutas de todos os usuários, "
                    + "de forma agregada e anônima. Mostra só as faixas com mais ouvintes diferentes e a "
                    + "porcentagem de ouvintes da comunidade que as ouviram, sem citar nenhum usuário."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Top global retornado (lista vazia se ninguém registrou escutas)"),
            @ApiResponse(responseCode = "400", description = "'limit' inválido (inteiro entre 1 e 50)")
    })
    public List<CommunityTopTrackResponse> topTracks(
            @Parameter(description = "Quantas faixas retornar (mínimo 1, máximo 50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return statsService.communityTopTracks(limit);
    }
}
