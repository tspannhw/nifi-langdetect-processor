/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dataflowdeveloper.langdetect;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

@Tags({ "nlpprocessor, apache opennlp, nlp, natural language processing" })
@CapabilityDescription("Run OpenNLP Natural Language Processing for Name, Location, Date Finder")
@SeeAlso({})
@ReadsAttributes({
		@ReadsAttribute(attribute = "sentence", description = "text to analyze, this is optional can pass in a flowfile") })
@WritesAttributes({
		@WritesAttribute(attribute = "langdetectTika", description = "Returns Apache Tika - Optimaize - Language Detection") })
public class LangDetectProcessor extends AbstractProcessor {

	public static final String ATTRIBUTE_INPUT_NAME = "sentence";
	public static final String PROPERTY_NAME_EXTRA = "Extra Resources";

	public static final PropertyDescriptor MY_PROPERTY = new PropertyDescriptor.Builder().name(ATTRIBUTE_INPUT_NAME)
			.displayName("Sentence").expressionLanguageSupported(true)
			.addValidator(StandardValidators.NON_BLANK_VALIDATOR)
			.description("A sentence to parse, such as a Tweet.  This can also be a Flow File.").required(false)
			.build();

	public static final PropertyDescriptor EXTRA_RESOURCE = new PropertyDescriptor.Builder().name(PROPERTY_NAME_EXTRA)
			.description(
					"The path to one or more Apache OpenNLP Models to add to the classpath. See http://opennlp.sourceforge.net/models-1.5/")
			.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).expressionLanguageSupported(true).required(true)
			.defaultValue("src/main/resources/META-INF/input").dynamic(true).build();

	public static final Relationship REL_SUCCESS = new Relationship.Builder().name("success")
			.description("Successfully extracted.").build();

	public static final Relationship REL_FAILURE = new Relationship.Builder().name("failure")
			.description("Failed to extract.").build();

	private List<PropertyDescriptor> descriptors;

	private Set<Relationship> relationships;

	private LangDetectService service;

	@Override
	protected void init(final ProcessorInitializationContext context) {
		final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
		descriptors.add(MY_PROPERTY);
		descriptors.add(EXTRA_RESOURCE);
		this.descriptors = Collections.unmodifiableList(descriptors);

		final Set<Relationship> relationships = new HashSet<Relationship>();
		relationships.add(REL_SUCCESS);
		relationships.add(REL_FAILURE);
		this.relationships = Collections.unmodifiableSet(relationships);
	}

	@Override
	public Set<Relationship> getRelationships() {
		return this.relationships;
	}

	@Override
	public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		return descriptors;
	}

	@OnScheduled
	public void onScheduled(final ProcessContext context) {
		return;
	}

	@Override
	public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
		FlowFile flowFile = session.get();
		if (flowFile == null) {
			flowFile = session.create();
		}
		try {
			flowFile.getAttributes();

			service = new LangDetectService();

			String sentence = flowFile.getAttribute(ATTRIBUTE_INPUT_NAME);
			String sentence2 = context.getProperty(ATTRIBUTE_INPUT_NAME).evaluateAttributeExpressions(flowFile)
					.getValue();

			if (sentence == null) {
				sentence = sentence2;
			}

			try {
				// if they pass in a sentence do that instead of flowfile
				if (sentence == null) {
					final AtomicReference<String> contentsRef = new AtomicReference<>(null);

					session.read(flowFile, new InputStreamCallback() {
						@Override
						public void process(final InputStream input) throws IOException {
							final String contents = IOUtils.toString(input, "UTF-8");
							contentsRef.set(contents);
						}
					});

					// use this as our text
					if (contentsRef.get() != null) {
						sentence = contentsRef.get();
					}
				}

				//flowFile = session.putAttribute(flowFile, "langdetectTikaTextDetector", service.langdetectTikaTextLangDetector(sentence));
				
			    flowFile = session.putAttribute(flowFile, "langdetectTika", service.langdetectTikaOptimaize(sentence));
//				flowFile = session.putAttribute(flowFile, "langdetectOpenNLP",
//						service.langdetectOpenNLP(
//								context.getProperty(EXTRA_RESOURCE).evaluateAttributeExpressions(flowFile).getValue(),
//								service.langdetectTika(sentence)));

				//
			} catch (Exception e) {
				throw new ProcessException(e);
			}

			session.transfer(flowFile, REL_SUCCESS);
			session.commit();
		} catch (final Throwable t) {
			getLogger().error("Unable to process NLP Processor file " + t.getLocalizedMessage());
			getLogger().error("{} failed to process due to {}; rolling back session", new Object[] { this, t });
			throw t;
		}
	}
}
