package schultz.thomas.schub.connector.freebox.business.services;

import schultz.thomas.schub.connector.freebox.business.exceptions.FreeboxException;
import schultz.thomas.schub.connector.freebox.config.FreeboxProperties;
import schultz.thomas.schub.connector.freebox.data.model.FreeboxResponse;
import schultz.thomas.schub.connector.freebox.data.model.FreeboxSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Map;

// Protocole : GET /login/ → challenge ; session = HMAC-SHA1(challenge, app_token) → session_token
// à poser dans X-Fbx-App-Auth. Expire après quelques dizaines de minutes d'inactivité.
@Slf4j
@Component
@RequiredArgsConstructor
class FreeboxSessionManager {

    private static final String HMAC_ALGORITHM = "HmacSHA1";

    @Qualifier("freeboxRestClient")
    private final RestClient restClient;

    private final FreeboxProperties properties;

    private volatile String sessionToken;

    String currentSessionToken() {
        String cached = sessionToken;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (sessionToken == null) {
                sessionToken = openSession();
            }
            return sessionToken;
        }
    }

    void invalidate() {
        sessionToken = null;
    }

    private String openSession() {
        if (!properties.isPaired()) {
            throw new FreeboxException("Aucun jeton d'appairage Freebox configuré (freebox.app-token)");
        }

        String challenge = fetchChallenge();
        String password = hmacSha1(properties.getAppToken(), challenge);

        FreeboxResponse<FreeboxSession> response = restClient.post()
                .uri("/login/session/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("app_id", properties.getAppId(), "password", password))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> { /* corps JSON exploité ci-dessous */ })
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || !response.success() || response.result() == null) {
            throw new FreeboxException("Ouverture de session Freebox refusée: "
                    + (response != null ? response.errorCode() + " / " + response.msg() : "réponse vide"));
        }

        FreeboxSession session = response.result();
        if (!session.hasSettingsPermission()) {
            throw new FreeboxException("L'application est appairée mais n'a pas la permission '"
                    + FreeboxSession.SETTINGS_PERMISSION + "'. Accordez « Modification des réglages de la Freebox » "
                    + "dans Freebox OS > Paramètres > Gestion des accès. Permissions obtenues: "
                    + session.grantedPermissions());
        }

        log.info("Session Freebox ouverte (permissions={})", session.grantedPermissions());
        return session.sessionToken();
    }

    private String fetchChallenge() {
        FreeboxResponse<FreeboxSession> response = restClient.get()
                .uri("/login/")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> { /* corps JSON exploité ci-dessous */ })
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.result() == null || response.result().challenge() == null) {
            throw new FreeboxException("Impossible de récupérer le challenge de connexion Freebox");
        }
        return response.result().challenge();
    }

    static String hmacSha1(String key, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new FreeboxException("Calcul HMAC-SHA1 impossible", e);
        }
    }
}
