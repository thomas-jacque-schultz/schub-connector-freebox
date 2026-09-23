package schultz.thomas.schub.connector.freebox.api.controller;

import schultz.thomas.schub.connector.freebox.api.dto.PortRule;
import schultz.thomas.schub.connector.freebox.api.dto.RouterStatus;
import schultz.thomas.schub.connector.freebox.business.services.FreeboxRedirectionService;
import schultz.thomas.schub.connector.freebox.config.FreeboxProperties;
import schultz.thomas.schub.connector.freebox.data.model.FreeboxRedirection;

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
import java.util.List;

// Contrat en PortRule uniquement : FreeboxRedirection ne sort jamais du connecteur.
@RestController
@RequiredArgsConstructor
public class RedirectionController {

    private final FreeboxRedirectionService redirectionService;

    private final FreeboxProperties freeboxProperties;

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
