/**
 * GlobalExceptionHandler API 엔드포인트
 */
package com.dating.backend.controller;

import com.dating.backend.dto.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.name(), exception.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        if (fieldError.getDefaultMessage() != null && !fieldError.getDefaultMessage().isBlank()) {
                            return fieldError.getDefaultMessage();
                        }
                        return fieldError.getField() + " 값을 다시 확인해 주세요.";
                    }
                    return error.getDefaultMessage() != null ? error.getDefaultMessage() : "입력값을 다시 확인해 주세요.";
                })
                .orElse("입력값을 다시 확인해 주세요.");

        return ResponseEntity.badRequest().body(new ApiErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String rawMessage = exception.getMostSpecificCause() != null
                ? exception.getMostSpecificCause().getMessage()
                : exception.getMessage();

        String message = "중복되었거나 사용할 수 없는 값입니다. 입력값을 다시 확인해 주세요.";
        if (rawMessage != null) {
            if (rawMessage.contains("uk_users_email")) {
                message = "이미 가입된 이메일입니다.";
            } else if (rawMessage.contains("uk_users_nickname")) {
                message = "이미 사용 중인 닉네임입니다.";
            } else if (rawMessage.contains("uk_users_phone")) {
                message = "이미 사용 중인 전화번호입니다.";
            } else if (rawMessage.contains("uk_user_profile_images_main_user")) {
                message = "대표 사진은 1장만 지정할 수 있습니다. 다시 시도해 주세요.";
            } else if (rawMessage.contains("uk_user_profile_images_user_order")) {
                message = "같은 사진 순서에는 1장만 등록할 수 있습니다. 다른 순서를 선택해 주세요.";
            } else if (rawMessage.contains("user_profile_images")) {
                message = "프로필 사진 저장 중 DB 제약 조건이 맞지 않습니다. 사진 순서와 대표 사진 여부를 확인해 주세요.";
            }
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("DUPLICATE_VALUE", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception exception) {
        log.error("Unhandled server error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("INTERNAL_SERVER_ERROR", "잠시 후 다시 시도해 주세요."));
    }
}
