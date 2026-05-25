package com.pos.factura.exception;

import com.pos.factura.dto.FacturaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Errores de validación de Bean Validation (@NotNull, @NotBlank, etc.)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<FacturaResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        log.warn("Error de validación en request: {}", errores);

        return ResponseEntity.badRequest().body(
                FacturaResponse.builder()
                        .exitoso(false)
                        .mensaje("Error de validación en los datos enviados")
                        .errores(errores.toArray(new String[0]))
                        .build()
        );
    }

    /**
     * Errores de validación de negocio (reglas AFIP)
     */
    @ExceptionHandler(FacturaValidationException.class)
    public ResponseEntity<FacturaResponse> handleFacturaValidation(FacturaValidationException ex) {
        log.warn("Error de validación de negocio: {}", ex.getErrores());

        return ResponseEntity.badRequest().body(
                FacturaResponse.builder()
                        .exitoso(false)
                        .mensaje("Error de validación del comprobante")
                        .errores(ex.getErrores().toArray(new String[0]))
                        .build()
        );
    }

    /**
     * Errores de comunicación con la API externa
     */
    @ExceptionHandler(ApiComunicacionException.class)
    public ResponseEntity<FacturaResponse> handleApiComunicacion(ApiComunicacionException ex) {
        log.error("Error comunicándose con la API de factura electrónica: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                FacturaResponse.builder()
                        .exitoso(false)
                        .mensaje("Error comunicándose con el servicio de factura electrónica: " + ex.getMessage())
                        .build()
        );
    }

    /**
     * Cualquier otro error inesperado
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<FacturaResponse> handleGeneral(Exception ex) {
        log.error("Error inesperado procesando factura", ex);

        return ResponseEntity.internalServerError().body(
                FacturaResponse.builder()
                        .exitoso(false)
                        .mensaje("Error interno del servidor: " + ex.getMessage())
                        .build()
        );
    }
}
