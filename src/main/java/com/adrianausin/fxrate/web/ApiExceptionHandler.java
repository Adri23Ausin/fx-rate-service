package com.adrianausin.fxrate.web;

import com.adrianausin.fxrate.rates.FxRateException;
import com.adrianausin.fxrate.rates.InvalidRateDateException;
import com.adrianausin.fxrate.rates.RatesUnavailableException;
import com.adrianausin.fxrate.rates.UnsupportedCurrencyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns errors into RFC 9457 problem details. Framework errors (bad parameters, type
 * mismatches...) are handled by {@link ResponseEntityExceptionHandler}.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(FxRateException.class)
    ProblemDetail handleFxRateException(FxRateException ex) {
        HttpStatus status = switch (ex) {
            case UnsupportedCurrencyException e -> HttpStatus.BAD_REQUEST;
            case InvalidRateDateException e -> HttpStatus.BAD_REQUEST;
            case RatesUnavailableException e -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }
}
