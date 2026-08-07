package pl.mm.discountcoupons.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import pl.mm.discountcoupons.api.dto.ValidationError;
import pl.mm.discountcoupons.domain.ClientIpResolutionException;
import pl.mm.discountcoupons.domain.CountryResolutionException;
import pl.mm.discountcoupons.domain.CouponAlreadyExistsException;
import pl.mm.discountcoupons.domain.CouponAlreadyUsedException;
import pl.mm.discountcoupons.domain.CouponCountryMismatchException;
import pl.mm.discountcoupons.domain.CouponExhaustedException;
import pl.mm.discountcoupons.domain.CouponNotFoundException;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CouponNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException e, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler({
            CouponAlreadyExistsException.class,
            CouponAlreadyUsedException.class,
            CouponExhaustedException.class
    })
    public ResponseEntity<ProblemDetail> handleConflict(RuntimeException e, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(CouponCountryMismatchException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(RuntimeException e, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, e.getMessage(), request);
    }

    @ExceptionHandler(ClientIpResolutionException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(RuntimeException e, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(CountryResolutionException.class)
    public ResponseEntity<ProblemDetail> handleServiceUnavailable(RuntimeException e, HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {
        List<ValidationError> errors = e.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField)
                        .thenComparing(error -> Objects.toString(error.getDefaultMessage(), "")))
                .map(error -> new ValidationError(error.getField(), message(error)))
                .toList();

        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, "Request validation failed", request);
        problem.setProperty("errors", errors);
        return response(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedBody(HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Request body is invalid", request);
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String detail,
            HttpServletRequest request) {
        return response(status, createProblem(status, detail, request));
    }

    private static ProblemDetail createProblem(
            HttpStatus status,
            String detail,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    private static ResponseEntity<ProblemDetail> response(HttpStatus status, ProblemDetail problem) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static String message(FieldError error) {
        return Objects.toString(error.getDefaultMessage(), "Invalid value");
    }
}
