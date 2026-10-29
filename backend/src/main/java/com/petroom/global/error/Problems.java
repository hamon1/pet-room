package com.petroom.global.error;

import org.springframework.http.ProblemDetail;

public final class Problems {

    private Problems() {
    }

    public static ProblemDetail of(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setProperty("code", code.name());
        return problem;
    }

    public static ProblemDetail of(ErrorCode code) {
        return of(code, code.defaultMessage());
    }
}
