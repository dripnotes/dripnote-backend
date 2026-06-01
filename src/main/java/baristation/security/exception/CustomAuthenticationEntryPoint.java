package baristation.security.exception;

import baristation.common.exception.CustomException;
import baristation.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

// 토큰 없이 인증이 필요한 요청을 할 경우
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver handlerExceptionResolver;
    // 왜 입장 불가능한지 설명해주는 클래스
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // 토큰 에러(만료, 위조 등)는 이미 JwtFilter에서 처리됨.
        // 이곳에 도달했다는 것은 '인증 정보(토큰) 없이 보호된 자원에 접근함'을 의미함.
        CustomException customException = new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        handlerExceptionResolver.resolveException(request, response, null, customException);
    }
}