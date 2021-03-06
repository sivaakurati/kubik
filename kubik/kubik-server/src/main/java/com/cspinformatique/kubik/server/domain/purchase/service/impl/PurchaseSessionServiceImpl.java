package com.cspinformatique.kubik.server.domain.purchase.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.cspinformatique.kubik.server.domain.product.service.ProductService;
import com.cspinformatique.kubik.server.domain.purchase.repository.PurchaseSessionRepository;
import com.cspinformatique.kubik.server.domain.purchase.service.PurchaseOrderService;
import com.cspinformatique.kubik.server.domain.purchase.service.PurchaseSessionDetailService;
import com.cspinformatique.kubik.server.domain.purchase.service.PurchaseSessionService;
import com.cspinformatique.kubik.server.model.product.Product;
import com.cspinformatique.kubik.server.model.product.Supplier;
import com.cspinformatique.kubik.server.model.purchase.NotationCode;
import com.cspinformatique.kubik.server.model.purchase.PurchaseOrder;
import com.cspinformatique.kubik.server.model.purchase.PurchaseOrderDetail;
import com.cspinformatique.kubik.server.model.purchase.PurchaseSession;
import com.cspinformatique.kubik.server.model.purchase.PurchaseSessionDetail;
import com.cspinformatique.kubik.server.model.purchase.ShippingMode;
import com.cspinformatique.kubik.server.model.purchase.PurchaseSession.Status;

@Service
public class PurchaseSessionServiceImpl implements PurchaseSessionService {
	@Autowired
	private ProductService productService;

	@Autowired
	private PurchaseOrderService purchaseOrderService;

	@Autowired
	private PurchaseSessionDetailService purchaseSessionDetailService;

	@Autowired
	private PurchaseSessionRepository purchaseSessionRepository;

	@Override
	public PurchaseSession addProductToNewestPurchaseSession(Product product,
			double quantity) {
		PurchaseSession purchaseSession = null;
		Page<PurchaseSession> purchaseSessionPage = this
				.findByStatus(Status.DRAFT, new PageRequest(0, 1,
						Direction.DESC, "openDate"));

		if (purchaseSessionPage.getTotalElements() > 0) {
			purchaseSession = purchaseSessionPage.getContent().get(0);
		} else {
			purchaseSession = new PurchaseSession(null, null, null,
					new ArrayList<PurchaseSessionDetail>(), Status.DRAFT,
					new Date(), null, null);
		}

		PurchaseSessionDetail purchaseSessionDetail = null;
		for (PurchaseSessionDetail existingDetail : purchaseSession
				.getDetails()) {
			if (existingDetail.getProduct().getId().intValue() == product
					.getId().intValue()) {
				purchaseSessionDetail = existingDetail;
			}
		}

		if (purchaseSessionDetail == null) {
			purchaseSessionDetail = new PurchaseSessionDetail(0,
					purchaseSession, product, 0d);

			purchaseSession.getDetails().add(purchaseSessionDetail);
		}

		purchaseSessionDetail.setQuantity(purchaseSessionDetail.getQuantity()
				+ quantity);

		return this.save(purchaseSession);
	}

	@Override
	public Iterable<PurchaseSession> findAll() {
		return this.purchaseSessionRepository.findAll();
	}

	@Override
	public Page<PurchaseSession> findAll(Pageable pageable) {
		return this.purchaseSessionRepository.findAll(pageable);
	}

	@Override
	public Page<PurchaseSession> findByStatus(Status status, Pageable pageable) {
		return this.purchaseSessionRepository.findByStatus(status, pageable);
	}

	@Override
	public Page<PurchaseSession> findByStatus(List<Status> status, Pageable pageable) {
		return this.purchaseSessionRepository.findByStatus(status, pageable);
	}

	@Override
	public Iterable<PurchaseSession> findByProduct(Product product) {
		return this.purchaseSessionDetailService
				.findPurchaseOrdersByProduct(product);
	}

	@Override
	public Iterable<PurchaseSession> findByProductAndStatus(Product product,
			Status status) {
		return this.purchaseSessionDetailService
				.findPurchaseOrdersByProductAndStatus(product, status);
	}

	@Override
	public PurchaseSession findOne(int id) {
		return this.purchaseSessionRepository.findOne(id);
	}

	private void generatePurchaseOrder(PurchaseSession purchaseSession) {
		// Generates a purchase order for every distinct supplier concerned by
		// the delivery.
		Map<String, PurchaseOrder> purchaseOrders = new HashMap<String, PurchaseOrder>();

		Calendar maxDeliveryCalendar = Calendar.getInstance();
		maxDeliveryCalendar.add(Calendar.DAY_OF_MONTH, 7);
		Date maxDeliveryDate = maxDeliveryCalendar.getTime();

		for (PurchaseSessionDetail detail : purchaseSession.getDetails()) {
			detail.setProduct(this.productService.findOne(detail.getProduct()
					.getId()));

			Supplier supplier = detail.getProduct().getSupplier();

			PurchaseOrder order = purchaseOrders.get(supplier.getEan13());

			if (order == null) {
				order = new PurchaseOrder(0, supplier, new Date(), null, null,
						ShippingMode.USUAL_METHOD, NotationCode.USUAL_RULE,
						new Date(), maxDeliveryDate,
						new ArrayList<PurchaseOrderDetail>(),
						PurchaseOrder.Status.DRAFT, null, 0f, 0d,
						purchaseSession, null);

				purchaseOrders.put(supplier.getEan13(), order);
			}

			order.getDetails().add(
					new PurchaseOrderDetail(null, order, detail.getProduct(),
							detail.getQuantity(), 0f, 0f, null, 0d, 0d));
		}

		for(PurchaseOrder purchaseOrder : purchaseOrders.values()){
			this.purchaseOrderService.save(purchaseOrder);
		}
	}

	@Override
	@Transactional
	public PurchaseSession save(PurchaseSession purchaseSession) {
		if (purchaseSession.getOpenDate() == null) {
			purchaseSession.setOpenDate(new Date());
		}

		if (purchaseSession.getMinDeliveryDate() == null) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			purchaseSession.setMinDeliveryDate(calendar.getTime());
		}

		if (purchaseSession.getMaxDeliveryDate() == null) {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DAY_OF_MONTH, 7);
			purchaseSession.setMaxDeliveryDate(calendar.getTime());
		}

		if (purchaseSession.getStatus() == null) {
			purchaseSession.setStatus(Status.DRAFT);
		}

		Status status = purchaseSession.getStatus();
		if (status.equals(Status.SUBMITED) || status.equals(Status.CANCELED)) {
			PurchaseSession oldSession = this.findOne(purchaseSession.getId());

			if (oldSession == null
					|| oldSession.getStatus().equals(Status.DRAFT)) {
				if (status.equals(Status.SUBMITED)) {
					// Generate the purchase order
					this.generatePurchaseOrder(purchaseSession);
				}

				purchaseSession.setCloseDate(new Date());
			}
		}

		return this.purchaseSessionRepository.save(purchaseSession);
	}
}
