package schultz.thomas.schub.connector.freebox.api.dto;

import schultz.thomas.schub.connector.freebox.data.model.FreeboxRedirection;

import java.util.Objects;

/**
 * Une redirection de port, exprimée indépendamment du routeur qui la porte.
 *
 * <p>C'est le seul vocabulaire que ce connecteur expose : {@code FreeboxRedirection} ne
 * franchit jamais la frontière HTTP. Copie volontaire du record du cœur — la découpe
 * assume la duplication des contrats plutôt qu'un jar partagé imposant une cadence de
 * release commune (cf. {@code docs/migration-microservices.md} §9).</p>
 *
 * @param providerId identifiant opaque attribué par le routeur ; {@code null} pour une règle
 *                   voulue qui n'existe pas encore. Le domaine ne l'interprète jamais, il se
 *                   contente de le rendre à l'adapter pour désigner la règle à modifier.
 * @param owner      qui réclame cette règle ({@code minecraft-ftb}, {@code static/wireguard}).
 *                   {@code null} désigne une règle créée à la main sur le routeur, que l'on
 *                   observe sans jamais y toucher.
 * @param open       état voulu ou observé : ouvert sur Internet, ou fermé.
 */
public record PortRule(
        String providerId,
        String owner,
        Protocol protocol,
        int wanPortStart,
        int wanPortEnd,
        String lanIp,
        int lanPort,
        boolean open
) {

    /** Une règle sans propriétaire n'a pas été posée par nous : elle est hors de notre gestion. */
    public boolean managed() {
        return owner != null;
    }

    public PortRuleKey key() {
        return new PortRuleKey(protocol, wanPortStart, wanPortEnd);
    }

    /** Vrai si les deux règles envoient le même trafic au même endroit, ouverture mise à part. */
    public boolean hasSameRoutingAs(PortRule other) {
        return other != null
                && Objects.equals(lanIp, other.lanIp)
                && lanPort == other.lanPort
                && Objects.equals(owner, other.owner);
    }

    /** La même règle, portée par une redirection existante du routeur. */
    public PortRule withProviderId(String id) {
        return new PortRule(id, owner, protocol, wanPortStart, wanPortEnd, lanIp, lanPort, open);
    }

    /** La même règle, ouverte ou fermée. */
    public PortRule withOpen(boolean newOpen) {
        return new PortRule(providerId, owner, protocol, wanPortStart, wanPortEnd, lanIp, lanPort, newOpen);
    }

    /** Libellé court pour les logs et les comptes rendus. */
    public String describe() {
        String range = wanPortStart == wanPortEnd
                ? String.valueOf(wanPortStart)
                : wanPortStart + "-" + wanPortEnd;
        return (owner != null ? owner : "(manuelle)") + " " + protocol.wireName() + "/" + range;
    }
}
