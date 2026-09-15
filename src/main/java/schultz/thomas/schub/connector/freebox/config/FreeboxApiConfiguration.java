package schultz.thomas.schub.connector.freebox.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Client HTTP vers l'API Freebox OS.
 *
 * <p>L'en-tête de session (X-Fbx-App-Auth) n'est pas posé ici : il change à chaque ouverture
 * de session et est ajouté requête par requête par le gestionnaire de session.</p>
 */
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(FreeboxProperties.class)
public class FreeboxApiConfiguration {

    private final FreeboxProperties freeboxProperties;

    @Bean("freeboxRestClient")
    public RestClient freeboxRestClient() {
        // Sans bornes explicites, une box qui ne répond plus fait pendre indéfiniment
        // l'appel — et donc l'appelant qui l'a déclenché.
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(freeboxProperties.getConnectTimeout())
                .withReadTimeout(freeboxProperties.getReadTimeout());

        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .baseUrl(freeboxProperties.getBaseUrl() + "/api/" + freeboxProperties.getApiVersion())
                .build();
    }
}
