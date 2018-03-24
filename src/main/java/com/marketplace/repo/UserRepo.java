package com.marketplace.repo;

import org.springframework.data.repository.CrudRepository;

import com.marketplace.model.Job;
import com.marketplace.model.User;

public interface UserRepo extends CrudRepository<User, Long>{
	User findByEmailid(String emailid);
	User findByUsername(String username);
}
