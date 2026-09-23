package schultz.thomas.schub.connector.freebox.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(FreeboxProperties.class)
public class FreeboxApiConfiguration {

    private final FreeboxProperties freeboxProperties;

    @Bean("freeboxRestClient")
    public RestClient freeboxRestClient() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(freeboxProperties.getConnectTimeout())
                .withReadTimeout(freeboxProperties.getReadTimeout());

        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .baseUrl(freeboxProperties.getBaseUrl() + "/api/" + freeboxProperties.getApiVersion())
                .build();
    }
}
