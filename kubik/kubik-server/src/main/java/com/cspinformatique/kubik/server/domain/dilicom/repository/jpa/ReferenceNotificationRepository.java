package com.cspinformatique.kubik.server.domain.dilicom.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cspinformatique.kubik.server.domain.dilicom.model.ReferenceNotification;
import com.cspinformatique.kubik.server.domain.dilicom.model.ReferenceNotification.Status;
import com.cspinformatique.kubik.server.model.product.Product;

public interface ReferenceNotificationRepository extends
		JpaRepository<ReferenceNotification, Integer> {
	Long countByStatus(Status status);
	
	ReferenceNotification findByProduct(Product product);
	
	Page<ReferenceNotification> findByStatus(Status status, Pageable pageable);
}
