package com.example.submito.exception

import com.example.submito.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import java.time.Instant
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice


@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailAlreadyExistsException(
            exception: EmailAlreadyExistsException,
            request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.CONFLICT

        return ResponseEntity
                .status(status)
                .body(
                        ErrorResponse(
                                status = status.value(),
                                error = status.reasonPhrase,
                                message = "Email ${exception.email} already exists",
                                path = request.servletPath,
                                timestamp = Instant.now()
                        )
                )
    }

    // should it return all validation errors or just the first one?
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        exception: MethodArgumentNotValidException, 
        request: HttpServletRequest): ResponseEntity<ErrorResponse> {

        val status = HttpStatus.BAD_REQUEST
        val message = exception.bindingResult
            .fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }

        return ResponseEntity
                .status(status)
                .body(ErrorResponse(
                    status = status.value(),
                    error = status.reasonPhrase,
                    message = message,
                    path = request.servletPath,
                    timestamp = Instant.now()
                ))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleInvalidRequestBody(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST

        return ResponseEntity
            .status(status)
            .body(
                ErrorResponse(
                    status = status.value(),
                    error = status.reasonPhrase,
                    message = "Invalid request body. Role must be STUDENT or TEACHER",
                    path = request.servletPath,
                    timestamp = Instant.now()
                )
            )
    }
}