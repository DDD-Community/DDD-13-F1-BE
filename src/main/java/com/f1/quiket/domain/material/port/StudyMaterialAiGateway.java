package com.f1.quiket.domain.material.port;

import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import java.util.List;

public interface StudyMaterialAiGateway {

    String generateFromImages(String systemMessage, String userMessage, List<StudyMaterialFile> imageFiles);

    String generateFromPdf(String systemMessage, String userMessage, StudyMaterialFile pdfFile);

    String generateFromText(String systemMessage, String userMessage);
}

