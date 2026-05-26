package com.f1.quiket.domain.chapter.service;

import com.f1.quiket.domain.chapter.dto.ChapterResponse;
import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.chapter.repository.ChapterRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 챕터 비즈니스 서비스
 */
@Service
@RequiredArgsConstructor
public class ChapterService {

    private final UserRepository userRepository;
    private final ChapterRepository chapterRepository;
    private final SubjectRepository subjectRepository;

    /**
     * 챕터명 수정
     */
    @Transactional
    public ChapterResponse updateChapterName(String userPublicId, String chapterPublicId, String name) {
        User user = userRepository.findByPublicIdAndDeletedAtIsNull(userPublicId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
        Chapter chapter = chapterRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(chapterPublicId, user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Subject subject = subjectRepository.findById(chapter.getSubjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        chapter.updateName(name);
        return ChapterResponse.of(chapter, subject);
    }
}
