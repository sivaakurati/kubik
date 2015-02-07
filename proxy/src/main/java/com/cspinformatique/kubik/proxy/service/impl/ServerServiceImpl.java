package com.cspinformatique.kubik.proxy.service.impl;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.cspinformatique.kubik.print.model.ReceiptPrintJob;
import com.cspinformatique.kubik.proxy.service.ServerService;
import com.cspinformatique.kubik.proxy.util.RestUtil;
import com.cspinformatique.kubik.sales.model.Invoice;

@Service
public class ServerServiceImpl implements ServerService {
	@Autowired
	private RestTemplate restTemplate;

	@Value("${kubik.proxy.server.url}")
	private String serverUrl;
	
	@Value("${kubik.proxy.server.password}")
	private String kubikPassword;

	@Value("${kubik.proxy.server.username}")
	private String kubikUsername;

	@Override
	public void deleteReceiptPrintJob(int id) {
		this.restTemplate.exchange(this.serverUrl + "/invoice/" + id
				+ "/receipt", HttpMethod.DELETE, new HttpEntity<Void>(null,
				RestUtil.createBasicAuthHeader(kubikUsername, kubikPassword)),
				Void.class);
	}

	@Override
	public Iterable<ReceiptPrintJob> findPendingReceiptPrintJob() {
		return Arrays.asList(this.restTemplate
				.exchange(
						serverUrl + "/print.jsonm",
						HttpMethod.GET,
						new HttpEntity<Void>(null, RestUtil
								.createBasicAuthHeader(kubikUsername,
										kubikPassword)),
						ReceiptPrintJob[].class).getBody());
	}

	@Override
	public byte[] loadInvoiceReceiptData(Invoice invoice) {
		return this.restTemplate.exchange(
				serverUrl + "/print",
				HttpMethod.GET,
				new HttpEntity<Void>(null, RestUtil.createBasicAuthHeader(
						kubikUsername, kubikPassword)), byte[].class).getBody();
	}
}
