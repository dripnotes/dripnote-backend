package dripnote.common.config;

import dripnote.user.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /* [추가된 부분]
       - requestMatchers 내부에 swagger 경로 추가하였습니다.
       [변경된 부분]
       - .defaultSuccessUrl("/", true) -> .defaultSuccessUrl("/", false), 로그인 시 사용자가 원래 보던 페이지로 넘어가도록 수정하였습니다.
    */
    private final CustomOAuth2UserService customOAuth2UserService;
    // private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                // 1. CSRF 비활성화 - 개발 단계 및 OAuth2 연동 테스트 임시 설정
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 적용

                // 2. 세션 설정 (JWT 사용 시 보통 STATELESS로 설정하지만, 우선 기본 유지 가능)
                // .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 경로별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers(
                                "/",
                                "/signin",
                                "/beans/**",
                                "/classes/**",
                                "/oauth2/**",
                                "/login/**",   // 구글 리디렉션 도착 경로 허용
                                // swagger 경로
                                "/swagger-custom-ui.html",
                                "/swagger-ui/**",
                                "/api-docs",
                                "/api-docs/**"


                        ).permitAll()
                        .requestMatchers(
                                "/mypage/**"
                        ).authenticated()
                        .anyRequest().authenticated() // 나머지는 로그인한 유저만
                )
                // OAuth2 로그인 설정
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        // 리액트로 리다이렉트 시키기 위해 SuccessHandler를 연결
                        // .successHandler(oAuth2AuthenticationSuccessHandler)
                        // 핸들러 만들기 전까지는 아래처럼 주소만 적어도 되지만, 리액트 포트(3000)를 명시해야 합니다.
                        .defaultSuccessUrl("http://localhost:3000/oauth2/redirect", true)
                )

                // 5. 로그아웃 설정
                .logout(logout -> logout
                        .logoutSuccessUrl("http://localhost:3000/") // 로그아웃 후 리액트 메인으로
                        .deleteCookies("JSESSIONID")
                );
        return http.build();
    }

    // CORS 설정 Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000")); // 리액트 주소 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true); // 쿠키/인증 정보 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}