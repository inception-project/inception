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
package de.tudarmstadt.ukp.clarin.webanno.api.casstorage;

import java.time.Instant;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.DocumentRef;

/**
 * A copy of a CAS that the CAS storage keeps besides the current version - a backup from the
 * history, or the previous version left behind by an interrupted write.
 *
 * @param document
 *            the source document the CAS belongs to.
 * @param set
 *            the annotation set the CAS belongs to.
 * @param name
 *            the name of the copy in the storage, e.g. its file name.
 * @param timestamp
 *            when the copy was made.
 */
public record CasBackup(DocumentRef document, AnnotationSet set, String name, Instant timestamp) {}
