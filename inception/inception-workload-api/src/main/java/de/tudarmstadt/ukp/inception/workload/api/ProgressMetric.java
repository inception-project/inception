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
package de.tudarmstadt.ukp.inception.workload.api;

/**
 * Unit of the progress chart's Y axis. {@link #DOCUMENTS} counts one per source document;
 * everything else weights each document by some per-document quantity sourced from the search index
 * (see {@link ProgressWeighter}). When a weight source does not support a metric the caller falls
 * back to {@code DOCUMENTS}.
 */
public enum ProgressMetric
{
    DOCUMENTS, TOKENS, SENTENCES
}
