package com.cspinformatique.kubik.server.domain.warehouse.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cspinformatique.kubik.server.model.product.Product;
import com.cspinformatique.kubik.server.model.warehouse.InventoryCount;

public interface InventoryCountService {
	Page<InventoryCount> findByProduct(Product product, Pageable pageable);

	double findProductQuantityCounted(int productId);
	
	InventoryCount save(InventoryCount inventoryCount);
}