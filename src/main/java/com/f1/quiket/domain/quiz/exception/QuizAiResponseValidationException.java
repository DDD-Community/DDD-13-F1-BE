package com.f1.quiket.domain.quiz.exception;

import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;

public class QuizAiResponseValidationException extends CustomException {

    public QuizAiResponseValidationException(String message) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, message);
    }
}
