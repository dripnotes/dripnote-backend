package baristation.home.controller;

import baristation.common.payload.response.ApiResponse;
import baristation.home.payload.response.HomeResponse;
import baristation.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/api/main")
    public ResponseEntity<ApiResponse<HomeResponse>> getMain() {
        HomeResponse response = homeService.getHome();
        return ApiResponse.ok(response);
    }
}
