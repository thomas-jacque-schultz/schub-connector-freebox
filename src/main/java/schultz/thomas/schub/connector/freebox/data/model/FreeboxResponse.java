package schultz.thomas.schub.connector.freebox.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enveloppe commune à toutes les réponses de l'API Freebox OS.
 * En cas d'échec, {@code errorCode} porte le motif ("auth_required", "insufficient_rights"...).
 */
public record FreeboxResponse<T>(
        boolean success,
        String msg,
        @JsonProperty("error_code") String errorCode,
        T result
) {
}
