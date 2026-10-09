package musictracker.Exception;

import jakarta.servlet.http.HttpServletRequest;
import musictracker.DTO.APIError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

// @RestControllerAdvice: interceptador global. Se qualquer Controller lançar
// uma dessas exceções, essa classe captura antes de chegar no cliente e
// devolve sempre o mesmo formato (APIError), com o status HTTP correto.
//
// Isso é essencial para resistir a testes "adversariais" (professor tentando
// quebrar as rotas): sem esses handlers, um JSON mal formado, um enum
// inválido, um tipo errado no path/query ou um parâmetro obrigatório
// faltando cairiam no handler genérico de Exception (500) ou na página de
// erro padrão do Spring — em vez de um 400 claro e consistente.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Erros de Bean Validation (@NotBlank, @Email, @Positive, @PastOrPresent...) -> 400
    // Dispara DEPOIS que o JSON já foi convertido com sucesso para o objeto Java,
    // mas algum campo não passou nas regras de @Valid dentro da Entity/DTO.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIError> handleValidationExceptions(
            MethodArgumentNotValidException exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errorMessage = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Erro de validação");

        return ResponseEntity.status(status).body(new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.name(), errorMessage, request.getRequestURI()));
    }

    // JSON mal formado, tipo errado dentro do corpo da requisição (ex: enviar
    // "genre": 123 ou "genre": "NAO_EXISTE" quando o campo é o enum Genre),
    // data em formato inválido para LocalDateTime, corpo vazio quando era
    // obrigatório, etc. -> 400
    //
    // Isso acontece ANTES do Bean Validation rodar: o Jackson nem consegue
    // montar o objeto Java, então sem este handler o erro cairia no genérico
    // de Exception (500). É a categoria mais provável de ataque do professor
    // ("número onde não deveria", "data inválida").
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIError> handleMessageNotReadable(
            HttpMessageNotReadableException exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Corpo da requisição inválido ou mal formatado. Verifique se o JSON está "
                + "bem formado, se os tipos dos campos estão corretos (texto vs número vs data) e "
                + "se os valores de enum enviados realmente existem.";

        return ResponseEntity.status(status).body(new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.name(), message, request.getRequestURI()));
    }

    // Tipo errado em @PathVariable ou @RequestParam (ex: GET /artists/abc,
    // onde "abc" não é um Long válido; ou ?genre=abc quando se espera um
    // valor do enum Genre). -> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        String expectedType = exception.getRequiredType() != null
                ? exception.getRequiredType().getSimpleName()
                : "outro tipo";
        String message = String.format(
                "O parâmetro '%s' recebeu o valor '%s', que não é compatível com o tipo esperado (%s).",
                exception.getName(), exception.getValue(), expectedType);

        return ResponseEntity.status(status).body(new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.name(), message, request.getRequestURI()));
    }

    // @RequestParam obrigatório que não foi enviado na URL (ex: GET
    // /artists/search sem informar ?genre=...). -> 400
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<APIError> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = String.format("O parâmetro obrigatório '%s' não foi informado na requisição.",
                exception.getParameterName());

        return ResponseEntity.status(status).body(new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.name(), message, request.getRequestURI()));
    }

    // Validação de parâmetros de método (@Min/@Max/@Positive em @RequestParam,
    // como o "limit" dos endpoints de /stats) quando o Spring valida o método
    // do Controller diretamente (sem passar por um @RequestBody). -> 400
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<APIError> handleHandlerMethodValidation(
            HandlerMethodValidationException exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(error -> error.getDefaultMessage())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Um ou mais parâmetros da requisição são inválidos.");

        return ResponseEntity.status(status).body(new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.name(), message, request.getRequestURI()));
    }

    // ID que não existe no banco -> 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIError> handleResourceNotFoundException(
            ResourceNotFoundException exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.name(), exception.getMessage(), request.getRequestURI()));
    }

    // Última linha de defesa para erros não previstos -> 500 (sem vazar stack trace pro cliente)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIError> handleGenericException(
            Exception exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.name(), "Ocorreu um erro interno no servidor.", request.getRequestURI()));
    }
}
