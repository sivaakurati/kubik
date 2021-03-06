package com.cspinformatique.kubik.server.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cspinformatique.kubik.server.domain.dilicom.batch.itemProcessor.ReferenceItemProcessor;
import com.cspinformatique.kubik.server.domain.dilicom.batch.reader.DilicomReferenceReader;
import com.cspinformatique.kubik.server.domain.dilicom.batch.writer.DilicomReferenceWriter;
import com.cspinformatique.kubik.server.domain.dilicom.model.Reference;
import com.cspinformatique.kubik.server.domain.dilicom.model.ReferenceDTO;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
	public static final String IMPORT_DILICOM_REFERENCES_JOB = "importDilicomReferencesJob";

	public static final String NOTIFICATION_FILE_INTEGRATION_STEP = "notificationFileIntegrationStep";

	@Autowired
	private DilicomReferenceReader dilicomReferenceReader;

	@Autowired
	private DilicomReferenceWriter dilicomReferenceWriter;

	@Autowired
	private ReferenceItemProcessor referenceItemProcessor;

	@Autowired
	StepBuilderFactory stepBuilderFactory;

	public @Bean Job importDilicomReferencesJob(JobBuilderFactory jobs) {
		return jobs.get(IMPORT_DILICOM_REFERENCES_JOB)
				.incrementer(new RunIdIncrementer())
				.flow(notificationFileIntegrationStep()).end().build();
	}

	public @Bean Step notificationFileIntegrationStep() {
		return stepBuilderFactory.get(NOTIFICATION_FILE_INTEGRATION_STEP)
				.<ReferenceDTO, Reference> chunk(1000)
				.reader(dilicomReferenceReader).writer(dilicomReferenceWriter)
				.processor(referenceItemProcessor).build();
	}
}
