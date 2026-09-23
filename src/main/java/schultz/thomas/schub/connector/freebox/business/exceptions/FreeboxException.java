package schultz.thomas.schub.connector.freebox.business.exceptions;

public class FreeboxException extends RuntimeException {
    public FreeboxException(String message) {
        super(message);
    }

    public FreeboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
