package com.cspinformatique.kubik.server.domain.sales.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.Transactional;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.cspinformatique.kubik.server.domain.sales.repository.DailyReportRepository;
import com.cspinformatique.kubik.server.domain.sales.service.CustomerCreditService;
import com.cspinformatique.kubik.server.domain.sales.service.DailyReportService;
import com.cspinformatique.kubik.server.domain.sales.service.InvoiceService;
import com.cspinformatique.kubik.server.model.sales.CustomerCredit;
import com.cspinformatique.kubik.server.model.sales.DailyReport;
import com.cspinformatique.kubik.server.model.sales.Invoice;
import com.cspinformatique.kubik.server.model.sales.Payment;
import com.cspinformatique.kubik.server.model.sales.PaymentMethod;
import com.cspinformatique.kubik.server.model.sales.SalesByPaymentMethod;

@Service
public class DailyReportServiceImpl implements DailyReportService {
	@Autowired
	private CustomerCreditService customerCreditService;

	@Autowired
	private DailyReportRepository dailyReportRepository;

	@Autowired
	private InvoiceService invoiceService;

	@Override
	public Page<DailyReport> findAll(Pageable pageable) {
		return this.dailyReportRepository.findAll(pageable);
	}

	private DailyReport findLastDailyReport() {
		Page<DailyReport> page = this.findAll(new PageRequest(0, 1,
				Direction.DESC, "date"));

		if (page.getContent().size() == 0) {
			return null;
		}

		return page.getContent().get(0);
	}

	@Override
	public DailyReport findOne(int id) {
		return this.dailyReportRepository.findOne(id);
	}

	@Override
	@Transactional
	public void generateDailyReport(Date date) {
		date = LocalDate.fromDateFields(date).toDateTimeAtStartOfDay().toDate();

		DailyReport dailyReport = this.dailyReportRepository.findByDate(date);

		if (dailyReport == null) {
			dailyReport = new DailyReport();
			dailyReport.setDate(date);
		}

		int salesCount = 0;
		int returnCount = 0;

		double salesAmountTaxIn = 0d;
		double salesAmountTaxOut = 0d;

		Map<PaymentMethod.Types, SalesByPaymentMethod> paymentsMap = new HashMap<PaymentMethod.Types, SalesByPaymentMethod>();

		for (Invoice invoice : this.invoiceService.findByPaidDate(date)) {
			++salesCount;
			salesAmountTaxIn += invoice.getTotalAmount();
			salesAmountTaxOut += invoice.getTotalTaxLessAmount();

			for (Payment payment : invoice.getPayments()) {
				SalesByPaymentMethod salesByPaymentMethod = paymentsMap
						.get(PaymentMethod.Types.valueOf(payment
								.getPaymentMethod().getType()));

				if (salesByPaymentMethod == null) {
					salesByPaymentMethod = new SalesByPaymentMethod(null,
							payment.getPaymentMethod(), 0, 0d);

					paymentsMap.put(
							PaymentMethod.Types.valueOf(payment
									.getPaymentMethod().getType().toString()),
							salesByPaymentMethod);
				}

				salesByPaymentMethod.setSalesCount(salesByPaymentMethod
						.getSalesCount() + 1);
				salesByPaymentMethod.setPaymentsAmount(salesByPaymentMethod
						.getPaymentsAmount() + payment.getAmount());

				if (payment.getPaymentMethod().getType()
						.equals(PaymentMethod.Types.CASH.toString())) {
					salesByPaymentMethod.setPaymentsAmount(salesByPaymentMethod
							.getPaymentsAmount() - invoice.getAmountReturned());
				}
			}
		}

		for (CustomerCredit customerCredit : this.customerCreditService
				.findByCompleteDate(date)) {
			++returnCount;

			salesAmountTaxIn -= customerCredit.getTotalAmount();
			salesAmountTaxOut -= customerCredit.getTotalTaxLessAmount();

			SalesByPaymentMethod salesByPaymentMethod = paymentsMap
					.get(PaymentMethod.Types.valueOf(customerCredit
							.getPaymentMethod().getType()));

			if (salesByPaymentMethod == null) {
				salesByPaymentMethod = new SalesByPaymentMethod(null,
						customerCredit.getPaymentMethod(), 0, 0d);

				paymentsMap.put(
						PaymentMethod.Types.valueOf(customerCredit
								.getPaymentMethod().getType().toString()),
						salesByPaymentMethod);
			}

			salesByPaymentMethod.setSalesCount(salesByPaymentMethod
					.getSalesCount() - 1);
			salesByPaymentMethod.setPaymentsAmount(salesByPaymentMethod
					.getPaymentsAmount() - customerCredit.getTotalAmount());
		}

		dailyReport.setSalesCount(salesCount);
		dailyReport.setReturnCount(returnCount);
		dailyReport.setSalesAmountTaxIn(salesAmountTaxIn);
		dailyReport.setSalesAmountTaxOut(salesAmountTaxOut);
		dailyReport
				.setSalesByPaymentMethods(new ArrayList<SalesByPaymentMethod>(
						paymentsMap.values()));

		this.dailyReportRepository.save(dailyReport);
	}

	@Override
	public void initializeDailyReports() {
		Invoice firstInvoice = this.invoiceService.findFirstPaidInvoice();
		DailyReport lastDailyReport = this.findLastDailyReport();

		boolean completed = false;
		DateTime date = LocalDate.now().toDateTimeAtStartOfDay();
		do {
			this.generateDailyReport(date.toDate());

			/*
			 * Stops the generation if the last computed date equals the last
			 * generated daily report or the first invoice ever paid.
			 */
			if ((lastDailyReport != null && DateTimeComparator
					.getDateOnlyInstance().compare(
							LocalDate.fromDateFields(lastDailyReport.getDate())
									.toDateTimeAtStartOfDay(), date) == 0)
					|| DateTimeComparator.getDateOnlyInstance().compare(
							LocalDate
									.fromDateFields(firstInvoice.getPaidDate())
									.toDateTimeAtStartOfDay(), date) == 0) {
				completed = true;
			}

			date = date.minusDays(1);
		} while (!completed);
	}
}
