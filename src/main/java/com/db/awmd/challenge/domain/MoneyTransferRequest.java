package com.db.awmd.challenge.domain;

import java.math.BigDecimal;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * 
 * @author Abhay Kumar
 *
 */
@Data
public class MoneyTransferRequest {

	@NotNull
	@NotEmpty
	private String fromAccountId;

	@NotNull
	@NotEmpty
	private String toAccountId;

	@NotNull
	@Min(value = 0, message = "Initial balance must be positive.")
	private BigDecimal amount;

	@JsonCreator
	public MoneyTransferRequest(@JsonProperty("fromAccountId") String fromAccountId,
			@JsonProperty("toAccountId") String toAccountId, @JsonProperty("amount") BigDecimal amount) {
		this.fromAccountId = fromAccountId;
		this.toAccountId = toAccountId;
		this.amount = amount;
	}

}
