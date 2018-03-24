package com.marketplace.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "application", uniqueConstraints = { @UniqueConstraint(columnNames = { "userid", "jobid" }) })
public class Application {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(unique = true, nullable = false)
	private Long applicationid;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "userid")
	private User user;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "jobid")
	private Job job;

	private ApplicationType type; // (interested/ applied)
	private ApplicationStatus status;

	public Application(User user, Job job, ApplicationType type, ApplicationStatus status) {
		super();
		this.user = user;
		this.job = job;
		this.type = type;
		this.status = status;
	}

	public Application() {
	}

	public Long getApplicationid() {
		return applicationid;
	}

	public void setApplicationid(Long applicationid) {
		this.applicationid = applicationid;
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

	public ApplicationType getType() {
		return type;
	}

	public void setType(ApplicationType type) {
		this.type = type;
	}

	public ApplicationStatus getStatus() {
		return status;
	}

	public void setStatus(ApplicationStatus status) {
		this.status = status;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Application ? ((Application) obj).getApplicationid() == this.applicationid : false;
	}
}
