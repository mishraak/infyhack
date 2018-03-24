package com.marketplace.repo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.model.Application;
import com.marketplace.model.ApplicationStatus;
import com.marketplace.model.ApplicationType;
import com.marketplace.model.Job;
import com.marketplace.model.JobStatus;
import com.marketplace.model.User;

public interface ApplicationRepo extends CrudRepository<Application, Long> {

	// updating the application status when job status is changed as filled
	@Transactional
	@Modifying(clearAutomatically = true)
	@Query("update Application p set p.status=?1 where p.id=?2")
	void updateApplicationStatus_JS(ApplicationStatus status, Long id);

	List<Application> findByStatusAndJob(ApplicationStatus status, Job job);

	List<Application> findByJob(Job job);

	List<Application> findByUserAndType(User user, ApplicationType type);

	Application findByJobAndUser(Job job, User user);

	List<Application> findByUser(User user);

	Long removeByJob(Job job);

//	@Transactional
//	@Query("select user.emailid from user, application where application.jobid=?1 and user.userid=application.userid")
//	List<String> getEmailsByJobid(Long jobid);

	List<Application> findByJobAndStatusIn(Job job, List<ApplicationStatus> applicationStatus);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query("update Application p set p.type=?1 where p.applicationid=?2")
	void updateApplicationType(ApplicationType type, Long applicationid);
}
