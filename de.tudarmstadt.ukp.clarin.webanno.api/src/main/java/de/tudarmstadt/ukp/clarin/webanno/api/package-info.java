/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/**
 * Provides Interfaces for different dao method implementations. Currently there are two Interfaces,
 * {@link de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService}  and {@link de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService} .
 * {@link de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService} conatins mehtods related to {@link de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType}
 * ,{@link de.tudarmstadt.ukp.clarin.webanno.model.TagSet}, and {@link de.tudarmstadt.ukp.clarin.webanno.model.Tag}
 * . The {@link de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService} contains methods related to persistent Objects
 * {@link de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument}, {@link de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument},
 * {@link de.tudarmstadt.ukp.clarin.webanno.model.User}, {@link de.tudarmstadt.ukp.clarin.webanno.model.Project} and so on
 */
package de.tudarmstadt.ukp.clarin.webanno.api;
