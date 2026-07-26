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
package de.tudarmstadt.ukp.inception.workload.ui;

/**
 * CSS class names shared by the workload management pages. The pages style these classes in their
 * own stylesheets - scoped to their respective table - so the rules differ, but the class names and
 * the meaning attached to them must not.
 */
public final class WorkloadCssClasses
{
    /**
     * Marks a cell whose document is in curation although annotation on it is not (or no longer)
     * complete. Used on the state column of the dynamic workload management page and on the
     * curation column of the matrix workload management page.
     */
    public static final String CSS_CLASS_CURATION_NOT_READY = "curation-not-ready";

    private WorkloadCssClasses()
    {
        // No instances
    }
}
