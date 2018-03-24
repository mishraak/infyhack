package com.marketplace.model;

import java.sql.Date;

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
@Table(name="interview")
public class Interview {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false)
	private Long interviewid;
	
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="userid")
	private User user;
	
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="jobid")
	private Job job;
	
	private Date time;
	private String location;
	private String feedback;
	private InterviewStatus status;
	
	public Interview(Long interviewid, User user, Job job, Date time, String location, String feedback,
			InterviewStatus status) {
		super();
		this.interviewid = interviewid;
		this.user = user;
		this.job = job;
		this.time = time;
		this.location = location;
		this.feedback = feedback;
		this.status = status;
	}

	public Interview() {
	}

	public Long getInterviewid() {
		return interviewid;
	}

	public void setInterviewid(Long interviewid) {
		this.interviewid = interviewid;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getFeedback() {
		return feedback;
	}

	public void setFeedback(String feedback) {
		this.feedback = feedback;
	}

	public InterviewStatus getStatus() {
		return status;
	}

	public void setStatus(InterviewStatus status) {
		this.status = status;
	}

}
