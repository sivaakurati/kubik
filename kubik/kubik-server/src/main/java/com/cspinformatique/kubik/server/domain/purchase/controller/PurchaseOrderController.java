package com.cspinformatique.kubik.server.domain.purchase.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.cspinformatique.kubik.server.domain.purchase.service.PurchaseOrderService;
import com.cspinformatique.kubik.server.jasper.service.ReportService;
import com.cspinformatique.kubik.server.model.purchase.PurchaseOrder;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;

@Controller
@RequestMapping("/purchaseOrder")
public class PurchaseOrderController {
	@Autowired
	private PurchaseOrderService purchaseOrderService;

	@Autowired
	private ReportService reportService;

	@RequestMapping("/confirmOrders")
	public void confirmAllOrders() {
		this.purchaseOrderService.confirmAllOrders();
	}

	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Page<PurchaseOrder> findAll(@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "50") Integer resultPerPage,
			@RequestParam(required = false) Direction direction, @RequestParam(defaultValue = "date") String sortBy) {
		return this.purchaseOrderService
				.findAll(new PageRequest(page, resultPerPage, direction != null ? direction : Direction.DESC, sortBy));
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody PurchaseOrder findOne(@PathVariable int id) {
		return this.purchaseOrderService.findOne(id);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{id}/report", method = RequestMethod.GET, produces = "application/pdf")
	public byte[] generateOrderPdf(@PathVariable int id, HttpServletResponse response) {
		try {
			return JasperExportManager.exportReportToPdf(
					this.reportService.generatePurchaseOrderReport(this.purchaseOrderService.findOne(id)));
		} catch (JRException ex) {
			throw new RuntimeException(ex);
		}
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String getOrderDetailsPage(@PathVariable int id, Model model) {
		model.addAttribute("id", id);

		return "purchase/order/order-details";
	}

	@RequestMapping(method = RequestMethod.GET)
	public String getOrdersPage() {
		return "purchase/order/orders";
	}

	@RequestMapping("/fixSubmitedDate")
	public String fixSubmitedDate() {
		this.purchaseOrderService.fixSubmitedDate();

		return "purchase/order/orders";
	}

	@RequestMapping(method = { RequestMethod.POST, RequestMethod.PUT }, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody PurchaseOrder save(@RequestBody PurchaseOrder purchaseOrder) {
		return this.purchaseOrderService.save(purchaseOrder);
	}
}
