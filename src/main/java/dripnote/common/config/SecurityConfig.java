package dripnote.common.config;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /* [삭제된 부분]
       - .formLogin(...): 이제 아이디/비밀번호 로그인을 쓰지 않으므로 삭제합니다.
       - .rememberMe(...): 이 기능은 UserDetailsService가 필요하여 에러를 일으켰으므로 삭제합니다.
       - /signup: OAuth2 전용이므로 별도의 회원가입 페이지는 필요 없어 제외했습니다.
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
                                "/main",
                                "/signin",  // 로그인 페이지
                                "/beans/**",
                                "/classes/**"
                        ).permitAll()
                        .requestMatchers(
                                "/mypage/**",
                                "/reservations/**"
                        ).authenticated()
                        .anyRequest().authenticated() // 나머지는 로그인한 유저만
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth -> oauth
                        .loginPage("/signin") // 커스텀 로그인 페이지 사용 시
                        .defaultSuccessUrl("/main", true) // 로그인 성공 시 이동할 곳
                )

                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/main")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );
        return http.build();
    }
}