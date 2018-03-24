package com.marketplace.repo;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.model.Profile;

public interface ProfileRepo extends CrudRepository<Profile, Long>{
	
	@Transactional
    @Modifying(clearAutomatically = true)
	@Query("update Profile p set p.firstname=?1, p.lastname=?2, p.imageloc=?3, p.intro=?4, p.workex=?5, p.education=?6, p.skills=?7, p.phone=?8 where p.id=?9")
	void updateProfile(String firstname, String lastname, String picture, String intro, String workex, String education, String skills, String phone, Long id);

}