package schultz.thomas.schub.connector.freebox.api.dto;

import java.util.Arrays;
import java.util.Optional;

public enum Protocol {
    TCP,
    UDP;

    public static Optional<Protocol> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values()).filter(protocol -> protocol.name().equals(normalized)).findFirst();
    }

    public String wireName() {
        return name().toLowerCase();
    }
}
