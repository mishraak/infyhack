package com.marketplace.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;


@Entity
@Table(name = "profile")
public class Profile {

	@Id
	@Column(name = "userid", unique = true, nullable = false)
	private Long userid;

	@Column(name = "firstname")
	private String firstname;

	@Column(name = "lastname")
	private String lastname;

	private String imageloc;

	private String intro;

	private String workex;

	@Column(nullable = false)
	private String education;

	@Column(nullable = false)
	//@ElementCollection
	//private List<String> skills;
	String skills;
	
	private String phone;
	
	private String resumePath;
	
	//public Profile(Long userid, String firstname, String lastname, String imageloc, String intro, String workex,
	//		String education, List<String> skills, String phone) {
	public Profile(Long userid, String firstname, String lastname, String imageloc, String intro, String workex,
				String education, String skills, String phone) {
		super();
		this.userid = userid;
		this.firstname = firstname;
		this.lastname = lastname;
		this.imageloc = imageloc;
		this.intro = intro;
		this.workex = workex;
		this.education = education;
		this.skills = skills;
		this.phone = phone;
//		this.resumePath = resumePath;
	}

	public Profile() {
	}

	public Long getUserid() {
		return userid;
	}

	public void setUserid(Long userid) {
		this.userid = userid;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getImageloc() {
		return imageloc;
	}

	public void setImageloc(String imageloc) {
		this.imageloc = imageloc;
	}

	public String getIntro() {
		return intro;
	}

	public void setIntro(String intro) {
		this.intro = intro;
	}

	public String getWorkex() {
		return workex;
	}

	public void setWorkex(String workex) {
		this.workex = workex;
	}

	public String getEducation() {
		return education;
	}

	public void setEducation(String education) {
		this.education = education;
	}

	public String getSkills() {
		return skills;
	}

	public void setSkills(String skills) {
		this.skills = skills;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getResumePath() {
		return resumePath;
	}

	public void setResumePath(String resumePath) {
		this.resumePath = resumePath;
	}

	
}
