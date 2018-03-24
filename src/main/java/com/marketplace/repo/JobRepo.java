package com.marketplace.repo;


import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.model.Company;
import com.marketplace.model.Job;
import com.marketplace.model.JobStatus;


public interface JobRepo extends CrudRepository<Job, Long>{

		List<Job> findByStatusAndJobtitleContaining(JobStatus status, String jobtitle);
		List<Job> findByStatusAndSkillContaining(JobStatus status, String skill);
		List<Job> findByStatusAndLocationContaining(JobStatus status, String location);
		List<Job> findByStatusAndCompanyContaining(JobStatus status,  Company company);
		List<Job> findByStatusAndSalaryGreaterThan(JobStatus status, int salary);
		List<Job> findByStatusAndCompany(JobStatus status,  Company company);
		List<Job> findByStatusAndDescriptionContaining(JobStatus status,  String description);
		List<Job> findByStatusAndSalaryLessThan(JobStatus status, int salary);
		
	@Transactional
    @Modifying(clearAutomatically = true)
	@Query("update Job p set p.jobtitle=?1, p.skill=?2, p.description=?3, p.location=?4, p.salary=?5 where p.id=?6")
	void updateJobDetails(String job_title, String skill, String desc, String location, int salary, Long id);
	
	@Transactional
    @Modifying(clearAutomatically = true)
	@Query("update Job p set p.status=?1 where p.jobid=?2")
	void updateJobStatus(JobStatus status, Long jobid);

}
