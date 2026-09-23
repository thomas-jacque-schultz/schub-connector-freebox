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

    @Qualifier("freeboxRestClient")
    private final RestClient restClient;

    private final FreeboxSessionManager sessionManager;

    private final FreeboxProperties freeboxProperties;

    public String unavailableReason() {
        return freeboxProperties.isPaired() ? null : "aucun jeton d'appairage Freebox configuré";
    }

    public List<PortRule> listRules() {
        FreeboxResponse<List<FreeboxRedirection>> response = authenticated(token -> restClient.get()
                .uri("/fw/redir/")
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTH_HEADER, token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> { /* enveloppe JSON lue ci-dessous */ })
                .body(new ParameterizedTypeReference<>() {}));

        if (response.result() == null) {
            return List.of();
        }
        return response.result().stream()
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

        FreeboxResponse<FreeboxRedirection> response = authenticated(token -> restClient.post()
                .uri("/fw/redir/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTH_HEADER, token)
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> { /* enveloppe JSON lue ci-dessous */ })
                .body(new ParameterizedTypeReference<FreeboxResponse<FreeboxRedirection>>() {}));

        return created(response, rule);
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
        FreeboxResponse<FreeboxRedirection> response = authenticated(token -> restClient.put()
                .uri("/fw/redir/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTH_HEADER, token)
                .body(patch)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> { /* enveloppe JSON lue ci-dessous */ })
                .body(new ParameterizedTypeReference<FreeboxResponse<FreeboxRedirection>>() {}));

        return created(response, rule.withProviderId(providerId));
    }

    public void deleteRule(String providerId) {
        int id = parseProviderId(providerId);
        authenticated(token -> restClient.delete()
                .uri("/fw/redir/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTH_HEADER, token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> { /* enveloppe JSON lue ci-dessous */ })
                .body(new ParameterizedTypeReference<FreeboxResponse<Object>>() {}));
    }

    private PortRule created(FreeboxResponse<FreeboxRedirection> response, PortRule fallback) {
        if (response.result() == null) {
            return fallback;
        }
        return toDomain(response.result()).orElse(fallback);
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

    private <T> FreeboxResponse<T> authenticated(Function<String, FreeboxResponse<T>> call) {
        FreeboxResponse<T> response = call.apply(sessionManager.currentSessionToken());

        if (response != null && !response.success() && AUTH_REQUIRED.equals(response.errorCode())) {
            log.info("Session Freebox expirée, réouverture");
            sessionManager.invalidate();
            response = call.apply(sessionManager.currentSessionToken());
        }

        if (response == null || !response.success()) {
            throw new FreeboxException("Appel Freebox en échec: "
                    + (response != null ? response.errorCode() + " / " + response.msg() : "réponse vide"));
        }
        return response;
    }
}
