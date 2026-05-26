package com.f1.quiket.domain.lecture.service;

import com.f1.quiket.domain.lecture.dto.LectureMaterialFile;
import com.f1.quiket.domain.lecture.dto.LecturePdfTextExtractionResult;

public interface LecturePdfTextExtractor {

    LecturePdfTextExtractionResult extract(LectureMaterialFile pdfFile);
}

