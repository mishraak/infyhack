package com.marketplace.model;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="user")
public class User implements Serializable{
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="userid", unique=true, nullable=false)
	private Long userid;
	
	@Column(name="username")
	private String username;
	
	@Column(name="emailid", unique=true, nullable=false)
	private String emailid;

//	@OneToOne(fetch=FetchType.LAZY)
//	@JoinColumn(name="profileid")
//	private Profile profile;

	@Column(name="password")
	private String password;
	
	@Column(name="verificationcode")
	private String verificationcode;
	
//	@Column(name="status", nullable=false)
	@Column(name="status", columnDefinition="boolean not null default 0")
	private boolean status = false;
	
//	public User(String username, String emailid, Profile profile) {
	public User(String username, String emailid, String password, String verificationcode) {
		super();
		this.username = username;
		this.emailid = emailid;
		this.password = password;
		this.verificationcode = verificationcode;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -4018494950502453606L;

	public Long getUserid() {
		return userid;
	}

	public void setUserid(Long userid) {
		this.userid = userid;
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmailid() {
		return emailid;
	}

	public void setEmailid(String emailid) {
		this.emailid = emailid;
	}

	public User() {
	}


	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getVerificationcode() {
		return verificationcode;
	}

	public void setVerificationcode(String verificationcode) {
		this.verificationcode = verificationcode;
	}

	public boolean isStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}
		
	
}
