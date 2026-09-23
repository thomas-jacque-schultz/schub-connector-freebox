package schultz.thomas.schub.connector.freebox.api.dto;

public record PortRuleKey(Protocol protocol, int wanPortStart, int wanPortEnd) {
}
