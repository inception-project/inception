/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.curation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * Provides {@link CurationProperties} unconditionally. This deliberately does not live in the
 * curation implementation module: the curation entry rules also govern whether an annotator may
 * still edit a document (see {@code DocumentAccessImpl}), so core modules need to be able to read
 * these properties without depending on the curation implementation or on its autoconfiguration
 * having been activated.
 * </p>
 */
@Configuration
@EnableConfigurationProperties({ CurationPropertiesImpl.class })
public class CurationPropertiesAutoConfiguration
{
    // No Beans
}
