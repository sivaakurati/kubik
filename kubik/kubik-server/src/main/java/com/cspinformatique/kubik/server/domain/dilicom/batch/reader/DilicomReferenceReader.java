package com.cspinformatique.kubik.server.domain.dilicom.batch.reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.mapping.PatternMatchingCompositeLineMapper;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Component;

import com.cspinformatique.kubik.server.domain.dilicom.batch.mapper.ReferenceDeletedMappingEnum;
import com.cspinformatique.kubik.server.domain.dilicom.batch.mapper.ReferenceExtractionMappingEnum;
import com.cspinformatique.kubik.server.domain.dilicom.batch.mapper.ReferenceNotificationMappingEnum;
import com.cspinformatique.kubik.server.domain.dilicom.model.ReferenceDTO;

@Component
@StepScope
public class DilicomReferenceReader extends
		FlatFileItemReader<ReferenceDTO> implements InitializingBean {
	@Value("#{jobParameters['filemame']}")
	private String fileName;

	public DilicomReferenceReader() {
		super();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.setResource(new PathResource(fileName));

		this.setStrict(false);

		PatternMatchingCompositeLineMapper<ReferenceDTO> lineMapper = new PatternMatchingCompositeLineMapper<ReferenceDTO>();

		HashMap<String, LineTokenizer> tokenizers = new HashMap<String, LineTokenizer>();

		tokenizers.put("C*", new FixedLengthTokenizer() {
			{
				setNames(getDilicomNotificationNames());
				setColumns(getDilicomNotificationRanges());
				setStrict(false);
			}
		});
		tokenizers.put("M*", new FixedLengthTokenizer() {
			{
				setNames(getDilicomNotificationNames());
				setColumns(getDilicomNotificationRanges());
				setStrict(false);
			}
		});
		tokenizers.put("E*", new FixedLengthTokenizer() {
			{
				setNames(getDilicomExtractionNames());
				setColumns(getDilicomExtractionRanges());
				setStrict(false);
			}
		});
		tokenizers.put("S*", new FixedLengthTokenizer() {
			{
				setNames(getDilicomDeletedNames());
				setColumns(getDilicomDeletedRanges());
				setStrict(false);
			}
		});
		tokenizers.put("*", new FixedLengthTokenizer() {
			{
				setNames(new String[] {"movementCode"});
				setColumns(new Range[] {new Range(1,1)});
				setStrict(false);
			}
		});

		lineMapper.setTokenizers(tokenizers);

		HashMap<String, FieldSetMapper<ReferenceDTO>> fieldSetMappers = new HashMap<String, FieldSetMapper<ReferenceDTO>>();

		fieldSetMappers.put("C*",
				new BeanWrapperFieldSetMapper<ReferenceDTO>() {
					{
						setTargetType(ReferenceDTO.class);
					}
				});
		fieldSetMappers.put("M*",
				new BeanWrapperFieldSetMapper<ReferenceDTO>() {
					{
						setTargetType(ReferenceDTO.class);
					}
				});
		fieldSetMappers.put("E*",
				new BeanWrapperFieldSetMapper<ReferenceDTO>() {
					{
						setTargetType(ReferenceDTO.class);
					}
				});
		fieldSetMappers.put("S*",
				new BeanWrapperFieldSetMapper<ReferenceDTO>() {
					{
						setTargetType(ReferenceDTO.class);
					}
				});
		fieldSetMappers.put("*",
				new BeanWrapperFieldSetMapper<ReferenceDTO>() {
					{
						setTargetType(ReferenceDTO.class);
					}
				});

		lineMapper.setFieldSetMappers(fieldSetMappers);

		this.setLineMapper(lineMapper);

		super.afterPropertiesSet();
	}

	public static String[] getDilicomDeletedNames() {
		List<String> names = new ArrayList<String>();

		for (ReferenceDeletedMappingEnum mapping : ReferenceDeletedMappingEnum
				.values()) {
			names.add(mapping.getFieldName());
		}

		return names.toArray(new String[] {});
	}

	public static Range[] getDilicomDeletedRanges() {
		List<Range> ranges = new ArrayList<Range>();
		for (ReferenceDeletedMappingEnum mapping : ReferenceDeletedMappingEnum
				.values()) {
			ranges.add(new Range(mapping.getStartRange(), mapping.getEndRange()));
		}

		return ranges.toArray(new Range[] {});
	}

	public static String[] getDilicomExtractionNames() {
		List<String> names = new ArrayList<String>();

		for (ReferenceExtractionMappingEnum mapping : ReferenceExtractionMappingEnum
				.values()) {
			names.add(mapping.getFieldName());
		}

		return names.toArray(new String[] {});
	}

	public static Range[] getDilicomExtractionRanges() {
		List<Range> ranges = new ArrayList<Range>();
		for (ReferenceExtractionMappingEnum mapping : ReferenceExtractionMappingEnum
				.values()) {
			ranges.add(new Range(mapping.getStartRange(), mapping.getEndRange()));
		}

		return ranges.toArray(new Range[] {});
	}

	public static String[] getDilicomNotificationNames() {
		List<String> names = new ArrayList<String>();

		for (ReferenceNotificationMappingEnum mapping : ReferenceNotificationMappingEnum
				.values()) {
			names.add(mapping.getFieldName());
		}

		return names.toArray(new String[] {});
	}

	public static Range[] getDilicomNotificationRanges() {
		List<Range> ranges = new ArrayList<Range>();
		for (ReferenceNotificationMappingEnum mapping : ReferenceNotificationMappingEnum
				.values()) {
			ranges.add(new Range(mapping.getStartRange(), mapping.getEndRange()));
		}

		return ranges.toArray(new Range[] {});
	}
}
