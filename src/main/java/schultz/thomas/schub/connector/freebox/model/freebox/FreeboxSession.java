package schultz.thomas.schub.connector.freebox.model.freebox;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** Réponse de GET /login/ (challenge) et de POST /login/session/ (session_token). */
public record FreeboxSession(
        String challenge,
        @JsonProperty("logged_in") Boolean loggedIn,
        @JsonProperty("session_token") String sessionToken,
        Map<String, Boolean> permissions
) {

    /** Permission Freebox OS requise pour manipuler les redirections de ports. */
    public static final String SETTINGS_PERMISSION = "settings";

    public boolean hasSettingsPermission() {
        return permissions != null && Boolean.TRUE.equals(permissions.get(SETTINGS_PERMISSION));
    }

    public List<String> grantedPermissions() {
        if (permissions == null) return List.of();
        return permissions.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
    }
}
