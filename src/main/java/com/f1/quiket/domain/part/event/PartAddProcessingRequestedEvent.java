package com.f1.quiket.domain.part.event;

import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import java.util.List;

/**
 * 기존 챕터 파트 추가 비동기 처리 요청 이벤트
 *
 * 업로드 접수 커밋 이후 OCR/텍스트 추출과 단일 파트 생성을 시작하기 위한 데이터 전달
 */
public record PartAddProcessingRequestedEvent(
        Long lectureUploadId,
        Long chapterId,
        Long subjectId,
        Long userId,
        String partName,
        Integer partNumber,
        StudyMaterialUploadType uploadType,
        String text,
        List<StudyMaterialFile> files
) {
}
