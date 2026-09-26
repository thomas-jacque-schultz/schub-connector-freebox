package schultz.thomas.schub.connector.freebox.business.services;

import schultz.thomas.schub.connector.freebox.api.dto.PortRule;
import schultz.thomas.schub.connector.freebox.api.dto.Protocol;
import schultz.thomas.schub.connector.freebox.business.exceptions.FreeboxException;
import schultz.thomas.schub.connector.freebox.config.FreeboxProperties;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FreeboxRedirectionServiceTest {

    private static final String BASE = "http://box/api/v16";
    private static final String AUTH_REQUIRED = """
            {"success":false,"error_code":"auth_required","msg":"Invalid session token, or no session token sent",
             "result":{"password_salt":"x","challenge":"c2"}}""";
    private static final String REDIRECTION = """
            {"id":7,"enabled":true,"comment":"[schub] minecraft","ip_proto":"tcp","wan_port_start":25565,
             "wan_port_end":25565,"lan_ip":"192.168.1.202","lan_port":25565,"src_ip":"0.0.0.0","hostname":"",
             "host":{"l2ident":{"id":"aa:bb"}}}""";
    private static final PortRule RULE = new PortRule(null, "minecraft", Protocol.TCP, 25565, 25565,
            "192.168.1.202", 25565, true);

    private MockRestServiceServer box;
    private FreeboxRedirectionService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        box = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        FreeboxProperties properties = new FreeboxProperties();
        properties.setAppToken("app-token");
        service = new FreeboxRedirectionService(client, new FreeboxSessionManager(client, properties), properties,
                Jackson2ObjectMapperBuilder.json().build());
    }

    @Test
    void listRulesReopensTheSessionWhenItExpired() {
        session("s1");
        expect(HttpMethod.GET, "/fw/redir/", "s1").andRespond(expired());
        session("s2");
        expect(HttpMethod.GET, "/fw/redir/", "s2").andRespond(ok("[" + REDIRECTION + "]"));

        List<PortRule> rules = service.listRules();

        assertThat(rules).extracting(PortRule::owner, PortRule::providerId).containsExactly(
                org.assertj.core.groups.Tuple.tuple("minecraft", "7"));
        box.verify();
    }

    @Test
    void createRuleReopensTheSessionWhenItExpired() {
        session("s1");
        expect(HttpMethod.POST, "/fw/redir/", "s1").andRespond(expired());
        session("s2");
        expect(HttpMethod.POST, "/fw/redir/", "s2").andRespond(ok(REDIRECTION));

        assertThat(service.createRule(RULE).providerId()).isEqualTo("7");
        box.verify();
    }

    @Test
    void updateRuleReopensTheSessionWhenItExpired() {
        session("s1");
        expect(HttpMethod.PUT, "/fw/redir/7", "s1").andRespond(expired());
        session("s2");
        expect(HttpMethod.PUT, "/fw/redir/7", "s2").andRespond(ok(REDIRECTION));

        assertThat(service.updateRule("7", RULE).providerId()).isEqualTo("7");
        box.verify();
    }

    @Test
    void deleteRuleReopensTheSessionWhenItExpired() {
        session("s1");
        expect(HttpMethod.DELETE, "/fw/redir/7", "s1").andRespond(expired());
        session("s2");
        expect(HttpMethod.DELETE, "/fw/redir/7", "s2").andRespond(ok("null"));

        service.deleteRule("7");
        box.verify();
    }

    @Test
    void aSecondRefusalIsReportedWithTheBoxErrorCode() {
        session("s1");
        expect(HttpMethod.GET, "/fw/redir/", "s1").andRespond(expired());
        session("s2");
        expect(HttpMethod.GET, "/fw/redir/", "s2").andRespond(expired());

        assertThatThrownBy(() -> service.listRules())
                .isInstanceOf(FreeboxException.class)
                .hasMessageContaining("auth_required");
    }

    private void session(String token) {
        box.expect(requestTo(BASE + "/login/")).andExpect(method(HttpMethod.GET))
                .andRespond(ok("{\"logged_in\":false,\"challenge\":\"c\"}"));
        box.expect(requestTo(BASE + "/login/session/")).andExpect(method(HttpMethod.POST))
                .andRespond(ok("{\"session_token\":\"" + token + "\",\"permissions\":{\"settings\":true}}"));
    }

    private ResponseActions expect(HttpMethod httpMethod, String path, String token) {
        return box.expect(requestTo(BASE + path))
                .andExpect(method(httpMethod))
                .andExpect(header("X-Fbx-App-Auth", token));
    }

    private static org.springframework.test.web.client.ResponseCreator ok(String result) {
        return withSuccess("{\"success\":true,\"result\":" + result + "}", MediaType.APPLICATION_JSON);
    }

    private static org.springframework.test.web.client.ResponseCreator expired() {
        return withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body(AUTH_REQUIRED);
    }
}
