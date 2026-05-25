package com.f1.quiket.domain.subject.repository;

import com.f1.quiket.domain.subject.entity.Certificate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 자격증 마스터 리포지토리
 */
@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    /**
     * 전체 자격증 목록 조회
     */
    List<Certificate> findAllByOrderByDisplayOrderAscNameAsc();

    /**
     * 자주 찾는 자격증 목록 조회
     */
    List<Certificate> findAllByFeaturedTrueOrderByDisplayOrderAscNameAsc();

    /**
     * 자격증명 검색
     */
    List<Certificate> findAllByNameContainingIgnoreCaseOrderByDisplayOrderAscNameAsc(String keyword);

    /**
     * 자주 찾는 자격증명 검색
     */
    List<Certificate> findAllByFeaturedTrueAndNameContainingIgnoreCaseOrderByDisplayOrderAscNameAsc(String keyword);
}
