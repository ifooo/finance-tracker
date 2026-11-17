package com.parcels.report.dto;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.nio.file.Path;
import java.time.YearMonth;

/** Report files written for a month (paths serialize as plain strings, not file:// URIs). */
public record ReportFiles(YearMonth month,
                          @JsonSerialize(using = ToStringSerializer.class) Path spreadsheet,
                          @JsonSerialize(using = ToStringSerializer.class) Path pdf) {
}
