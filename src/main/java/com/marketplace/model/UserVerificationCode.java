package com.marketplace.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="user_verification_code")
public class UserVerificationCode {
	@Id
	@Column(unique = true, nullable = false)
	private String emailID;
	
	private String verificationCode;
	
	private boolean status;
}
