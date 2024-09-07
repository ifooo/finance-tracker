package com.parcels.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
@EqualsAndHashCode
@ConfigurationProperties(prefix = "")
public class CustomRestClientProperties {
}
