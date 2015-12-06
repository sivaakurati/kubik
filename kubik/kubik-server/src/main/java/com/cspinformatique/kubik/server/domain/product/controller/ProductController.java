package com.cspinformatique.kubik.server.domain.product.controller;

import java.util.Calendar;
import java.util.Date;

import javax.annotation.Resource;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cspinformatique.kubik.server.domain.product.service.CategoryService;
import com.cspinformatique.kubik.server.domain.product.service.ProductImageService;
import com.cspinformatique.kubik.server.domain.product.service.ProductService;
import com.cspinformatique.kubik.server.domain.product.service.ProductStatsService;
import com.cspinformatique.kubik.server.domain.product.service.SupplierService;
import com.cspinformatique.kubik.server.domain.purchase.service.ReceptionDetailService;
import com.cspinformatique.kubik.server.domain.purchase.service.RmaDetailService;
import com.cspinformatique.kubik.server.domain.sales.service.CustomerCreditDetailService;
import com.cspinformatique.kubik.server.domain.sales.service.InvoiceDetailService;
import com.cspinformatique.kubik.server.domain.warehouse.service.InventoryCountService;
import com.cspinformatique.kubik.server.model.product.Product;
import com.cspinformatique.kubik.server.model.product.ProductImage;
import com.cspinformatique.kubik.server.model.product.ProductImageSize;
import com.cspinformatique.kubik.server.model.product.ProductStats;
import com.cspinformatique.kubik.server.model.purchase.Reception;
import com.cspinformatique.kubik.server.model.purchase.ReceptionDetail;
import com.cspinformatique.kubik.server.model.purchase.Rma;
import com.cspinformatique.kubik.server.model.purchase.RmaDetail;
import com.cspinformatique.kubik.server.model.sales.CustomerCredit;
import com.cspinformatique.kubik.server.model.sales.CustomerCreditDetail;
import com.cspinformatique.kubik.server.model.sales.InvoiceDetail;
import com.cspinformatique.kubik.server.model.sales.InvoiceStatus;
import com.cspinformatique.kubik.server.model.warehouse.InventoryCount;

@Controller
@RequestMapping("/product")
public class ProductController {
	@Resource
	private CategoryService categoryService;

	@Resource
	private ProductService productService;

	@Resource
	private SupplierService supplierService;

	@Resource
	private CustomerCreditDetailService customerCreditDetailService;

	@Resource
	private InventoryCountService inventoryCountService;

	@Resource
	private InvoiceDetailService invoiceDetailService;

	@Resource
	private ProductImageService productImageService;

	@Resource
	private ProductStatsService productsStatsService;

	@Resource
	private ReceptionDetailService receptionDetailService;

	@Resource
	private RmaDetailService rmaDetailService;

	@RequestMapping(method = RequestMethod.GET, params = "category", produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Integer countByCategory(@RequestParam Integer category) {
		return productService.countByCategory(category != null ? categoryService.findOne(category) : null);
	}

	@RequestMapping(params = "ean13", produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Iterable<Product> findByEan13(@RequestParam String ean13) {
		return this.productService.findByEan13(ean13);
	}

	@RequestMapping(params = { "ean13", "supplierEan13" }, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Product findByEan13AndSupplierEan13(@RequestParam String ean13,
			@RequestParam String supplierEan13) {
		return this.productService.findByEan13AndSupplier(ean13, this.supplierService.findByEan13(supplierEan13));
	}

	@RequestMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Product findOne(@PathVariable int id) {
		return this.productService.findOne(id);
	}

	@RequestMapping(value = "/{id}/productStats", produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ProductStats findProductStats(@PathVariable int id,
			@RequestParam(required = false) Date startDate, @RequestParam(required = false) Date endDate) {
		if (startDate == null) {
			startDate = new Date(0);
		}
		if (endDate == null) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, 1000);

			endDate = cal.getTime();
		}

		return this.productsStatsService.findByProductId(id, startDate, endDate);
	}

	@RequestMapping(value = "/{productId}/customerCredit", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Page<CustomerCreditDetail> findProductCustomerCredits(@PathVariable("productId") int productId,
			@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer resultPerPage,
			@RequestParam(required = false) Direction direction,
			@RequestParam(defaultValue = "extendedLabel") String sortBy) {
		return this.customerCreditDetailService.findByProductAndCustomerCreditStatus(
				this.productService.findOne(productId), CustomerCredit.Status.COMPLETED,
				new PageRequest(page, resultPerPage, direction != null ? direction : Direction.ASC, sortBy));
	}

	@RequestMapping(value = "/{productId}/inventoryCount", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Page<InventoryCount> findProductInventoryCounts(@PathVariable("productId") int productId,
			@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer resultPerPage,
			@RequestParam(defaultValue = "DESC") Direction direction,
			@RequestParam(defaultValue = "dateCounted") String sortBy) {
		return inventoryCountService.findByProduct(productService.findOne(productId),
				new PageRequest(page, resultPerPage, direction != null ? direction : Direction.ASC, sortBy));
	}

	@RequestMapping(value = "/{productId}/invoice", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Page<InvoiceDetail> findProductInvoices(@PathVariable("productId") int productId,
			@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer resultPerPage,
			@RequestParam(defaultValue = "DESC") Direction direction,
			@RequestParam(defaultValue = "invoice.paidDate") String sortBy) {
		return this.invoiceDetailService.findByProductAndInvoiceStatus(this.productService.findOne(productId),
				new InvoiceStatus(InvoiceStatus.Types.PAID.name(), null),
				new PageRequest(page, resultPerPage, direction, sortBy));
	}

	@RequestMapping(value = "/{productId}/reception", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Page<ReceptionDetail> findProductPurchaseOrders(@PathVariable("productId") int productId,
			@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer resultPerPage,
			@RequestParam(required = false) Direction direction,
			@RequestParam(defaultValue = "extendedLabel") String sortBy) {
		return this.receptionDetailService.findByProductAndReceptionStatus(this.productService.findOne(productId),
				Reception.Status.CLOSED,
				new PageRequest(page, resultPerPage, direction != null ? direction : Direction.ASC, sortBy));
	}

	@RequestMapping(value = "/{productId}/rma", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Page<RmaDetail> findProductRmas(@PathVariable("productId") int productId,
			@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer resultPerPage,
			@RequestParam(required = false) Direction direction,
			@RequestParam(defaultValue = "extendedLabel") String sortBy) {
		return this.rmaDetailService.findByProductAndRmaStatus(this.productService.findOne(productId),
				Rma.Status.SHIPPED,
				new PageRequest(page, resultPerPage, direction != null ? direction : Direction.ASC, sortBy));
	}

	@RequestMapping(method = RequestMethod.GET, params = { "random",
			"category" }, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Product findRandomByCategory(@RequestParam Integer category) {
		return productService.findRandomByCategory(category != null ? categoryService.findOne(category) : null);
	}

	@RequestMapping(method = RequestMethod.GET, params = { "random", "category" }, produces = MediaType.TEXT_HTML_VALUE)
	public String getNonCategorizedProductsPage() {
		return "product/non-categorized-product";
	}

	@RequestMapping(params = "card", produces = MediaType.TEXT_HTML_VALUE)
	public String getProductCardPage() {
		return "product/product-card :: product-card";
	}

	@RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
	public String getProductsPage() {
		return "product/products-page";
	}

	@RequestMapping(value = "/{id}/image/{size}", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<InputStreamResource> loadProductImage(@PathVariable int id,
			@PathVariable ProductImageSize size) {
		ProductImage productImage = productImageService.findByProductAndSize(productService.findOne(id), size);

		if (productImage != null) {
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentLength(productImage.getContentLength());
			
			return new ResponseEntity<InputStreamResource>(
					new InputStreamResource(productImageService.loadInputStream(productService.findOne(id), size)),
					httpHeaders, HttpStatus.OK);
		} else {
			return new ResponseEntity<InputStreamResource>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);
		}
	}

	@RequestMapping(value = "/{productId}", params = { "targetProductId", "mergeProduct" })
	public void mergeProducts(@PathVariable int productId, @RequestParam int targetProductId) {
		this.productService.mergeProduct(this.findOne(productId), this.findOne(targetProductId));
	}

	@RequestMapping(method = { RequestMethod.POST, RequestMethod.PUT }, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Product save(@RequestBody Product product) {
		return this.productService.save(product);
	}

	@RequestMapping(method = RequestMethod.GET, params = "search", produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Page<Product> search(@RequestParam(defaultValue = "") String query,
			@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "50") Integer resultPerPage,
			@RequestParam(required = false) Direction direction,
			@RequestParam(defaultValue = "extendedLabel") String sortBy) {
		return this.productService.search(query,
				new PageRequest(page, resultPerPage, direction != null ? direction : Direction.ASC, sortBy));
	}
}