package schultz.thomas.schub.connector.freebox.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "freebox")
public class FreeboxProperties {

    private String baseUrl = "http://mafreebox.freebox.fr";

    private String apiVersion = "v16";

    private String appId = "fr.schultz.schub.portmanager";

    private String appName = "Schub Port Manager";

    private String appVersion = "1.0.0";

    private String deviceName = "dynamis";

    private String appToken = "";

    // La box n'a qu'un champ comment libre : le propriétaire y est encodé par ce marqueur. Le cœur ne parle qu'en owner.
    private String marker = "[schub]";

    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(5);

    public boolean isPaired() {
        return appToken != null && !appToken.isBlank();
    }
}
