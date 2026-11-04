package com.petroom.global.error;

import com.petroom.global.config.RequestLogFilter;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException e) {
        return Problems.of(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        ProblemDetail problem = Problems.of(ErrorCode.INVALID_INPUT);
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() == null ? "올바르지 않은 값입니다." : fe.getDefaultMessage()))
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ProblemDetail handleUnreadable(Exception e) {
        return Problems.of(ErrorCode.INVALID_INPUT, "요청 형식을 확인해 주세요.");
    }

    /**
     * 예상하지 못한 오류: 스택을 ERROR로 남기고, 사용자에게는 내부 정보 없이 요청 ID만 돌려줍니다.
     * 문의가 오면 요청 ID로 로그를 찾습니다. Spring이 상태 코드를 정해 둔 예외(404, 405 등)는 그대로 둡니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e) throws Exception {
        if (e instanceof ErrorResponse known) {
            return ResponseEntity.status(known.getStatusCode()).body(known.getBody());
        }
        if (e instanceof org.springframework.security.access.AccessDeniedException
                || e instanceof org.springframework.security.core.AuthenticationException) {
            throw e;
        }
        log.error("Unhandled exception", e);
        ProblemDetail problem = Problems.of(ErrorCode.INTERNAL_ERROR);
        problem.setProperty("requestId", MDC.get(RequestLogFilter.MDC_KEY));
        return ResponseEntity.internalServerError().body(problem);
    }
}
