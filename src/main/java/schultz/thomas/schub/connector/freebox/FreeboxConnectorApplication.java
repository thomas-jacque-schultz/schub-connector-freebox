package schultz.thomas.schub.connector.freebox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Connecteur Freebox : redirections de ports du routeur.
 */
@SpringBootApplication
public class FreeboxConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreeboxConnectorApplication.class, args);
    }
}
