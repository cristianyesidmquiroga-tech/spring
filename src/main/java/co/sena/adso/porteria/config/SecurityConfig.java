package co.sena.adso.porteria.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFiltro;
    private final LimitePeticionesFilter limitePeticiones;
    private final boolean seguridadActiva;

    public SecurityConfig(JwtAuthenticationFilter jwtFiltro, LimitePeticionesFilter limitePeticiones,
                          @Value("${app.security.enabled:true}") boolean seguridadActiva) {
        this.jwtFiltro = jwtFiltro;
        this.limitePeticiones = limitePeticiones;
        this.seguridadActiva = seguridadActiva;
    }

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .headers(h -> h
                        .contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                        .frameOptions(f -> f.deny()))
                .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(limitePeticiones, JwtAuthenticationFilter.class);

        if (seguridadActiva) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/login", "/api/hello", "/actuator/health").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/avatares/*").permitAll()
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                    // Por ruta y no solo con @PreAuthorize: así un perfil sin permiso no ve errores de validación
                    .requestMatchers("/api/porteria/**").hasAuthority("OPERAR_PORTERIA")
                    .anyRequest().authenticated());
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }
        return http.build();
    }

    // Los filtros son @Component: se apaga su registro automático para que solo corran dentro de Spring Security
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> registroJwt(JwtAuthenticationFilter filtro) {
        FilterRegistrationBean<JwtAuthenticationFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<LimitePeticionesFilter> registroLimite(LimitePeticionesFilter filtro) {
        FilterRegistrationBean<LimitePeticionesFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.origenes:http://localhost:5173}") List<String> origenes) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origenes);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }
}
