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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.page;

/**
 * Controls how the curation CAS is (re-)created when opening or (re-)merging a document.
 */
public enum CurationMergeMode
{
    /**
     * Do not merge into an existing curation CAS. If a curation CAS already exists, it is loaded
     * as-is; only if none exists yet is it created from the annotators and a full merge performed.
     * This is the normal document-loading behavior.
     */
    LOAD_ONLY,

    /**
     * Recreate the curation CAS from scratch and perform a full merge, discarding any annotations
     * already present in the curation document.
     */
    RECREATE,

    /**
     * Merge into the existing curation CAS, only filling positions which are not yet occupied so
     * that annotations already present in the curation document (e.g. curator decisions) are
     * preserved.
     */
    FILL_ONLY;
}
