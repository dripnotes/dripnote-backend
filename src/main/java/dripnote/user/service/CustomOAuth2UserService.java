package dripnote.user.service;

import dripnote.user.domain.User;
import dripnote.user.payload.dto.GoogleUserInfoDTO;
import dripnote.user.payload.dto.KakaoUserInfoDTO;
import dripnote.user.payload.dto.NaverUserInfoDTO;
import dripnote.user.payload.dto.OAuth2UserInfo;
import dripnote.user.enums.UserRole;
import dripnote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
//    private final RestTemplate restTemplate;


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuth2UserInfo oAuth2UserInfo = null;
        if (registrationId.equals("google")) {
            System.out.println("구글 로그인 요청");
            oAuth2UserInfo = new GoogleUserInfoDTO(attributes);
        } else if (registrationId.equals("naver")) {
            System.out.println("네이버 로그인 요청");
            oAuth2UserInfo = new NaverUserInfoDTO(attributes);
        } else if (registrationId.equals("kakao")) {
            System.out.println("카카오 로그인 요청");
             oAuth2UserInfo = new KakaoUserInfoDTO(attributes);
        }
        saveOrUpdateUser(oAuth2UserInfo);

        // 5. 시큐리티 세션에 담기 위해 원본 객체를 그대로 반환합니다.
        // (이 반환값이 나중에 우리가 만들 SuccessHandler로 고스란히 전달됩니다.)

        return oAuth2User;
    }

    // Id를 통해 조회하도록 수정하였습니다.
    private void saveOrUpdateUser(OAuth2UserInfo userInfo) {
        if (userInfo == null) return;

        Optional<User> userOptional = userRepository.findByProviderAndProviderId(
                userInfo.getProvider(),
                userInfo.getProviderId()
        );

        if (userOptional.isEmpty()) {
            User newUser = User.builder()
                    .email(userInfo.getEmail())
                    .provider(userInfo.getProvider())
                    .providerId(userInfo.getProviderId())
                    .nickname(userInfo.getName())
                    .role(UserRole.USER)
                    .build();

            userRepository.save(newUser);
            System.out.println("신규 소셜 유저 회원가입 완료! " + userInfo.getName());
        }
}
}
