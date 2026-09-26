package schultz.thomas.schub.connector.freebox.api.controller;

import schultz.thomas.schub.connector.freebox.business.exceptions.FreeboxException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
@Slf4j
@RestControllerAdvice
public class FreeboxExceptionHandler {

    @ExceptionHandler(FreeboxException.class)
    public ProblemDetail handleFreeboxFailure(FreeboxException exception) {
        log.warn("Échec du dialogue avec la Freebox: {}", exception.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
        detail.setTitle("Le routeur n'a pas pu être joint ou a refusé l'opération");
        return detail;
    }

    @ExceptionHandler(RestClientException.class)
    public ProblemDetail handleUnreachable(RestClientException exception) {
        log.warn("Freebox injoignable: {}", exception.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Le routeur est injoignable.");
        detail.setTitle("Le routeur n'a pas pu être joint ou a refusé l'opération");
        return detail;
    }
}
