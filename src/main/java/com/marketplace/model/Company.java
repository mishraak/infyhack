package com.marketplace.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name="company")
public class Company {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false)
	private Long companyid;

	@Column(unique = true, nullable = false)
	private String name;

	@Column(unique = true, nullable = false)
	private String emailid;

	private String website;
	private String address;
	private String description;
	private String logo_image;
	private String password;
	private String verificationcode;
	
	@Column(name="status", columnDefinition="boolean not null default 0")
	private boolean status = false;
	
	@OneToMany(mappedBy = "company")
	@JsonBackReference
	private List<Job> jobs;

	public Company() {
	}
	
	public Company(String name, String registered_email, String password, String website, String address,
			String description, String logo_image, String verificationcode) {
		super();
		//this.companyid = companyid;
		this.name = name;
		this.emailid = registered_email;
		this.website = website;
		this.address = address;
		this.description = description;
		this.logo_image = logo_image;
		this.password = password;
		this.verificationcode = verificationcode;
	}

	public Long getCompanyid() {
		return companyid;
	}

	public void setCompanyid(Long companyid) {
		this.companyid = companyid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLogo_image() {
		return logo_image;
	}

	public void setLogo_image(String logo_image) {
		this.logo_image = logo_image;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public List<Job> getJobs() {
		return jobs;
	}

	public void setJobs(List<Job> jobs) {
		this.jobs = jobs;
	}

	public String getEmailid() {
		return emailid;
	}

	public void setEmailid(String emailid) {
		this.emailid = emailid;
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
