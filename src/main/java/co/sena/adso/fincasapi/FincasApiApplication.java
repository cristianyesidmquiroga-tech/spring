package co.sena.adso.fincasapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// El login va por JWT contra la tabla usuarios, no se necesita el usuario en memoria de Spring
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class FincasApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FincasApiApplication.class, args);
    }
}
