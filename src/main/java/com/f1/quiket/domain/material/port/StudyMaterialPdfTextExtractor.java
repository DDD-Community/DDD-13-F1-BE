package com.f1.quiket.domain.material.port;

import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.dto.StudyMaterialPdfTextExtractionResult;

public interface StudyMaterialPdfTextExtractor {

    StudyMaterialPdfTextExtractionResult extract(StudyMaterialFile pdfFile);
}

