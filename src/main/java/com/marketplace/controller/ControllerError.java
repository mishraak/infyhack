package com.marketplace.controller;

import javax.swing.text.AbstractDocument.Content;

import org.springframework.http.HttpStatus;

public class ControllerError {
	public ControllerError(ErrorType badRequest) {
		super();
		this.BadRequest = badRequest;
	}
	
	public ControllerError(int code, String msg){
		super();
		this.BadRequest = new ErrorType(String.valueOf(code), msg);
	}
	
	public ControllerError(){}
	
	private ErrorType BadRequest;

	public ErrorType getBadRequest() {
		return BadRequest;
	}

	public void setBadRequest(ErrorType badRequest) {
		BadRequest = badRequest;
	}

}
