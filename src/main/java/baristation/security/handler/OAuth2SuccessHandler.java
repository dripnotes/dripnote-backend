package baristation.security.handler;

import baristation.common.annotation.ExternalApiLog;
import baristation.common.cookie.CookieUtil;
import baristation.common.exception.CustomException;
import baristation.common.exception.ErrorCode;
import baristation.common.redis.RedisService;
import baristation.security.jwt.JwtTokenProvider;
import baristation.security.payload.dto.TokenPair;
import baristation.user.domain.User;
import baristation.user.payload.dto.oauth.GoogleUserInfoDTO;
import baristation.user.payload.dto.oauth.KakaoUserInfoDTO;
import baristation.user.payload.dto.oauth.NaverUserInfoDTO;
import baristation.user.payload.dto.oauth.OAuth2UserInfo;
import baristation.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final CookieUtil cookieUtil;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    @ExternalApiLog("OAuth2 Success Handler")
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        String provider = oauth2Token.getAuthorizedClientRegistrationId();
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        Map<String, Object> attributes = oauth2User.getAttributes();

        OAuth2UserInfo userInfo = null;
        if (provider.equals("google")) {
            userInfo = GoogleUserInfoDTO.from(attributes);
        } else if (provider.equals("naver")) {
            userInfo = NaverUserInfoDTO.from(attributes);
        } else if (provider.equals("kakao")) {
            userInfo = KakaoUserInfoDTO.from(attributes);
        } else {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        User user = userRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        TokenPair tokenPair = jwtTokenProvider.createTokenSet(user);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.createRefreshTokenCookie(tokenPair.refreshToken()).toString());

        redisService.setRefreshToken(String.valueOf(user.getUserId()), tokenPair.refreshToken());

        String targetUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/auth/success")
                .build().toUriString();


        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
