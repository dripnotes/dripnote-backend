package dripnote.user.payload.dto;

import dripnote.user.enums.UserProvider;
import java.util.Map;

public class NaverUserInfoDTO implements OAuth2UserInfo {
    private Map<String, Object> response; // 네이버 response 데이터

    @SuppressWarnings("unchecked")
    public NaverUserInfoDTO(Map<String, Object> attributes) {
        this.response = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public UserProvider getProvider() {
        return UserProvider.NAVER;
    }

    @Override
    public String getProviderId() {
        return (String) response.get("id");
    }


    @Override
    public String getEmail() {
        return (String) response.get("email");
    }

    @Override
    public String getName() {
        return (String) response.get("name");
    }
}