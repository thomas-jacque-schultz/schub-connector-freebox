package schultz.thomas.schub.connector.freebox.model.freebox;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Une redirection de port telle que l'expose /api/vXX/fw/redir/.
 * Les null sont omis à la sérialisation : la Freebox refuse un POST de création
 * portant un "id", et complète elle-même les champs absents.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FreeboxRedirection(
        Integer id,
        Boolean enabled,
        String comment,
        @JsonProperty("ip_proto") String ipProto,
        @JsonProperty("wan_port_start") Integer wanPortStart,
        @JsonProperty("wan_port_end") Integer wanPortEnd,
        @JsonProperty("lan_ip") String lanIp,
        @JsonProperty("lan_port") Integer lanPort,
        @JsonProperty("src_ip") String srcIp,
        String hostname
) {
}
