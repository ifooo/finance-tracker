package com.parcels.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Schema(description = "Details about the error that occurred")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetails {

  @Schema(description = "Timestamp when the error occurred", example = "2022-04-01T14:30:00Z")
  @NotNull
  private OffsetDateTime timestamp;

  @Schema(description = "URL that was accessed when the error occurred", example = "https://example.com")
  @NotBlank
  private String url;

  @Schema(description = "HTTP method used when the error occurred", example = "POST")
  @NotBlank
  private String method;

  @Schema(description = "Path accessed when the error occurred", example = "/example/path")
  @NotBlank
  private String path;

  @Schema(description = "HTTP Status returned when the error occurred", example = "500")
  @NotNull
  private Integer status;

  @Schema(description = "Brief description of the error", example = "Internal Server Error")
  @NotBlank
  private String error;

  @Schema(description = "Type of error", example = "Server Error")
  @NotBlank
  private String type;

  @Schema(description = "Detailed message about the error", example = "An unexpected server error occurred")
  @Nullable
  private String message;

  @Schema(description = "Cause message of the error", example = "Null pointer exception occurred")
  @Nullable
  private String rootCauseMessage;

  @Schema(description = "Type of root cause error", example = "NullPointerException")
  @Nullable
  private String rootCauseType;

  @Nullable
  @Schema(description = "Error details", example = "{'body': '...', 'errorCount': '3'}")
  private Map<String, Object> properties;

  public static ErrorDetails of(HttpServletRequest request, HttpStatus httpStatus, Class<?> errorType, String message, Throwable rootCause) {
    return of(request, httpStatus.value(), httpStatus.getReasonPhrase(), errorType, message, rootCause);
  }

  public static ErrorDetails of(HttpServletRequest request, ErrorResponse errorResponse, Class<?> errorType, String message, Throwable rootCause) {
    final var rawCode = errorResponse.getStatusCode().value();
    final var phrase = Optional.ofNullable(HttpStatus.resolve(rawCode)).map(HttpStatus::getReasonPhrase).orElse("n/a");
    return of(request, rawCode, phrase, errorType, message, rootCause);
  }

  // Factory method using the builder pattern
  private static ErrorDetails of(HttpServletRequest request, int status, String phrase, Class<?> errorType, String message, Throwable rootCause) {
    return ErrorDetails.builder()
        .timestamp(OffsetDateTime.now())
        .url(buildUrl(request))
        .method(request.getMethod())
        .path(request.getRequestURI())
        .status(status)
        .error(phrase)
        .type(Optional.ofNullable(errorType).map(Class::getSimpleName).orElse("n/a"))
        .message(message)
        .build();
  }

  private static String buildUrl(HttpServletRequest httpServletRequest) {
    final var queryString = httpServletRequest.getQueryString();
    final var url = httpServletRequest.getRequestURL();
    if (StringUtils.isBlank(queryString)) {
      return url.toString();
    }
    return url.append('?').append(queryString).toString();
  }

  public ErrorDetails property(String key, Object value) {
    if (properties == null) {
      properties = new HashMap<>();
    }
    properties.put(key, value);
    return this;
  }

}
