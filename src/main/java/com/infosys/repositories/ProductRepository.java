package com.infosys.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.infosys.model.Product;

@Repository
@Transactional
public interface ProductRepository extends JpaRepository<Product, Long> {
	
	Product findByName(String name);
}
