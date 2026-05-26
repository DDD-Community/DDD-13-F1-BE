package com.f1.quiket.domain.lecture.service;

import com.f1.quiket.domain.lecture.dto.LectureMaterialFile;
import java.util.List;

public interface LectureMaterialAiGateway {

    String analyzeImage(String systemMessage, String userMessage, List<LectureMaterialFile> imageFiles);

    String analyzePdf(String systemMessage, String userMessage, LectureMaterialFile pdfFile);

    String analyzeText(String systemMessage, String userMessage);
}

