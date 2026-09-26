package schultz.thomas.schub.connector.freebox.business.services;

import schultz.thomas.schub.connector.freebox.api.dto.PortRule;
import schultz.thomas.schub.connector.freebox.api.dto.Protocol;
import schultz.thomas.schub.connector.freebox.business.exceptions.FreeboxException;
import schultz.thomas.schub.connector.freebox.config.FreeboxProperties;
import schultz.thomas.schub.connector.freebox.data.model.FreeboxRedirection;
import schultz.thomas.schub.connector.freebox.data.model.FreeboxResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreeboxRedirectionService {

    private static final String AUTH_HEADER = "X-Fbx-App-Auth";
    private static final String AUTH_REQUIRED = "auth_required";
    private static final String WILDCARD_SOURCE_IP = "0.0.0.0";
    private static final ParameterizedTypeReference<FreeboxResponse<JsonNode>> ENVELOPE = new ParameterizedTypeReference<>() {};

    @Qualifier("freeboxRestClient")
    private final RestClient restClient;

    private final FreeboxSessionManager sessionManager;

    private final FreeboxProperties freeboxProperties;

    private final ObjectMapper objectMapper;

    public String unavailableReason() {
        return freeboxProperties.isPaired() ? null : "aucun jeton d'appairage Freebox configuré";
    }

    public List<PortRule> listRules() {
        List<FreeboxRedirection> redirections = authenticated(token -> restClient.get()
                .uri("/fw/redir/")
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTH_HEADER, token), new TypeReference<>() {});

        if (redirections == null) {
            return List.of();
        }
        return redirections.stream()
                .map(this::toDomain)
                .flatMap(Optional::stream)
                .toList();
    }

    public PortRule createRule(PortRule rule) {
        FreeboxRedirection payload = new FreeboxRedirection(
                null,
                rule.open(),
                toComment(rule.owner()),
                rule.protocol().wireName(),
                rule.wanPortStart(),
                rule.wanPortEnd(),
                rule.lanIp(),
                rule.lanPort(),
                WILDCARD_SOURCE_IP,
                null
        );

        FreeboxRedirection created = authenticated(token -> restClient.post()
                .uri("/fw/redir/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTH_HEADER, token)
                .body(payload), new TypeReference<>() {});

        return orFallback(created, rule);
    }

    public PortRule updateRule(String providerId, PortRule rule) {
        FreeboxRedirection patch = new FreeboxRedirection(
                null,
                rule.open(),
                toComment(rule.owner()),
                null, null, null,
                rule.lanIp(),
                rule.lanPort(),
                null, null
        );

        int id = parseProviderId(providerId);
        FreeboxRedirection updated = authenticated(token -> restClient.put()
                .uri("/fw/redir/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTH_HEADER, token)
                .body(patch), new TypeReference<>() {});

        return orFallback(updated, rule.withProviderId(providerId));
    }

    public void deleteRule(String providerId) {
        int id = parseProviderId(providerId);
        authenticated(token -> restClient.delete()
                .uri("/fw/redir/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTH_HEADER, token), new TypeReference<JsonNode>() {});
    }

    private PortRule orFallback(FreeboxRedirection redirection, PortRule fallback) {
        if (redirection == null) {
            return fallback;
        }
        return toDomain(redirection).orElse(fallback);
    }

    private Optional<PortRule> toDomain(FreeboxRedirection redirection) {
        Optional<Protocol> protocol = Protocol.parse(redirection.ipProto());
        if (protocol.isEmpty() || redirection.wanPortStart() == null) {
            log.debug("Redirection Freebox ignorée, non représentable: {}", redirection);
            return Optional.empty();
        }

        int wanPortStart = redirection.wanPortStart();
        int wanPortEnd = redirection.wanPortEnd() != null ? redirection.wanPortEnd() : wanPortStart;
        int lanPort = redirection.lanPort() != null ? redirection.lanPort() : wanPortStart;

        return Optional.of(new PortRule(
                redirection.id() != null ? String.valueOf(redirection.id()) : null,
                toOwner(redirection.comment()),
                protocol.get(),
                wanPortStart,
                wanPortEnd,
                redirection.lanIp(),
                lanPort,
                Boolean.TRUE.equals(redirection.enabled())
        ));
    }

    private String toComment(String owner) {
        return freeboxProperties.getMarker() + " " + owner;
    }

    private String toOwner(String comment) {
        String marker = freeboxProperties.getMarker();
        if (comment == null || !comment.startsWith(marker)) {
            return null;
        }
        String owner = comment.substring(marker.length()).trim();
        return owner.isEmpty() ? null : owner;
    }

    private int parseProviderId(String providerId) {
        if (providerId == null) {
            throw new FreeboxException("Règle sans identifiant Freebox");
        }
        try {
            return Integer.parseInt(providerId);
        } catch (NumberFormatException e) {
            throw new FreeboxException("Identifiant Freebox illisible: " + providerId, e);
        }
    }

    // result n'est converti qu'après lecture de success : sur auth_required, la box y met un objet (challenge), quel que soit le type attendu.
    private <T> T authenticated(Function<String, RestClient.RequestHeadersSpec<?>> request, TypeReference<T> type) {
        Function<String, FreeboxResponse<JsonNode>> call = token -> request.apply(token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, clientResponse) -> { })
                .body(ENVELOPE);

        FreeboxResponse<JsonNode> response = call.apply(sessionManager.currentSessionToken());

        if (response != null && !response.success() && AUTH_REQUIRED.equals(response.errorCode())) {
            log.info("Session Freebox expirée, réouverture");
            sessionManager.invalidate();
            response = call.apply(sessionManager.currentSessionToken());
        }

        if (response == null || !response.success()) {
            throw new FreeboxException("Appel Freebox en échec: "
                    + (response != null ? response.errorCode() + " / " + response.msg() : "réponse vide"));
        }
        JsonNode result = response.result();
        return result == null || result.isNull() ? null : objectMapper.convertValue(result, type);
    }
}
