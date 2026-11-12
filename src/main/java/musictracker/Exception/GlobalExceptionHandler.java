package musictracker.Exception;

import jakarta.servlet.http.HttpServletRequest;
import musictracker.DTO.APIError;
import org.springframework.dao.DataIntegrityViolationException;
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


@RestControllerAdvice
public class GlobalExceptionHandler {

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

    private ResponseEntity<APIError> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.name(), message, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIError> handleGenericException(
            Exception exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(new APIError(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.name(), "Ocorreu um erro interno no servidor.", request.getRequestURI()));
    }
}
