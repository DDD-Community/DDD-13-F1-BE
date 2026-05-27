package com.f1.quiket.domain.lecture.dto;

import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.dto.StudyMaterialUploadType;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 강의 자료 AI 처리 요청 DTO
 *
 * 업로드 입력값과 파트 분류 조건 전달
 */
@Getter
@Builder
public class LectureMaterialAiProcessRequest {
    private final StudyMaterialUploadType uploadType;
    private final PartSplitMethod partSplitMethod;
    private final String chapterName;
    private final String text;
    private final List<StudyMaterialFile> files;
    private final List<LecturePartSplitPlan> partSplitPlans;
}

