package com.cspinformatique.kubik.domain.product.service.impl;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.cspinformatique.kubik.domain.product.repository.ProductRepository;
import com.cspinformatique.kubik.domain.product.service.ImageService;
import com.cspinformatique.kubik.domain.product.service.ProductService;
import com.cspinformatique.kubik.domain.product.service.SupplierService;
import com.cspinformatique.kubik.domain.purchase.model.PurchaseOrder.Status;
import com.cspinformatique.kubik.domain.purchase.service.PurchaseOrderService;
import com.cspinformatique.kubik.domain.reference.model.Reference;
import com.cspinformatique.kubik.domain.reference.service.ReferenceService;
import com.cspinformatique.kubik.model.product.AvailabilityCode;
import com.cspinformatique.kubik.model.product.BarcodeType;
import com.cspinformatique.kubik.model.product.PriceType;
import com.cspinformatique.kubik.model.product.Product;
import com.cspinformatique.kubik.model.product.ProductType;
import com.cspinformatique.kubik.model.product.ReturnType;
import com.cspinformatique.kubik.model.product.Supplier;

@Service
public class ProductServiceImpl implements ProductService, InitializingBean {
	private static final Logger logger = LoggerFactory
			.getLogger(ProductServiceImpl.class);

	@Autowired
	private ImageService imageService;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private PurchaseOrderService purchaseOrderService;

	@Autowired
	private ReferenceService referenceServive;

	@Autowired
	private SupplierService supplierService;

	@Resource
	private Environment env;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!env.getRequiredProperty("kubik.environment").equals("production")) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(15000);

						generateProductsFromImportedReferences();

						generateMissingReferences();
					} catch (Exception ex) {
						logger.error(
								"Could not generate products from imported references",
								ex);
					}
				}
			}).start();
		}
	}

	@Override
	public Product buildProductFromReference(Reference reference) {
		Supplier supplier = this.supplierService.findByEan13(reference
				.getSupplierEan13());
		Integer productId = null;
		if (reference.isImportedInKubik()) {
			Product product = this.findByEan13AndSupplier(reference.getEan13(),
					supplier);

			if (product != null) {
				productId = product.getId();
			}
		}

		return new Product(productId, reference.getEan13(), supplier,
				AvailabilityCode.parseByCode(reference.getAvailability()),
				reference.getPriceType() != null ? PriceType
						.parseByCode(reference.getPriceType()) : null,
				reference.getPriceTaxIn(), reference.getSchoolbook(),
				reference.getTvaRate1(), reference.getPriceTaxOut1(),
				reference.getTvaRate2(), reference.getPriceTaxOut2(),
				reference.getTvaRate3(), reference.getPriceTaxOut3(), 0d,
				reference.getReturnType() != null ? ReturnType
						.parseByCode(reference.getReturnType()) : null,
				reference.getAvailableForOrder(), reference.getDatePublished(),
				reference.getProductType() != null ? ProductType
						.parseByCode(reference.getProductType()) : null,
				reference.getPublishEndDate(), reference.getStandardLabel(),
				reference.getCashRegisterLabel(), reference.getThickness(),
				reference.getWidth(), reference.getHeight(),
				reference.getWeight(), reference.getExtendedLabel(),
				reference.getPublisher(), reference.getCollection(),
				reference.getAuthor(), reference.getPublisherPresentation(),
				reference.getIsbn(), reference.getSupplierReference(),
				reference.getCollectionReference(), reference.getTheme(),
				reference.getPublisherIsnb(),
				reference.getReplacingAReference(),
				reference.getReplacedByAReference(),
				reference.getReplacesEan13(), reference.getReplacedByEan13(),
				reference.getOrderableByUnit(),
				reference.getBarcodeType() != null ? BarcodeType
						.parseByCode(reference.getBarcodeType()) : null,
				reference.getMainReference(),
				reference.getSecondaryReference(),
				reference.getReferencesCount(), 0f, null, true);
	}

	@Override
	public Page<Product> findAll(Pageable pageable) {
		Page<Product> productPage = this.productRepository.findAll(pageable);

		for (Product product : productPage.getContent()) {
			this.calculateImageEncryptedKey(product);
		}

		return productPage;
	}
	
	@Override
	public Iterable<Product> findByEan13(String ean13){
		return this.productRepository.findByEan13(ean13);
	}

	@Override
	public Product findByEan13AndSupplier(String ean13, Supplier supplier) {
		return this.productRepository.findByEan13AndSupplier(ean13, supplier);
	}

	@Override
	public Iterable<Product> findBySupplier(Supplier supplier) {
		return this.productRepository.findBySupplier(supplier);
	}

	@Override
	public Product findOne(int id) {
		Product product = this.productRepository.findOne(id);

		this.calculateImageEncryptedKey(product);

		return product;
	}

	private void calculateImageEncryptedKey(Product product) {
		if (product.getSupplier() != null) {
			product.setImageEncryptedKey(this.imageService.getEncryptedUrl(
					product.getEan13(), product.getSupplier().getEan13()));
		}
	}

	private void calculateTaxesAmounts(Product product) {
		if (product.getTvaRate1() != null) {
			product.setPriceTaxOut1(Precision.round(product.getPriceTaxIn()
					/ (1 + product.getTvaRate1() / 100), 2));
		}
		if (product.getTvaRate2() != null) {
			product.setPriceTaxOut1(Precision.round(product.getPriceTaxIn()
					/ (1 + product.getTvaRate2() / 100), 2));
		}
		if (product.getTvaRate3() != null) {
			product.setPriceTaxOut1(Precision.round(product.getPriceTaxIn()
					/ (1 + product.getTvaRate3() / 100), 2));
		}
	}

	@Override
	public Product generateProductIfNotFound(String ean13, String supplierEan13) {
		Supplier supplier = this.supplierService.findByEan13(supplierEan13);

		if (supplier == null) {
			supplier = this.supplierService
					.generateSupplierIfNotFound(supplierEan13);
		}

		Product product = this.findByEan13AndSupplier(ean13, supplier);
		if (product == null) {
			Reference reference = this.referenceServive
					.findByEan13AndSupplierEan13(ean13, supplierEan13);

			logger.info("Generating product " + ean13 + " from supplier "
					+ supplierEan13 + ".");

			product = this.save(this.buildProductFromReference(reference));

			reference.setImportedInKubik(true);

			referenceServive.save(reference);
		}

		return product;
	}

	private void generateMissingReferences() {
		for (int productId : this.productRepository
				.findIdByDilicomReference(false)) {
			this.save(this.findOne(productId));
		}
	}

	private void generateProductsFromImportedReferences() {
		Pageable pageRequest = new PageRequest(0, 100);
		Page<Reference> page = null;
		do {
			for (Reference reference : this.referenceServive
					.findByImportedInKubik(true, pageRequest)) {
				this.generateProductIfNotFound(reference.getEan13(),
						reference.getSupplierEan13());
			}

			pageRequest = pageRequest.next();
		} while (page != null && page.hasNext());
	}

	@Override
	@Transactional
	public Product save(Product product) {
		boolean updatePurchaseOrders = false;
		Product oldVersion = null;

		if (product.getId() != null) {
			oldVersion = this.findOne(product.getId());
		}

		// Checks if the supplier has changed.
		if (oldVersion != null
				&& !product.getSupplier().getEan13()
						.equals(oldVersion.getSupplier().getEan13())) {
			// Makes sure no purchase orders exists for the product.
			if (this.purchaseOrderService.findByProduct(product).iterator()
					.hasNext()) {
				throw new RuntimeException(
						"Product can't change supplier as purchase orders exists for it.");
			}

			// Delete the reference from the old supplier.
			this.referenceServive.delete(oldVersion.getEan13(), oldVersion
					.getSupplier().getEan13());
		}

		// checks if the prices needs to be calculated.
		if (oldVersion == null
				|| product.getPriceTaxIn() != oldVersion.getPriceTaxIn()) {
			this.calculateTaxesAmounts(product);

			updatePurchaseOrders = true;
		}

		Reference newReference = null;
		if (!product.isDilicomReference()) {
			// Checks if a dilicom reference exists for the new product.
			Reference existingReference = this.referenceServive
					.find(product.getEan13(), product.getSupplier().getEan13(),
							false);

			if (existingReference != null) {
				// Overwrite product with dilicom reference.
				product = this.buildProductFromReference(existingReference);

				existingReference.setImportedInKubik(true);
				
				newReference = existingReference;
			}
		}
		
		if(newReference == null){
			newReference = this.referenceServive
					.buildReferenceFromProduct(product);
		}
		
		this.referenceServive.save(newReference);

		// Saves the product.
		product = this.productRepository.save(product);

		if (updatePurchaseOrders) {
			this.purchaseOrderService.save(this.purchaseOrderService
					.findByProductAndStatus(product, Status.DRAFT));
		}

		return product;
	}

	@Override
	public Page<Product> search(String query, Pageable pageable) {
		Page<Product> page = this.productRepository.search("%" + query + "%",
				pageable);

		for (Product product : page.getContent()) {
			this.calculateImageEncryptedKey(product);
		}

		return page;
	}
}
