package musictracker.Exception;

import jakarta.servlet.http.HttpServletRequest;
import musictracker.DTO.APIError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIError> handleValidationExceptions(
            MethodArgumentNotValidException exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errorMessage = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Erro de validação");

        return  ResponseEntity.status(status).body(new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.name(), errorMessage, request.getRequestURI()));
    }

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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIError> handleResourceNotFoundException(
            ResourceNotFoundException exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.name(), exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(UnsupportedApiVersionException.class)
    public ResponseEntity<APIError> handleUnsupportedApiVersion(
            UnsupportedApiVersionException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<APIError> handleConflict(
            ConflictException exception, HttpServletRequest request) {

        return build(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIError> handleDataIntegrity(
            DataIntegrityViolationException exception, HttpServletRequest request) {

        return build(HttpStatus.CONFLICT,
                "A operação conflita com dados existentes: o valor já está em uso ou o registro "
                        + "possui outros registros dependentes (remova ou altere os dependentes primeiro).",
                request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<APIError> handleUnauthorized(
            UnauthorizedException exception, HttpServletRequest request) {

        return build(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<APIError> handleForbidden(
            ForbiddenException exception, HttpServletRequest request) {

        return build(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<APIError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .contentType(MediaType.APPLICATION_JSON);
        Set<HttpMethod> allowed = exception.getSupportedHttpMethods();
        String message = "O método " + exception.getMethod() + " não é permitido nesta rota.";
        if (allowed != null && !allowed.isEmpty()) {
            builder.allow(allowed.toArray(new HttpMethod[0]));
            message += " Métodos aceitos: " + allowed.stream().map(HttpMethod::name).sorted().toList() + ".";
        }
        return builder.body(apiError(HttpStatus.METHOD_NOT_ALLOWED, message, request));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<APIError> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {

        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Tipo de conteúdo não suportado. Envie o cabeçalho Content-Type: application/json.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<APIError> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException exception, HttpServletRequest request) {

        return build(HttpStatus.NOT_ACCEPTABLE,
                "Formato de resposta não suportado. Esta API só responde em JSON (Accept: application/json).", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<APIError> handleNoResource(
            NoResourceFoundException exception, HttpServletRequest request) {

        return build(HttpStatus.NOT_FOUND, "A rota " + request.getRequestURI() + " não existe.", request);
    }

    private ResponseEntity<APIError> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(apiError(status, message, request));
    }

    private APIError apiError(HttpStatus status, String message, HttpServletRequest request) {
        return new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.name(), message, request.getRequestURI());
    }

    private static boolean isInvalidSort(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getClass().getSimpleName().equals("PropertyReferenceException")) {
                return true;
            }
        }
        return false;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIError> handleGenericException(
            Exception exception, HttpServletRequest request) {

        if (isInvalidSort(exception)) {
            return build(HttpStatus.BAD_REQUEST,
                    "O parâmetro 'sort' usa um campo que não existe neste recurso.", request);
        }

        log.error("Erro não tratado em {} {}", request.getMethod(), request.getRequestURI(), exception);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.name(), "Ocorreu um erro interno no servidor.", request.getRequestURI()));
    }
}
