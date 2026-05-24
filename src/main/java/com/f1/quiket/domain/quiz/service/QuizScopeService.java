package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.quiz.dto.QuizScopeResponse;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 퀴즈 출제 범위 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizScopeService {

    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;
    private final PartRepository partRepository;

    /**
     * 퀴즈 출제 범위 조회
     */
    public QuizScopeResponse getQuizScope(Long userId, String subjectPublicId) {
        Subject subject = findSubject(userId, subjectPublicId);
        List<Chapter> chapters = chapterRepository
                .findAllBySubjectIdAndUserIdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(
                        subject.getId(),
                        userId
                );
        Map<Long, List<Part>> partsByChapterId = findPartsByChapterId(userId, subject.getId());

        return QuizScopeResponse.of(subject, chapters, partsByChapterId);
    }

    private Subject findSubject(Long userId, String subjectPublicId) {
        return subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subjectPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));
    }

    // TODO: Part.content(@Lob, 최대 30,000자) 전량 로드 회피 — JPQL projection으로 미리보기 컬럼만 조회 필요
    private Map<Long, List<Part>> findPartsByChapterId(Long userId, Long subjectId) {
        return partRepository
                .findAllBySubjectIdAndUserIdAndDeletedAtIsNullOrderByChapterIdAscPartNumberAscCreatedAtAsc(
                        subjectId,
                        userId
                )
                .stream()
                .collect(Collectors.groupingBy(Part::getChapterId));
    }
}
