package schultz.thomas.schub.connector.freebox.api.controller;

import schultz.thomas.schub.connector.freebox.business.exceptions.FreeboxException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/**
 * Traduit un échec de dialogue avec la box en réponse HTTP honnête.
 *
 * <p>502 plutôt que 500 : la panne est en amont, chez le routeur, et l'appelant doit pouvoir
 * distinguer « le connecteur est cassé » de « la box ne répond pas » — le cœur réessaiera de
 * lui-même au prochain passage du réconciliateur.</p>
 */
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
}
