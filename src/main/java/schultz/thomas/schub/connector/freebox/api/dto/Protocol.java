package schultz.thomas.schub.connector.freebox.api.dto;

import java.util.Arrays;
import java.util.Optional;

/** Protocole d'une redirection de port. */
public enum Protocol {
    TCP,
    UDP;

    /** Tolère la casse et les espaces ; vide si la valeur ne désigne aucun protocole connu. */
    public static Optional<Protocol> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values()).filter(protocol -> protocol.name().equals(normalized)).findFirst();
    }

    /** Forme attendue par la plupart des API de routeur. */
    public String wireName() {
        return name().toLowerCase();
    }
}
