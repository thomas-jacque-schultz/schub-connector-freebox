package schultz.thomas.schub.connector.freebox.business.exceptions;

/** Échec d'un échange avec l'API Freebox OS (appairage, session, redirections). */
public class FreeboxException extends RuntimeException {
    public FreeboxException(String message) {
        super(message);
    }

    public FreeboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
