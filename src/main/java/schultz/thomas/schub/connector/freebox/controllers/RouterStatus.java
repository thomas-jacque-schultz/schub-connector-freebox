package schultz.thomas.schub.connector.freebox.controllers;

/**
 * État du lien avec le routeur, pour diagnostiquer sans lire les logs.
 *
 * @param unavailableReason motif empêchant d'agir, ou {@code null} si le routeur est utilisable
 */
public record RouterStatus(
        String router,
        boolean paired,
        String unavailableReason,
        String marker
) {
}
