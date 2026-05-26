package com.f1.quiket.domain.lecture.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 강의 자료 AI 처리 요청 DTO
 */
@Getter
@Builder
public class LectureMaterialAiProcessRequest {
    private final LectureUploadType uploadType;
    private final PartSplitMethod partSplitMethod;
    private final String chapterName;
    private final String text;
    private final List<LectureMaterialFile> files;
    private final List<LecturePartSplitPlan> partSplitPlans;
}

