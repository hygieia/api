package com.capitalone.dashboard.request;

import javax.validation.constraints.NotNull;

public class EncryptionRequest extends BaseRequest {
	@NotNull
	private String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
