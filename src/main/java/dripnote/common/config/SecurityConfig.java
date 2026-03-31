package dripnote.common.config;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /* [삭제된 부분]
       - .invalidateHttpSession(true): 디폴트값이 true라 불필요할 것 같아서 삭제합니다.
       - .logoutUrl("/logout"): 디폴트값이 "/logout"이라 불필요할 것 같아서 삭제합니다.
       - requestMatchers 내부에 "/main" 삭제, 메인페이지는 "/"으로 사용
       [추가된 부분]
       - requestMatchers 내부에 "/oauth2/**" 추가, 추후에 oauth 요청 시 사용
       [변경된 부분]
       - .logoutSuccessUrl("/main") -> .logoutSuccessUrl("/")
       - .defaultSuccessUrl("/main", true) -> .defaultSuccessUrl("/", true)
    */

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                // 1. CSRF 비활성화 - 개발 단계 및 OAuth2 연동 테스트 임시 설정
                .csrf(csrf -> csrf.disable())

                // 경로별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers(
                                "/",
                                "/signin",
                                "/beans/**",
                                "/classes/**",
                                "/oauth2/**"
                        ).permitAll()
                        .requestMatchers(
                                "/mypage/**"
                        ).authenticated()
                        .anyRequest().authenticated() // 나머지는 로그인한 유저만
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth -> oauth
                        .loginPage("/signin") // 커스텀 로그인 페이지 사용 시
                        .defaultSuccessUrl("/", true) // 로그인 성공 시 이동할 곳
                )

                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .deleteCookies("JSESSIONID")
                );
        return http.build();
    }
}