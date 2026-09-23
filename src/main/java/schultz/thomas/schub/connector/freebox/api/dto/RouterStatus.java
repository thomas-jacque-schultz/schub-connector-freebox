package schultz.thomas.schub.connector.freebox.api.dto;

public record RouterStatus(
        String router,
        boolean paired,
        String unavailableReason,
        String marker
) {
}
