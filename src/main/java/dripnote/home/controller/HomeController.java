package dripnote.home.controller;

import dripnote.common.response.ApiResponse;
import dripnote.home.payload.response.HomeResponse;
import dripnote.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController {
    private final HomeService homeService;

    // 메인페이지에 원두, 향미 정보 전송
    @GetMapping("/api/main")
    public ApiResponse<HomeResponse> getHome() {
        return ApiResponse.ok(homeService.getHome());
    }
}
