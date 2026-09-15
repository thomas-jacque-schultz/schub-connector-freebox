package schultz.thomas.schub.connector.freebox.model.portforwarding;

/**
 * Identité d'une redirection côté WAN. Un routeur ne peut pas héberger deux règles
 * sur le même protocole et la même plage de ports publics : c'est donc ce triplet
 * qui permet de rapprocher une règle voulue d'une règle déjà en place.
 */
public record PortRuleKey(Protocol protocol, int wanPortStart, int wanPortEnd) {
}
