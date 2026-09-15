package schultz.thomas.schub.connector.freebox.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Comment joindre la Freebox : adresse, version d'API, identité applicative et jeton d'appairage.
 *
 * <p>Uniquement de l'infrastructure, plus le marqueur de propriété. La politique d'ouverture
 * (quoi ouvrir, quels ports sont interdits, que faire des règles orphelines) vit dans le cœur
 * et n'a pas à traverser la frontière : ce connecteur ne sait pas <em>pourquoi</em> on l'appelle.</p>
 *
 * <p>Le jeton s'obtient une seule fois par appairage physique sur l'écran de la box
 * (cf. {@code scripts/freebox-pair.sh}), puis est fourni comme secret Docker.</p>
 */
@Data
@ConfigurationProperties(prefix = "freebox")
public class FreeboxProperties {

    private String baseUrl = "http://mafreebox.freebox.fr";

    /** Version majeure de l'API, telle que renvoyée par /api_version (Freebox v8 : v16). */
    private String apiVersion = "v16";

    private String appId = "fr.schultz.schub.portmanager";

    private String appName = "Schub Port Manager";

    private String appVersion = "1.0.0";

    private String deviceName = "dynamis";

    /** Jeton d'appairage. Vide = connecteur en veille, aucune requête émise vers la box. */
    private String appToken = "";

    /**
     * Marqueur inscrit dans le libellé de chaque règle posée par Schub.
     *
     * <p>Détail d'encodage propre à la Freebox, qui n'a qu'un champ {@code comment} libre pour
     * porter la notion de propriétaire. Le cœur ne le connaît pas : il parle en {@code owner},
     * ce connecteur traduit dans les deux sens. Toute règle de la box ne portant pas ce
     * marqueur est traitée comme manuelle et n'est jamais modifiée.</p>
     */
    private String marker = "[schub]";

    /**
     * Bornes sur les échanges HTTP avec la box. Une box qui ne répond plus ne doit pas
     * bloquer l'appelant : l'appel échoue vite et la réconciliation périodique repassera.
     */
    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(5);

    public boolean isPaired() {
        return appToken != null && !appToken.isBlank();
    }
}
