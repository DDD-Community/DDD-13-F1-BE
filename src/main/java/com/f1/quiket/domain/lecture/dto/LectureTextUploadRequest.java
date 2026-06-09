package com.f1.quiket.domain.lecture.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 텍스트 직접 입력 강의 업로드 요청 DTO
 *
 * 과목, 챕터, 파트 분류 방식, 입력 텍스트를 전달
 * chapterName 미입력 시 AI가 업로드 내용 기반으로 자동 생성
 */
@Getter
@NoArgsConstructor
public class LectureTextUploadRequest {

    @NotBlank(message = "subjectId는 필수입니다.")
    private String subjectId;

    /**
     * 챕터명 (선택)
     * null이면 AI가 업로드 내용 기반으로 챕터명 자동 생성
     * 제공 시 1~30자 제약 적용
     */
    @Size(min = 1, max = 30, message = "chapterName은 1~30자로 입력해주세요.")
    private String chapterName;

    @NotBlank(message = "uploadType은 필수입니다.")
    private String uploadType;

    @NotBlank(message = "partSplitMethod는 필수입니다.")
    private String partSplitMethod;

    @NotBlank(message = "text는 필수입니다.")
    @Size(min = 100, max = 30000, message = "text는 100~30,000자로 입력해주세요.")
    private String text;

    @Valid
    private List<@NotNull PartSplitPlanRequest> partSplitPlans;
}
