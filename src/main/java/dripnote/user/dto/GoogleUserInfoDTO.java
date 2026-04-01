package dripnote.user.dto;

import dripnote.user.enums.UserProvider;

import java.util.Map;

// 구글 구현체 DTO
public class GoogleUserInfoDTO implements OAuth2UserInfo {
    private Map<String, Object> attributes; // 구글에서 넘어온 JSON 데이터

    public GoogleUserInfoDTO(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public UserProvider getProvider() {
        return UserProvider.GOOGLE;
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }
}