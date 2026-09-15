package schultz.thomas.schub.connector.freebox.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import schultz.thomas.schub.connector.freebox.business.services.FreeboxRedirectionService;
import schultz.thomas.schub.connector.freebox.config.FreeboxProperties;
import schultz.thomas.schub.connector.freebox.model.portforwarding.PortRule;

import java.util.List;

/**
 * API interne du connecteur : les redirections du routeur, exprimées en {@link PortRule}.
 *
 * <p>{@code FreeboxRedirection} ne franchit jamais cette frontière. Un appelant qui lirait du
 * vocabulaire Freebox ici serait couplé à la marque du routeur, ce que toute la découpe cherche
 * précisément à éviter.</p>
 *
 * <p>Protégé comme le reste du maillage par {@code X-Internal-Secret} ; seul
 * {@code GET /actuator/health} est ouvert.</p>
 */
@RestController
@RequiredArgsConstructor
public class RedirectionController {

    private final FreeboxRedirectionService redirectionService;

    private final FreeboxProperties freeboxProperties;

    /** Toutes les redirections portées par le routeur, les manuelles comprises. */
    @GetMapping("/redirections")
    public List<PortRule> list() {
        return redirectionService.listRules();
    }

    @PostMapping("/redirections")
    public ResponseEntity<PortRule> create(@RequestBody PortRule rule) {
        return ResponseEntity.status(HttpStatus.CREATED).body(redirectionService.createRule(rule));
    }

    @PutMapping("/redirections/{id}")
    public PortRule update(@PathVariable String id, @RequestBody PortRule rule) {
        return redirectionService.updateRule(id, rule);
    }

    @DeleteMapping("/redirections/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        redirectionService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    /** État du lien avec la box, sans rien écrire. */
    @GetMapping("/router/status")
    public RouterStatus status() {
        return new RouterStatus(
                freeboxProperties.getBaseUrl() + "/api/" + freeboxProperties.getApiVersion(),
                freeboxProperties.isPaired(),
                redirectionService.unavailableReason(),
                freeboxProperties.getMarker()
        );
    }
}
