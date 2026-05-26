package com.f1.quiket.domain.subject.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.subject.dto.CertificateResponse;
import com.f1.quiket.domain.subject.service.CertificateService;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CertificateControllerTest {

    private CertificateService certificateService;
    private CertificateController certificateController;

    @BeforeEach
    void setUp() {
        certificateService = mock(CertificateService.class);
        certificateController = new CertificateController(certificateService);
    }

    @Test
    void getCertificates_returns_ok_response() {
        List<CertificateResponse> response = List.of(
                CertificateResponse.builder().id(1L).name("정보처리기사").featured(true).displayOrder(1).build()
        );
        when(certificateService.getCertificates(true, "정보")).thenReturn(response);

        ResponseEntity<ApiResponse<List<CertificateResponse>>> result = certificateController.getCertificates(true, "정보");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isSuccess()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo(SuccessCode.OK.getCode());
        assertThat(result.getBody().getData()).isSameAs(response);
        verify(certificateService).getCertificates(true, "정보");
    }
}
