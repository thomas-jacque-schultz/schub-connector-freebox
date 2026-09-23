package schultz.thomas.schub.connector.freebox.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record FreeboxSession(
        String challenge,
        @JsonProperty("logged_in") Boolean loggedIn,
        @JsonProperty("session_token") String sessionToken,
        Map<String, Boolean> permissions
) {

    public static final String SETTINGS_PERMISSION = "settings";

    public boolean hasSettingsPermission() {
        return permissions != null && Boolean.TRUE.equals(permissions.get(SETTINGS_PERMISSION));
    }

    public List<String> grantedPermissions() {
        if (permissions == null) return List.of();
        return permissions.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
    }
}
