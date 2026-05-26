package com.f1.quiket.domain.subject.service;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.domain.subject.dto.ChapterWithPartsResponse;
import com.f1.quiket.domain.subject.dto.PartSummaryResponse;
import com.f1.quiket.domain.subject.dto.QuizScopeResponse;
import com.f1.quiket.domain.subject.dto.SubjectCreateRequest;
import com.f1.quiket.domain.subject.dto.SubjectDetailResponse;
import com.f1.quiket.domain.subject.dto.SubjectDetailUpdateRequest;
import com.f1.quiket.domain.subject.dto.SubjectExamDetailRequest;
import com.f1.quiket.domain.subject.dto.SubjectExamDetailResponse;
import com.f1.quiket.domain.subject.dto.SubjectExamScheduleResponse;
import com.f1.quiket.domain.subject.dto.SubjectExamScheduleUpsertRequest;
import com.f1.quiket.domain.subject.dto.SubjectOtherDetailRequest;
import com.f1.quiket.domain.subject.dto.SubjectOtherDetailResponse;
import com.f1.quiket.domain.subject.dto.SubjectPageResponse;
import com.f1.quiket.domain.subject.dto.SubjectPurposeDetailResponse;
import com.f1.quiket.domain.subject.dto.SubjectResponse;
import com.f1.quiket.domain.subject.dto.SubjectReviewDetailRequest;
import com.f1.quiket.domain.subject.dto.SubjectReviewDetailResponse;
import com.f1.quiket.domain.subject.dto.SubjectSummaryResponse;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.entity.SubjectExamDetail;
import com.f1.quiket.domain.subject.entity.SubjectExamSchedule;
import com.f1.quiket.domain.subject.entity.SubjectOtherDetail;
import com.f1.quiket.domain.subject.entity.SubjectReviewDetail;
import com.f1.quiket.domain.subject.entity.type.ExamType;
import com.f1.quiket.domain.subject.entity.type.StudyLevel;
import com.f1.quiket.domain.subject.entity.type.SubjectPurpose;
import com.f1.quiket.domain.subject.entity.type.UsagePurpose;
import com.f1.quiket.domain.subject.repository.SubjectExamDetailRepository;
import com.f1.quiket.domain.subject.repository.SubjectExamScheduleRepository;
import com.f1.quiket.domain.subject.repository.SubjectOtherDetailRepository;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.domain.subject.repository.SubjectReviewDetailRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 과목 비즈니스 서비스
 */
@Service
@RequiredArgsConstructor
public class SubjectService {

    private static final int MIN_PAGE = 0;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectExamDetailRepository subjectExamDetailRepository;
    private final SubjectReviewDetailRepository subjectReviewDetailRepository;
    private final SubjectOtherDetailRepository subjectOtherDetailRepository;
    private final SubjectExamScheduleRepository subjectExamScheduleRepository;
    private final ChapterRepository chapterRepository;
    private final PartRepository partRepository;
    private final QuizSessionRepository quizSessionRepository;

    /**
     * 과목 목록 조회
     */
    @Transactional(readOnly = true)
    public SubjectPageResponse getSubjects(String userPublicId, int page, int size) {
        User user = findUser(userPublicId);
        PageRequest pageRequest = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Subject> subjectPage = subjectRepository.findByUserIdAndDeletedAtIsNull(user.getId(), pageRequest);
        List<Subject> subjects = subjectPage.getContent();
        List<Long> subjectIds = subjects.stream().map(Subject::getId).toList();

        if (subjectIds.isEmpty()) {
            return SubjectPageResponse.builder()
                    .page(subjectPage.getNumber())
                    .size(subjectPage.getSize())
                    .totalElements(subjectPage.getTotalElements())
                    .totalPages(subjectPage.getTotalPages())
                    .last(subjectPage.isLast())
                    .content(List.of())
                    .build();
        }

        Map<Long, Long> chapterCounts = chapterRepository.findAllBySubjectIdInAndDeletedAtIsNull(subjectIds).stream()
                .collect(Collectors.groupingBy(Chapter::getSubjectId, Collectors.counting()));
        Map<Long, Long> partCounts = partRepository.findAllBySubjectIdInAndDeletedAtIsNull(subjectIds).stream()
                .collect(Collectors.groupingBy(Part::getSubjectId, Collectors.counting()));
        Map<Long, SubjectExamSchedule> schedules = subjectExamScheduleRepository.findAllBySubjectIdInAndDeletedAtIsNull(subjectIds).stream()
                .collect(Collectors.toMap(SubjectExamSchedule::getSubjectId, Function.identity()));

        List<SubjectSummaryResponse> content = subjects.stream()
                .map(subject -> toSummary(subject, chapterCounts, partCounts, schedules))
                .toList();

        return SubjectPageResponse.builder()
                .page(subjectPage.getNumber())
                .size(subjectPage.getSize())
                .totalElements(subjectPage.getTotalElements())
                .totalPages(subjectPage.getTotalPages())
                .last(subjectPage.isLast())
                .content(content)
                .build();
    }

    /**
     * 과목 생성
     */
    @Transactional
    public SubjectResponse createSubject(String userPublicId, SubjectCreateRequest request) {
        User user = findUser(userPublicId);
        SubjectPurpose purpose = validatePurpose(request);
        Subject subject = subjectRepository.save(Subject.create(user.getId(), request.getName(), purpose.value()));
        saveDetail(subject.getId(), purpose, request);
        return SubjectResponse.of(subject, getPurposeDetail(subject));
    }

    /**
     * 과목 상세 조회
     */
    @Transactional(readOnly = true)
    public SubjectDetailResponse getSubject(String userPublicId, String subjectPublicId) {
        User user = findUser(userPublicId);
        Subject subject = findSubject(subjectPublicId, user.getId());
        return toDetail(subject, user.getId());
    }

    /**
     * 퀴즈 출제 범위 조회
     */
    @Transactional(readOnly = true)
    public QuizScopeResponse getQuizScope(String userPublicId, String subjectPublicId) {
        User user = findUser(userPublicId);
        Subject subject = findSubject(subjectPublicId, user.getId());
        return QuizScopeResponse.builder()
                .subjectId(subject.getPublicId())
                .subjectName(subject.getName())
                .chapters(getChapterWithParts(subject, user.getId()))
                .build();
    }

    /**
     * 과목명 수정
     */
    @Transactional
    public SubjectResponse updateSubjectName(String userPublicId, String subjectPublicId, String name) {
        User user = findUser(userPublicId);
        Subject subject = findSubject(subjectPublicId, user.getId());
        subject.updateName(name);
        return SubjectResponse.of(subject, getPurposeDetail(subject));
    }

    /**
     * 과목 상세 수정
     */
    @Transactional
    public SubjectDetailResponse updateSubjectDetails(String userPublicId, String subjectPublicId, SubjectDetailUpdateRequest request) {
        User user = findUser(userPublicId);
        Subject subject = findSubject(subjectPublicId, user.getId());
        SubjectPurpose purpose = validatePurpose(request);
        subject.updatePurpose(purpose.value());
        replaceDetails(subject.getId(), purpose, request);
        return toDetail(subject, user.getId());
    }

    /**
     * 시험 일정 등록 또는 수정
     */
    @Transactional
    public SubjectExamScheduleResponse upsertExamSchedule(String userPublicId, String subjectPublicId, SubjectExamScheduleUpsertRequest request) {
        User user = findUser(userPublicId);
        Subject subject = findSubject(subjectPublicId, user.getId());
        SubjectExamSchedule schedule = subjectExamScheduleRepository.findBySubjectId(subject.getId())
                .map(existingSchedule -> {
                    existingSchedule.update(normalizeBlank(request.getExamName()), request.getExamDate());
                    return existingSchedule;
                })
                .orElseGet(() -> subjectExamScheduleRepository.save(
                        SubjectExamSchedule.create(subject.getId(), user.getId(), normalizeBlank(request.getExamName()), request.getExamDate())
                ));
        return SubjectExamScheduleResponse.of(schedule, subject);
    }

    /**
     * 시험 일정 삭제
     */
    @Transactional
    public void deleteExamSchedule(String userPublicId, String subjectPublicId) {
        User user = findUser(userPublicId);
        Subject subject = findSubject(subjectPublicId, user.getId());
        SubjectExamSchedule schedule = subjectExamScheduleRepository.findBySubjectIdAndDeletedAtIsNull(subject.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        schedule.delete();
    }

    /**
     * 과목 삭제
     */
    @Transactional
    public void deleteSubject(String userPublicId, String subjectPublicId) {
        User user = findUser(userPublicId);
        Subject subject = findSubject(subjectPublicId, user.getId());

        // 하위 데이터 soft delete
        subjectExamScheduleRepository.findBySubjectIdAndDeletedAtIsNull(subject.getId()).ifPresent(SubjectExamSchedule::delete);
        chapterRepository.findAllBySubjectIdAndUserIdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(subject.getId(), user.getId())
                .forEach(Chapter::delete);
        partRepository.findAllBySubjectIdAndDeletedAtIsNull(subject.getId()).forEach(Part::delete);
        quizSessionRepository.findAllBySubjectIdAndDeletedAtIsNull(subject.getId()).forEach(session -> session.delete());

        subject.delete();
    }

    /**
     * 사용자 조회
     */
    private User findUser(String userPublicId) {
        return userRepository.findByPublicIdAndDeletedAtIsNull(userPublicId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
    }

    /**
     * 소유 과목 조회
     */
    private Subject findSubject(String subjectPublicId, Long userId) {
        return subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subjectPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    /**
     * 과목 상세 응답 변환
     */
    private SubjectDetailResponse toDetail(Subject subject, Long userId) {
        List<ChapterWithPartsResponse> chapterResponses = getChapterWithParts(subject, userId);

        SubjectExamScheduleResponse scheduleResponse = subjectExamScheduleRepository.findBySubjectIdAndDeletedAtIsNull(subject.getId())
                .map(schedule -> SubjectExamScheduleResponse.of(schedule, subject))
                .orElse(null);

        return SubjectDetailResponse.builder()
                .id(subject.getPublicId())
                .name(subject.getName())
                .purpose(subject.getPurpose())
                .detail(getPurposeDetail(subject))
                .createdAt(subject.getCreatedAt())
                .examSchedule(scheduleResponse)
                .chapters(chapterResponses)
                .build();
    }

    /**
     * 챕터와 파트 응답 목록 생성
     */
    private List<ChapterWithPartsResponse> getChapterWithParts(Subject subject, Long userId) {
        List<Chapter> chapters = chapterRepository.findAllBySubjectIdAndUserIdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(
                subject.getId(),
                userId
        );
        if (chapters.isEmpty()) {
            return List.of();
        }
        Map<Long, Chapter> chapterMap = chapters.stream().collect(Collectors.toMap(Chapter::getId, Function.identity()));
        Map<Long, List<Part>> partMap = partRepository.findAllByChapterIdInAndDeletedAtIsNull(chapters.stream().map(Chapter::getId).toList()).stream()
                .sorted(Comparator.comparing(Part::getPartNumber).thenComparing(Part::getCreatedAt))
                .collect(Collectors.groupingBy(Part::getChapterId));

        return chapters.stream()
                .map(chapter -> ChapterWithPartsResponse.of(
                        chapter,
                        subject.getPublicId(),
                        partMap.getOrDefault(chapter.getId(), List.of()).stream()
                                .map(part -> PartSummaryResponse.of(part, chapterMap.get(part.getChapterId())))
                                .toList()
                ))
                .toList();
    }

    /**
     * 과목 요약 응답 변환
     */
    private SubjectSummaryResponse toSummary(
            Subject subject,
            Map<Long, Long> chapterCounts,
            Map<Long, Long> partCounts,
            Map<Long, SubjectExamSchedule> schedules
    ) {
        SubjectExamSchedule schedule = schedules.get(subject.getId());
        return SubjectSummaryResponse.builder()
                .id(subject.getPublicId())
                .name(subject.getName())
                .purpose(subject.getPurpose())
                .chapterCount(Math.toIntExact(chapterCounts.getOrDefault(subject.getId(), 0L)))
                .partCount(Math.toIntExact(partCounts.getOrDefault(subject.getId(), 0L)))
                .lastActivityAt(subject.getUpdatedAt())
                .examSchedule(schedule == null ? null : SubjectExamScheduleResponse.of(schedule, subject))
                .build();
    }

    /**
     * 목적별 상세 조회
     */
    private SubjectPurposeDetailResponse getPurposeDetail(Subject subject) {
        SubjectPurpose purpose = SubjectPurpose.from(subject.getPurpose());
        if (purpose == SubjectPurpose.EXAM) {
            return SubjectPurposeDetailResponse.builder()
                    .examDetail(subjectExamDetailRepository.findBySubjectId(subject.getId())
                            .map(SubjectExamDetailResponse::from)
                            .orElse(null))
                    .build();
        }
        if (purpose == SubjectPurpose.REVIEW) {
            return SubjectPurposeDetailResponse.builder()
                    .reviewDetail(subjectReviewDetailRepository.findBySubjectId(subject.getId())
                            .map(SubjectReviewDetailResponse::from)
                            .orElse(null))
                    .build();
        }
        return SubjectPurposeDetailResponse.builder()
                .otherDetail(subjectOtherDetailRepository.findBySubjectId(subject.getId())
                        .map(SubjectOtherDetailResponse::from)
                        .orElse(null))
                .build();
    }

    /**
     * 목적별 상세 저장
     */
    private void saveDetail(Long subjectId, SubjectPurpose purpose, SubjectCreateRequest request) {
        if (purpose == SubjectPurpose.EXAM) {
            SubjectExamDetailRequest detail = validateExamDetail(request.getExamDetail());
            subjectExamDetailRepository.save(SubjectExamDetail.create(subjectId, detail));
            return;
        }
        if (purpose == SubjectPurpose.REVIEW) {
            SubjectReviewDetailRequest detail = validateReviewDetail(request.getReviewDetail());
            subjectReviewDetailRepository.save(SubjectReviewDetail.create(subjectId, detail));
            return;
        }
        SubjectOtherDetailRequest detail = validateOtherDetail(request.getOtherDetail());
        subjectOtherDetailRepository.save(SubjectOtherDetail.create(subjectId, detail));
    }

    /**
     * 목적별 상세 교체
     */
    private void replaceDetails(Long subjectId, SubjectPurpose purpose, SubjectCreateRequest request) {
        if (purpose == SubjectPurpose.EXAM) {
            SubjectExamDetailRequest detail = validateExamDetail(request.getExamDetail());
            subjectReviewDetailRepository.deleteBySubjectId(subjectId);
            subjectOtherDetailRepository.deleteBySubjectId(subjectId);
            subjectExamDetailRepository.findBySubjectId(subjectId)
                    .ifPresentOrElse(existingDetail -> existingDetail.update(detail),
                            () -> subjectExamDetailRepository.save(SubjectExamDetail.create(subjectId, detail)));
            return;
        }
        if (purpose == SubjectPurpose.REVIEW) {
            SubjectReviewDetailRequest detail = validateReviewDetail(request.getReviewDetail());
            subjectExamDetailRepository.deleteBySubjectId(subjectId);
            subjectOtherDetailRepository.deleteBySubjectId(subjectId);
            subjectReviewDetailRepository.findBySubjectId(subjectId)
                    .ifPresentOrElse(existingDetail -> existingDetail.update(detail),
                            () -> subjectReviewDetailRepository.save(SubjectReviewDetail.create(subjectId, detail)));
            return;
        }
        SubjectOtherDetailRequest detail = validateOtherDetail(request.getOtherDetail());
        subjectExamDetailRepository.deleteBySubjectId(subjectId);
        subjectReviewDetailRepository.deleteBySubjectId(subjectId);
        subjectOtherDetailRepository.findBySubjectId(subjectId)
                .ifPresentOrElse(existingDetail -> existingDetail.update(detail),
                        () -> subjectOtherDetailRepository.save(SubjectOtherDetail.create(subjectId, detail)));
    }

    /**
     * 학습 목적 검증
     */
    private SubjectPurpose validatePurpose(SubjectCreateRequest request) {
        try {
            return SubjectPurpose.from(request.getPurpose());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 학습 목적입니다.");
        }
    }

    /**
     * 시험 상세 검증
     */
    private SubjectExamDetailRequest validateExamDetail(SubjectExamDetailRequest detail) {
        if (detail == null || !StringUtils.hasText(detail.getExamType()) || !ExamType.contains(detail.getExamType())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "시험 상세 정보가 올바르지 않습니다.");
        }
        return detail;
    }

    /**
     * 복습 상세 검증
     */
    private SubjectReviewDetailRequest validateReviewDetail(SubjectReviewDetailRequest detail) {
        if (detail == null || !StudyLevel.contains(detail.getStudyLevel())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "복습 상세 정보가 올바르지 않습니다.");
        }
        return detail;
    }

    /**
     * 기타 상세 검증
     */
    private SubjectOtherDetailRequest validateOtherDetail(SubjectOtherDetailRequest detail) {
        if (detail == null || !UsagePurpose.contains(detail.getUsagePurpose())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "기타 상세 정보가 올바르지 않습니다.");
        }
        return detail;
    }

    /**
     * 페이지 번호 보정
     */
    private int normalizePage(int page) {
        return Math.max(page, MIN_PAGE);
    }

    /**
     * 페이지 크기 보정
     */
    private int normalizeSize(int size) {
        return Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);
    }

    /**
     * 빈 문자열 정규화
     */
    private String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
