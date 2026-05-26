package com.f1.quiket.domain.subject.service;

import com.f1.quiket.domain.subject.dto.CertificateResponse;
import com.f1.quiket.domain.subject.entity.Certificate;
import com.f1.quiket.domain.subject.repository.CertificateRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 자격증 마스터 조회 서비스
 */
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;

    /**
     * 자격증 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CertificateResponse> getCertificates(Boolean featured, String keyword) {
        boolean featuredOnly = Boolean.TRUE.equals(featured);
        List<Certificate> certificates;

        // featured와 keyword 조합별 JPA 기본 메서드 사용
        if (featuredOnly && StringUtils.hasText(keyword)) {
            certificates = certificateRepository.findAllByFeaturedTrueAndNameContainingIgnoreCaseOrderByDisplayOrderAscNameAsc(keyword);
        } else if (featuredOnly) {
            certificates = certificateRepository.findAllByFeaturedTrueOrderByDisplayOrderAscNameAsc();
        } else if (StringUtils.hasText(keyword)) {
            certificates = certificateRepository.findAllByNameContainingIgnoreCaseOrderByDisplayOrderAscNameAsc(keyword);
        } else {
            certificates = certificateRepository.findAllByOrderByDisplayOrderAscNameAsc();
        }

        return certificates.stream()
                .map(CertificateResponse::from)
                .toList();
    }
}
