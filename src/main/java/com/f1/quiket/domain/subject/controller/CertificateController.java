package com.f1.quiket.domain.subject.controller;

import com.f1.quiket.domain.subject.dto.CertificateResponse;
import com.f1.quiket.domain.subject.service.CertificateService;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자격증 마스터 API 진입점
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/certificates")
public class CertificateController {

    private final CertificateService certificateService;

    /**
     * 자격증 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CertificateResponse>>> getCertificates(
            @RequestParam(name = "featured", required = false) Boolean featured,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        List<CertificateResponse> response = certificateService.getCertificates(featured, keyword);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }
}
