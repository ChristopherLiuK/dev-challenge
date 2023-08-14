package com.dws.challenge.domain;

import jakarta.validation.constraints.Min;
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
  @Min(value = 0, message = "Amount to transfer must be positive")
  private BigDecimal amount;

}
