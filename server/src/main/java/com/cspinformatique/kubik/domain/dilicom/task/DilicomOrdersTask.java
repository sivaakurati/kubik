package com.cspinformatique.kubik.domain.dilicom.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cspinformatique.kubik.domain.dilicom.service.DilicomOrderService;

@Component
public class DilicomOrdersTask {
	@Autowired private DilicomOrderService dilicomOrderService;
	
	@Scheduled(fixedDelay = 1000 * 60 * 1)
	public void sendPendingDilicomOrders(){
		this.dilicomOrderService.sendPendingDilicomOrders();
	}
}
