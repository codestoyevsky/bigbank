package eu.bigbank.mugloar.api;

import eu.bigbank.mugloar.client.MugloarApiException;
import eu.bigbank.mugloar.service.AutoplayConflictException;
import eu.bigbank.mugloar.service.GameNotFoundException;
import eu.bigbank.mugloar.service.TooManyAutoplaysException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Validation errors are handled by the base class; the rest are mapped here. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MugloarApiException.class)
    ProblemDetail handleGameApi(MugloarApiException e) {
        if (e.isUnreachable()) {
            log.warn("Game API unavailable: {}", e.getMessage());
            return problem(HttpStatus.BAD_GATEWAY, "Game API unavailable",
                    "The Dragons of Mugloar API did not respond properly. Please try again.");
        }
        return switch (e.status()) {
            case 410 -> problem(HttpStatus.GONE, "Game over", "This game has ended. Start a new one.");
            case 404 -> problem(HttpStatus.NOT_FOUND, "Not found", "The game, ad or item does not exist.");
            default -> problem(HttpStatus.BAD_REQUEST, "Rejected by game API",
                    "The game refused the request (" + e.getMessage() + "). It may have expired or no longer exist.");
        };
    }

    @ExceptionHandler(GameNotFoundException.class)
    ProblemDetail handleUnknownGame(GameNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Unknown game", e.getMessage());
    }

    @ExceptionHandler(AutoplayConflictException.class)
    ProblemDetail handleConflict(AutoplayConflictException e) {
        return problem(HttpStatus.CONFLICT, "Auto-play already running", e.getMessage());
    }

    @ExceptionHandler(TooManyAutoplaysException.class)
    ProblemDetail handleTooMany(TooManyAutoplaysException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Server busy", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        log.error("Unhandled error", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Server error", "Something went wrong on our side.");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setTitle(title);
        return p;
    }
}
