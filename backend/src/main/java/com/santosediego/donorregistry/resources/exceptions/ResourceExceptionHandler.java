package com.santosediego.donorregistry.resources.exceptions;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.santosediego.donorregistry.services.exceptions.DuplicateCpfException;
import com.santosediego.donorregistry.services.exceptions.InvalidEnumValueException;
import com.santosediego.donorregistry.services.exceptions.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class ResourceExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ResourceExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<StandardError> notFound(ResourceNotFoundException e, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, "Recurso não encontrado", e.getMessage(), request);
	}

	@ExceptionHandler(DuplicateCpfException.class)
	public ResponseEntity<StandardError> duplicateCpf(DuplicateCpfException e, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, "Conflito", e.getMessage(), request);
	}

	@ExceptionHandler(InvalidEnumValueException.class)
	public ResponseEntity<StandardError> invalidEnum(InvalidEnumValueException e, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Requisição inválida", e.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationError> validation(MethodArgumentNotValidException e,
			HttpServletRequest request) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		ValidationError err = new ValidationError(Instant.now(), status.value(), "Dados inválidos",
				"Há campos com valores inválidos.", request.getRequestURI());
		e.getBindingResult().getFieldErrors().forEach(f -> err.addError(f.getField(), f.getDefaultMessage()));
		return ResponseEntity.status(status).body(err);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<StandardError> unreadable(HttpMessageNotReadableException e,
			HttpServletRequest request) {
		// The cause is not echoed: it may contain part of the submitted data
		return build(HttpStatus.BAD_REQUEST, "Requisição inválida",
				"Corpo da requisição inválido. Verifique o JSON e as datas enviadas.", request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<StandardError> typeMismatch(MethodArgumentTypeMismatchException e,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Requisição inválida", "Valor inválido para o parâmetro '"
				+ e.getName() + "'.", request);
	}

	// Last resort: nothing internal (message, stack trace) goes in the response body
	@ExceptionHandler(Exception.class)
	public ResponseEntity<StandardError> unexpected(Exception e, HttpServletRequest request) {
		if (e instanceof ErrorResponse errorResponse) {
			// Framework errors with their own status (405, 415, ...) keep that status
			HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
			return build(status, status.getReasonPhrase(), "Não foi possível processar a requisição.", request);
		}
		LOG.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Erro inesperado. Tente novamente mais tarde.",
				request);
	}

	private ResponseEntity<StandardError> build(HttpStatus status, String error, String message,
			HttpServletRequest request) {
		StandardError err = new StandardError(Instant.now(), status.value(), error, message,
				request.getRequestURI());
		return ResponseEntity.status(status).body(err);
	}
}
