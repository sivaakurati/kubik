package com.cspinformatique.kubik.server.domain.warehouse.service.impl;

import javax.annotation.Resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.cspinformatique.kubik.server.domain.warehouse.repository.InventoryCountRepository;
import com.cspinformatique.kubik.server.domain.warehouse.service.InventoryCountService;
import com.cspinformatique.kubik.server.domain.warehouse.service.ProductInventoryService;
import com.cspinformatique.kubik.server.model.product.Product;
import com.cspinformatique.kubik.server.model.warehouse.InventoryCount;

@Service
public class InventoryCountServiceImpl implements InventoryCountService {
	@Resource
	private InventoryCountRepository inventoryCountRepository;

	@Resource
	private ProductInventoryService productInventoryService;

	@Override
	public Page<InventoryCount> findByProduct(Product product, Pageable pageable) {
		return inventoryCountRepository.findByProduct(product, pageable);
	}

	@Override
	public double findProductQuantityCounted(int productId) {
		Double result = inventoryCountRepository.findProductQuantityCounted(productId);

		if (result == null) {
			return 0d;
		}

		return result;
	}

	@Override
	public InventoryCount save(InventoryCount inventoryCount) {
		inventoryCount = inventoryCountRepository.save(inventoryCount);

		productInventoryService.updateInventory(inventoryCount.getProduct());

		return inventoryCount;
	}
}
