package com.cspinformatique.kubik.domain.sales.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import com.cspinformatique.kubik.model.sales.CashRegisterSession;
import com.cspinformatique.kubik.model.sales.Invoice;
import com.cspinformatique.kubik.model.sales.InvoiceDetail;
import com.cspinformatique.kubik.model.sales.InvoiceStatus;

public interface InvoiceRepository extends
		PagingAndSortingRepository<Invoice, Integer> {
	List<Invoice> findByCashRegisterSessionAndStatus(
			CashRegisterSession session, InvoiceStatus invoiceStatus);

	Invoice findByNumber(String number);

	List<Invoice> findByPaidDateBetweenAndStatus(Date startPaidDate,
			Date startEndDate, InvoiceStatus status);

	Page<Invoice> findByPaidDateBetweenAndStatus(Date startPaidDate,
			Date startEndDate, InvoiceStatus status, Pageable pageable);

	Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);
	
	Page<Invoice> findByStatusAndNumberIsNotNull(InvoiceStatus status, Pageable pageable);

	@Query("SELECT invoiceDetail FROM Invoice invoice, InvoiceDetail invoiceDetail where invoice = invoiceDetail.invoice AND invoice.id = :invoiceId AND invoiceDetail.product.ean13 = :ean13")
	InvoiceDetail findDetailByInvoiceIdAndProductEan13(
			@Param("invoiceId") int invoiceId, @Param("ean13") String ean13);

	@Query("SELECT id FROM Invoice invoice WHERE id > :id")
	Page<Integer> findIdByIdGreaterThan(@Param("id") int id, Pageable pageable);

	@Query("SELECT id FROM Invoice invoice WHERE id < :id")
	Page<Integer> findIdByIdLessThan(@Param("id") int id, Pageable pageable);
	
	@Query("SELECT sum(detail.quantity) FROM InvoiceDetail detail WHERE detail.product.id = :productId AND detail.invoice.status.type = 'PAID'")
	Double findProductQuantityPurchased(@Param("productId") int productId);
}