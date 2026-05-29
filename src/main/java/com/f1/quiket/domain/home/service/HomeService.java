package com.f1.quiket.domain.home.service;

import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.home.dto.HomeDataResponse;
import com.f1.quiket.domain.home.dto.HomeHeroResponse;
import com.f1.quiket.domain.home.dto.HomeUserSummaryResponse;
import com.f1.quiket.domain.home.dto.RecentActivityPageResponse;
import com.f1.quiket.domain.home.dto.RecentActivityResponse;
import com.f1.quiket.domain.home.dto.RecentActivityType;
import com.f1.quiket.domain.home.dto.SubjectExamScheduleResponse;
import com.f1.quiket.domain.home.dto.SubjectSummaryResponse;
import com.f1.quiket.domain.home.repository.SubjectCountProjection;
import com.f1.quiket.domain.home.repository.SubjectLastActivityProjection;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import com.f1.quiket.domain.quiz.entity.QuizResult;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.repository.QuizPlaySessionRepository;
import com.f1.quiket.domain.quiz.repository.QuizResultRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.entity.SubjectExamSchedule;
import com.f1.quiket.domain.subject.repository.SubjectExamScheduleRepository;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 화면 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private static final int HOME_SUBJECT_SIZE = 10;
    private static final int HOME_D_DAY_SIZE = 1;
    private static final int HOME_RECENT_ACTIVITY_SIZE = 5;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private static final String QUIZ_SESSION_COMPLETED = "completed";
    private static final String PLAY_SESSION_IN_PROGRESS = "in_progress";

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectExamScheduleRepository subjectExamScheduleRepository;
    private final ChapterRepository chapterRepository;
    private final PartRepository partRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizPlaySessionRepository quizPlaySessionRepository;
    private final QuizResultRepository quizResultRepository;

    /**
     * 홈 메인 데이터 조회
     */
    public HomeDataResponse getHome(String publicId) {
        User user = findUser(publicId);
        HomeSubjectReadModel subjectReadModel = loadHomeSubjectReadModel(user.getId());
        RecentActivityReadModel activityReadModel = loadRecentActivityReadModel(user.getId(), 0, HOME_RECENT_ACTIVITY_SIZE);
        List<RecentActivityResponse> allRecentActivities = buildRecentActivities(activityReadModel);
        List<RecentActivityResponse> recentActivities = sliceRecentActivities(
                allRecentActivities,
                0,
                HOME_RECENT_ACTIVITY_SIZE
        );

        return HomeDataResponse.builder()
                .user(HomeUserSummaryResponse.from(user))
                .hero(HomeHeroResponse.from(findActiveQuiz(allRecentActivities)))
                .dDayCards(findDDayScheduleResponses(user.getId()))
                .subjects(findSubjectSummaryResponses(subjectReadModel))
                .recentActivities(recentActivities)
                .build();
    }

    /**
     * 최근활동 페이지 조회
     */
    public RecentActivityPageResponse getRecentActivities(String publicId, int page, int size) {
        User user = findUser(publicId);
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = normalizeSize(size);
        RecentActivityReadModel readModel = loadRecentActivityReadModel(user.getId(), normalizedPage, normalizedSize);
        List<RecentActivityResponse> activities = buildRecentActivities(readModel);
        List<RecentActivityResponse> content = sliceRecentActivities(activities, normalizedPage, normalizedSize);

        return RecentActivityPageResponse.of(content, normalizedPage, normalizedSize, readModel.totalElements());
    }

    /**
     * 사용자 조회
     */
    private User findUser(String publicId) {
        return userRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
    }

    /**
     * 홈 과목 조회 모델 로딩
     */
    private HomeSubjectReadModel loadHomeSubjectReadModel(Long userId) {
        List<Subject> subjects = subjectRepository.findHomeSummarySubjects(userId, HOME_SUBJECT_SIZE);
        List<Long> subjectIds = subjects.stream()
                .map(Subject::getId)
                .toList();
        List<SubjectExamSchedule> schedules = loadSubjectSchedules(userId, subjects);

        return new HomeSubjectReadModel(
                userId,
                subjects,
                schedules,
                loadChapterCountMap(userId, subjectIds),
                loadPartCountMap(userId, subjectIds),
                loadLastActivityAtMap(userId, subjectIds)
        );
    }

    /**
     * 최근활동 조회 모델 로딩
     */
    private RecentActivityReadModel loadRecentActivityReadModel(Long userId, int page, int size) {
        int fetchSize = calculateActivityFetchSize(page, size);
        PageRequest activityLimit = PageRequest.of(0, fetchSize);

        List<QuizSession> readyQuizSessions = quizSessionRepository.findReadyActivities(
                userId,
                QUIZ_SESSION_COMPLETED,
                activityLimit
        );
        List<QuizPlaySession> inProgressPlaySessions =
                quizPlaySessionRepository.findByUserIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(
                        userId,
                        PLAY_SESSION_IN_PROGRESS,
                        activityLimit
                );
        List<QuizResult> completedQuizResults =
                quizResultRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, activityLimit);

        long totalElements = quizSessionRepository.countReadyActivities(userId, QUIZ_SESSION_COMPLETED)
                + quizPlaySessionRepository.countByUserIdAndStatusAndDeletedAtIsNull(userId, PLAY_SESSION_IN_PROGRESS)
                + quizResultRepository.countByUserIdAndDeletedAtIsNull(userId);

        return new RecentActivityReadModel(
                readyQuizSessions,
                inProgressPlaySessions,
                completedQuizResults,
                loadSubjectMap(userId, readyQuizSessions, inProgressPlaySessions, completedQuizResults),
                loadQuizSessionMap(userId, readyQuizSessions, inProgressPlaySessions, completedQuizResults),
                loadPlaySessionMap(userId, inProgressPlaySessions, completedQuizResults),
                totalElements
        );
    }

    /**
     * 과목 시험 일정 로딩
     */
    private List<SubjectExamSchedule> loadSubjectSchedules(Long userId, List<Subject> subjects) {
        List<Long> subjectIds = subjects.stream()
                .map(Subject::getId)
                .toList();
        if (subjectIds.isEmpty()) {
            return List.of();
        }
        return subjectExamScheduleRepository.findAllBySubjectIdInAndDeletedAtIsNull(subjectIds).stream()
                // 사용자 소유 일정만 사용
                .filter(schedule -> userId.equals(schedule.getUserId()))
                .toList();
    }

    /**
     * 최근활동 소스별 조회 크기 산출
     */
    private int calculateActivityFetchSize(int page, int size) {
        long requestedSize = ((long) page + 1) * size;
        return (int) Math.min(requestedSize, Integer.MAX_VALUE);
    }

    /**
     * 과목별 챕터 개수 로딩
     */
    private Map<Long, Long> loadChapterCountMap(Long userId, List<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        return toCountMap(chapterRepository.countBySubjectIds(userId, subjectIds));
    }

    /**
     * 과목별 파트 개수 로딩
     */
    private Map<Long, Long> loadPartCountMap(Long userId, List<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        return toCountMap(partRepository.countBySubjectIds(userId, subjectIds));
    }

    /**
     * 과목별 개수 맵 변환
     */
    private Map<Long, Long> toCountMap(List<SubjectCountProjection> values) {
        return values.stream()
                .collect(Collectors.toMap(SubjectCountProjection::getSubjectId, SubjectCountProjection::getItemCount));
    }

    /**
     * 과목별 마지막 활동 시각 로딩
     */
    private Map<Long, LocalDateTime> loadLastActivityAtMap(Long userId, List<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, LocalDateTime> lastActivityMap = new HashMap<>();
        mergeLastActivityAt(lastActivityMap, quizSessionRepository.findLastActivityBySubjectIds(userId, subjectIds));
        mergeLastActivityAt(lastActivityMap, quizPlaySessionRepository.findLastActivityBySubjectIds(userId, subjectIds));
        mergeLastActivityAt(lastActivityMap, quizResultRepository.findLastActivityBySubjectIds(userId, subjectIds));
        return lastActivityMap;
    }

    /**
     * 마지막 활동 시각 병합
     */
    private void mergeLastActivityAt(
            Map<Long, LocalDateTime> target,
            List<SubjectLastActivityProjection> values
    ) {
        values.forEach(value -> {
            LocalDateTime lastActivityAt = value.getLastActivityAt();
            if (lastActivityAt == null) {
                return;
            }
            target.merge(value.getSubjectId(), lastActivityAt, (previous, current) ->
                    previous.isAfter(current) ? previous : current
            );
        });
    }

    /**
     * 최근활동 과목 맵 로딩
     */
    private Map<Long, Subject> loadSubjectMap(
            Long userId,
            List<QuizSession> readyQuizSessions,
            List<QuizPlaySession> inProgressPlaySessions,
            List<QuizResult> completedQuizResults
    ) {
        Set<Long> subjectIds = new HashSet<>();
        readyQuizSessions.forEach(session -> subjectIds.add(session.getSubjectId()));
        inProgressPlaySessions.forEach(session -> subjectIds.add(session.getSubjectId()));
        completedQuizResults.forEach(result -> subjectIds.add(result.getSubjectId()));

        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        return subjectRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(subjectIds, userId).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity()));
    }

    /**
     * 최근활동 퀴즈 세션 맵 로딩
     */
    private Map<Long, QuizSession> loadQuizSessionMap(
            Long userId,
            List<QuizSession> readyQuizSessions,
            List<QuizPlaySession> inProgressPlaySessions,
            List<QuizResult> completedQuizResults
    ) {
        Map<Long, QuizSession> quizSessionMap = readyQuizSessions.stream()
                .collect(Collectors.toMap(QuizSession::getId, Function.identity()));
        Set<Long> quizSessionIds = new HashSet<>();
        inProgressPlaySessions.forEach(session -> quizSessionIds.add(session.getQuizSessionId()));
        completedQuizResults.forEach(result -> quizSessionIds.add(result.getQuizSessionId()));
        quizSessionIds.removeAll(quizSessionMap.keySet());

        if (quizSessionIds.isEmpty()) {
            return quizSessionMap;
        }
        quizSessionRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(quizSessionIds, userId)
                .forEach(session -> quizSessionMap.put(session.getId(), session));
        return quizSessionMap;
    }

    /**
     * 최근활동 풀이 세션 맵 로딩
     */
    private Map<Long, QuizPlaySession> loadPlaySessionMap(
            Long userId,
            List<QuizPlaySession> inProgressPlaySessions,
            List<QuizResult> completedQuizResults
    ) {
        Set<Long> playSessionIds = completedQuizResults.stream()
                .map(QuizResult::getPlaySessionId)
                .collect(Collectors.toSet());

        List<QuizPlaySession> playSessions = new ArrayList<>(inProgressPlaySessions);
        if (!playSessionIds.isEmpty()) {
            playSessions.addAll(quizPlaySessionRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(playSessionIds, userId));
        }
        return playSessions.stream()
                .collect(Collectors.toMap(QuizPlaySession::getId, Function.identity(), (left, right) -> left));
    }

    /**
     * 페이지 크기 보정
     */
    private int normalizeSize(int size) {
        // 기본 페이지 크기 보정
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * 최근활동 목록 자르기
     */
    private List<RecentActivityResponse> sliceRecentActivities(List<RecentActivityResponse> activities, int page, int size) {
        int fromIndex = Math.min(page * size, activities.size());
        int toIndex = Math.min(fromIndex + size, activities.size());
        return activities.subList(fromIndex, toIndex);
    }

    /**
     * 최근활동 응답 목록 생성
     */
    private List<RecentActivityResponse> buildRecentActivities(RecentActivityReadModel readModel) {
        List<RecentActivityResponse> activities = new ArrayList<>();
        Map<Long, Subject> subjectMap = readModel.subjectMap();
        Map<Long, QuizSession> quizSessionMap = readModel.quizSessionMap();
        Map<Long, QuizPlaySession> playSessionMap = readModel.playSessionMap();

        activities.addAll(buildReadyQuizActivities(readModel.readyQuizSessions(), subjectMap));
        activities.addAll(buildInProgressQuizActivities(readModel.inProgressPlaySessions(), quizSessionMap, subjectMap));
        activities.addAll(buildCompletedQuizActivities(readModel.completedQuizResults(), quizSessionMap, playSessionMap, subjectMap));

        return activities.stream()
                .sorted(Comparator.comparing(RecentActivityResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * 생성 후 미시작 퀴즈 활동 생성
     */
    private List<RecentActivityResponse> buildReadyQuizActivities(
            List<QuizSession> quizSessions,
            Map<Long, Subject> subjectMap
    ) {
        return quizSessions.stream()
                .map(session -> toReadyQuizActivity(session, subjectMap.get(session.getSubjectId())))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * 진행 중 퀴즈 활동 생성
     */
    private List<RecentActivityResponse> buildInProgressQuizActivities(
            List<QuizPlaySession> playSessions,
            Map<Long, QuizSession> quizSessionMap,
            Map<Long, Subject> subjectMap
    ) {
        return playSessions.stream()
                // 제출 전 풀이 세션만 노출
                .filter(playSession -> PLAY_SESSION_IN_PROGRESS.equals(playSession.getStatus()))
                .map(playSession -> toInProgressQuizActivity(
                        playSession,
                        quizSessionMap.get(playSession.getQuizSessionId()),
                        subjectMap.get(playSession.getSubjectId())
                ))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * 완료 퀴즈 활동 생성
     */
    private List<RecentActivityResponse> buildCompletedQuizActivities(
            List<QuizResult> quizResults,
            Map<Long, QuizSession> quizSessionMap,
            Map<Long, QuizPlaySession> playSessionMap,
            Map<Long, Subject> subjectMap
    ) {
        return quizResults.stream()
                .map(result -> toCompletedQuizActivity(
                        result,
                        quizSessionMap.get(result.getQuizSessionId()),
                        playSessionMap.get(result.getPlaySessionId()),
                        subjectMap.get(result.getSubjectId())
                ))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * D-Day 응답 목록 조회
     */
    private List<SubjectExamScheduleResponse> findDDayScheduleResponses(Long userId) {
        List<SubjectExamSchedule> schedules = subjectExamScheduleRepository
                .findByUserIdAndDeletedAtIsNullAndExamDateGreaterThanEqualOrderByExamDateAscCreatedAtDesc(
                        userId,
                        LocalDate.now(),
                        PageRequest.of(0, HOME_D_DAY_SIZE)
                );
        Map<Long, Subject> subjectMap = loadSubjectMapByIds(
                userId,
                schedules.stream()
                        .map(SubjectExamSchedule::getSubjectId)
                        .collect(Collectors.toSet())
        );
        return schedules.stream()
                .map(schedule -> toSubjectExamScheduleResponse(schedule, subjectMap.get(schedule.getSubjectId())))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * 과목 요약 응답 목록 조회
     */
    private List<SubjectSummaryResponse> findSubjectSummaryResponses(HomeSubjectReadModel readModel) {
        Map<Long, SubjectExamSchedule> scheduleMap = readModel.scheduleMap();

        return readModel.subjects().stream()
                .map(subject -> toSubjectSummaryResponse(
                        subject,
                        readModel.chapterCountMap().getOrDefault(subject.getId(), 0L),
                        readModel.partCountMap().getOrDefault(subject.getId(), 0L),
                        readModel.lastActivityMap().get(subject.getId()),
                        scheduleMap.get(subject.getId())
                ))
                .toList();
    }

    /**
     * 과목 맵 로딩
     */
    private Map<Long, Subject> loadSubjectMapByIds(Long userId, Set<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        return subjectRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(subjectIds, userId).stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity()));
    }

    /**
     * 활성 퀴즈 선택
     */
    private RecentActivityResponse findActiveQuiz(List<RecentActivityResponse> recentActivities) {
        return recentActivities.stream()
                // 최근 5개 밖 활성 퀴즈 누락 방지를 위한 전체 활동 기준 판단
                .filter(activity -> RecentActivityType.QUIZ_IN_PROGRESS.equals(activity.getActivityType())
                        || RecentActivityType.QUIZ_READY.equals(activity.getActivityType()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 미시작 퀴즈 응답 변환
     */
    private Optional<RecentActivityResponse> toReadyQuizActivity(QuizSession quizSession, Subject subject) {
        if (subject == null) {
            return Optional.empty();
        }
        return Optional.of(RecentActivityResponse.builder()
                .activityId(quizSession.getPublicId())
                .activityType(RecentActivityType.QUIZ_READY)
                .quizSessionId(quizSession.getPublicId())
                .title(buildQuizTitle(subject.getName(), quizSession.getQuizType(), quizSession.getQuestionCount()))
                .subjectId(subject.getPublicId())
                .subjectName(subject.getName())
                .status(quizSession.getStatus())
                .progressPct(100)
                .createdAt(Optional.ofNullable(quizSession.getCompletedAt()).orElse(quizSession.getUpdatedAt()))
                .build());
    }

    /**
     * 진행 중 퀴즈 응답 변환
     */
    private Optional<RecentActivityResponse> toInProgressQuizActivity(
            QuizPlaySession playSession,
            QuizSession quizSession,
            Subject subject
    ) {
        if (quizSession == null || subject == null) {
            return Optional.empty();
        }
        return Optional.of(RecentActivityResponse.builder()
                .activityId(playSession.getClientSessionId())
                .activityType(RecentActivityType.QUIZ_IN_PROGRESS)
                .quizSessionId(quizSession.getPublicId())
                .playSessionId(playSession.getClientSessionId())
                .title(buildQuizTitle(subject.getName(), quizSession.getQuizType(), quizSession.getQuestionCount()))
                .subjectId(subject.getPublicId())
                .subjectName(subject.getName())
                .status(playSession.getStatus())
                .progressPct(calculateProgressPct(playSession, quizSession))
                .createdAt(playSession.getUpdatedAt())
                .build());
    }

    /**
     * 완료 퀴즈 응답 변환
     */
    private Optional<RecentActivityResponse> toCompletedQuizActivity(
            QuizResult result,
            QuizSession quizSession,
            QuizPlaySession playSession,
            Subject subject
    ) {
        if (quizSession == null || playSession == null || subject == null) {
            return Optional.empty();
        }
        return Optional.of(RecentActivityResponse.builder()
                .activityId(playSession.getClientSessionId())
                .activityType(RecentActivityType.QUIZ_COMPLETED)
                .quizSessionId(quizSession.getPublicId())
                .playSessionId(playSession.getClientSessionId())
                .resultId(result.getPublicId())
                .title(buildQuizTitle(subject.getName(), quizSession.getQuizType(), result.getTotalCount()))
                .subjectId(subject.getPublicId())
                .subjectName(subject.getName())
                .status(QUIZ_SESSION_COMPLETED)
                .progressPct(100)
                .scoreText(result.getCorrectCount() + "/" + result.getTotalCount())
                .createdAt(result.getCreatedAt())
                .build());
    }

    /**
     * 진행률 산출
     */
    private Integer calculateProgressPct(QuizPlaySession playSession, QuizSession quizSession) {
        // 문제 수 없음 방어
        if (quizSession.getQuestionCount() == null || quizSession.getQuestionCount() == 0) {
            return 0;
        }
        return Math.min(100, (playSession.getLastQuestionIndex() * 100) / quizSession.getQuestionCount());
    }

    /**
     * 퀴즈 제목 생성
     */
    private String buildQuizTitle(String subjectName, String quizType, Integer questionCount) {
        return subjectName + " " + toQuizTypeLabel(quizType) + " " + questionCount + "문제";
    }

    /**
     * 퀴즈 유형 라벨 변환
     */
    private String toQuizTypeLabel(String quizType) {
        return switch (quizType) {
            case "multiple_choice" -> "객관식";
            case "ox" -> "OX";
            default -> quizType;
        };
    }

    /**
     * 과목 요약 응답 변환
     */
    private SubjectSummaryResponse toSubjectSummaryResponse(
            Subject subject,
            Long chapterCount,
            Long partCount,
            LocalDateTime lastActivityAt,
            SubjectExamSchedule schedule
    ) {
        return SubjectSummaryResponse.builder()
                .id(subject.getPublicId())
                .name(subject.getName())
                .purpose(subject.getPurpose())
                .chapterCount(Math.toIntExact(chapterCount))
                .partCount(Math.toIntExact(partCount))
                .lastActivityAt(lastActivityAt)
                .examSchedule(toSubjectExamScheduleResponse(schedule, subject).orElse(null))
                .build();
    }

    /**
     * 시험 일정 응답 변환
     */
    private Optional<SubjectExamScheduleResponse> toSubjectExamScheduleResponse(
            SubjectExamSchedule schedule,
            Subject subject
    ) {
        if (schedule == null || subject == null) {
            return Optional.empty();
        }
        return Optional.of(SubjectExamScheduleResponse.of(
                schedule.getPublicId(),
                subject.getPublicId(),
                subject.getName(),
                schedule.getExamName(),
                schedule.getExamDate()
        ));
    }

    /**
     * 홈 과목 조합용 조회 모델
     */
    private record HomeSubjectReadModel(
            Long userId,
            List<Subject> subjects,
            List<SubjectExamSchedule> schedules,
            Map<Long, Long> chapterCountMap,
            Map<Long, Long> partCountMap,
            Map<Long, LocalDateTime> lastActivityMap
    ) {

        /**
         * 일정 맵 생성
         */
        private Map<Long, SubjectExamSchedule> scheduleMap() {
            return schedules.stream()
                    .collect(Collectors.toMap(SubjectExamSchedule::getSubjectId, Function.identity()));
        }
    }

    /**
     * 최근활동 조합용 조회 모델
     */
    private record RecentActivityReadModel(
            List<QuizSession> readyQuizSessions,
            List<QuizPlaySession> inProgressPlaySessions,
            List<QuizResult> completedQuizResults,
            Map<Long, Subject> subjectMap,
            Map<Long, QuizSession> quizSessionMap,
            Map<Long, QuizPlaySession> playSessionMap,
            long totalElements
    ) {
    }
}
