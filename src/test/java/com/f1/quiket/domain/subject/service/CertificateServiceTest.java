package com.f1.quiket.domain.subject.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.subject.dto.CertificateResponse;
import com.f1.quiket.domain.subject.entity.Certificate;
import com.f1.quiket.domain.subject.repository.CertificateRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CertificateServiceTest {

    private CertificateRepository certificateRepository;
    private CertificateService certificateService;

    @BeforeEach
    void setUp() {
        certificateRepository = mock(CertificateRepository.class);
        certificateService = new CertificateService(certificateRepository);
    }

    @Test
    void getCertificates_uses_featured_and_keyword_query() {
        Certificate certificate = certificate(1L, "정보처리기사", true, 1);
        when(certificateRepository.findAllByFeaturedTrueAndNameContainingIgnoreCaseOrderByDisplayOrderAscNameAsc("정보"))
                .thenReturn(List.of(certificate));

        List<CertificateResponse> response = certificateService.getCertificates(true, "정보");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getId()).isEqualTo(1L);
        assertThat(response.get(0).getName()).isEqualTo("정보처리기사");
        assertThat(response.get(0).getFeatured()).isTrue();
        verify(certificateRepository).findAllByFeaturedTrueAndNameContainingIgnoreCaseOrderByDisplayOrderAscNameAsc("정보");
        verifyNoMoreInteractions(certificateRepository);
    }

    @Test
    void getCertificates_uses_featured_only_query() {
        when(certificateRepository.findAllByFeaturedTrueOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(certificate(2L, "SQLD", true, 2)));

        List<CertificateResponse> response = certificateService.getCertificates(true, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getName()).isEqualTo("SQLD");
        verify(certificateRepository).findAllByFeaturedTrueOrderByDisplayOrderAscNameAsc();
        verifyNoMoreInteractions(certificateRepository);
    }

    @Test
    void getCertificates_uses_keyword_only_query() {
        when(certificateRepository.findAllByNameContainingIgnoreCaseOrderByDisplayOrderAscNameAsc("SQL"))
                .thenReturn(List.of(certificate(3L, "SQLD", false, 3)));

        List<CertificateResponse> response = certificateService.getCertificates(false, "SQL");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getFeatured()).isFalse();
        verify(certificateRepository).findAllByNameContainingIgnoreCaseOrderByDisplayOrderAscNameAsc("SQL");
        verifyNoMoreInteractions(certificateRepository);
    }

    @Test
    void getCertificates_uses_all_query_when_no_filter() {
        when(certificateRepository.findAllByOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(certificate(4L, "네트워크관리사", false, 4)));

        List<CertificateResponse> response = certificateService.getCertificates(null, " ");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getDisplayOrder()).isEqualTo(4);
        verify(certificateRepository).findAllByOrderByDisplayOrderAscNameAsc();
        verifyNoMoreInteractions(certificateRepository);
    }

    private Certificate certificate(Long id, String name, Boolean featured, Integer displayOrder) {
        Certificate certificate = org.springframework.beans.BeanUtils.instantiateClass(Certificate.class);
        ReflectionTestUtils.setField(certificate, "id", id);
        ReflectionTestUtils.setField(certificate, "name", name);
        ReflectionTestUtils.setField(certificate, "featured", featured);
        ReflectionTestUtils.setField(certificate, "displayOrder", displayOrder);
        return certificate;
    }
}
