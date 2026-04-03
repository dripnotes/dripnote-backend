package dripnote.user.payload.dto;

import dripnote.user.enums.UserProvider;

import java.util.Collections;
import java.util.Map;

public class KakaoUserInfoDTO implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    @SuppressWarnings("unchecked")
    public KakaoUserInfoDTO(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Collections.emptyMap());
        this.profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Collections.emptyMap());
    }

    @Override
    public UserProvider getProvider() {
        return UserProvider.KAKAO;
    }

    @Override
    public String getProviderId() {
        Object id = attributes.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public String getEmail() {
        Object email = kakaoAccount.get("email");
        return email != null ? String.valueOf(email) : null;
    }

    @Override
    public String getName() {
        Object nickname = profile.get("nickname");
        return nickname != null ? String.valueOf(nickname) : "kakao_user";
    }
}