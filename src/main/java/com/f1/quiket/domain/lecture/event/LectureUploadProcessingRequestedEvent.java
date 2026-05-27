package com.f1.quiket.domain.lecture.event;

import com.f1.quiket.domain.lecture.dto.LecturePartSplitPlan;
import com.f1.quiket.domain.lecture.dto.PartSplitMethod;
import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import java.util.List;

/**
 * 강의 업로드 비동기 처리 요청 이벤트
 *
 * 업로드 접수 트랜잭션 커밋 이후 OCR/텍스트 추출/파트 분류를 시작하기 위한 데이터 전달
 */
public record LectureUploadProcessingRequestedEvent(
        Long lectureUploadId,
        Long chapterId,
        Long subjectId,
        Long userId,
        String chapterName,
        StudyMaterialUploadType uploadType,
        PartSplitMethod partSplitMethod,
        String text,
        List<StudyMaterialFile> files,
        List<LecturePartSplitPlan> partSplitPlans
) {
}
