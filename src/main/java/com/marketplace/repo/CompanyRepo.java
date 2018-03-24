package com.marketplace.repo;

import org.springframework.data.repository.CrudRepository;

import com.marketplace.model.Company;

public interface CompanyRepo extends CrudRepository<Company, Long> {
	Company findByEmailid(String emailid);
	Company findByName(String name);
	Company findByNameContaining(String name);
}
