package dripnote.user.payload.dto;

import dripnote.user.enums.UserProvider;

public interface OAuth2UserInfo {
    UserProvider getProvider();    // "google", "naver", "kakao"
    String getProviderId();  // "10293847...", "abcde123..."
    String getEmail();
    String getName();
}