package com.dws.challenge.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class Transfer {

  @NotNull
  @NotEmpty
  private String fromAccountId;

  @NotNull
  @NotEmpty
  private String toAccountId;

  @NotNull
  @DecimalMin(value = "0.0", inclusive = false, message = "Amount to transfer must be positive.")
  private BigDecimal amount;

}
