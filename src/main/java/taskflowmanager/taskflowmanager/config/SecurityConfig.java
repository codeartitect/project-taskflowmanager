package taskflowmanager.taskflowmanager.config;

import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import taskflowmanager.taskflowmanager.security.CustomUserDetailsService;
import taskflowmanager.taskflowmanager.security.RestAuthenticationEntryPoint;
import taskflowmanager.taskflowmanager.security.TokenAuthenticationFilter;
import taskflowmanager.taskflowmanager.security.oauth2.CustomOAuth2UserService;
import taskflowmanager.taskflowmanager.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import taskflowmanager.taskflowmanager.security.oauth2.OAuth2AuthenticationFailureHandler;
import taskflowmanager.taskflowmanager.security.oauth2.OAuth2AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true
)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Autowired
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Autowired
    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter();
    }

    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(CsrfConfigurer::disable)
                .formLogin(FormLoginConfigurer::disable)
                .httpBasic(HttpBasicConfigurer::disable)
                .exceptionHandling(authenticationManager -> authenticationManager.authenticationEntryPoint(new RestAuthenticationEntryPoint()))
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers("/",
                                        "/error",
                                        "/favicon.ico",
                                        "/**/*.png",
                                        "/**/*.gif",
                                        "/**/*.svg",
                                        "/**/*.jpg",
                                        "/**/*.html",
                                        "/**/*.css",
                                        "/**/*.js").permitAll()
                                .requestMatchers("/auth/**", "/oauth2/**").permitAll()
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll())
                .oauth2Login(oauth2Login ->
                        oauth2Login.
                                authorizationEndpoint(authorizeEndpoint ->
                                        authorizeEndpoint
                                                .baseUri("/oauth2/authorize")
                                                .authorizationRequestRepository((AuthorizationRequestRepository<OAuth2AuthorizationRequest>) cookieAuthorizationRequestRepository()))
                                .redirectionEndpoint(redirectEndpoint ->
                                        redirectEndpoint.baseUri("/oauth2/callback/*"))
                                .userInfoEndpoint(userEndpoint ->
                                        userEndpoint
                                                .userService((OAuth2UserService<OAuth2UserRequest, OAuth2User>) customOAuth2UserService))
                                .successHandler((AuthenticationSuccessHandler) oAuth2AuthenticationSuccessHandler)
                                .failureHandler((AuthenticationFailureHandler) oAuth2AuthenticationFailureHandler)
                )
                // Add our custom Token based authentication filter
                .addFilterBefore((Filter) tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}
