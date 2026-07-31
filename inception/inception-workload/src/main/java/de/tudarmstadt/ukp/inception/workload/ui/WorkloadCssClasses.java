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
 * CSS class names shared by the workload management pages. Each page styles these in its own
 * stylesheet, scoped to its table, so the rules differ but the names and their meaning must not.
 */
public final class WorkloadCssClasses
{
    /**
     * Marks a cell whose document is in curation although annotation on it is not (or no longer)
     * complete.
     */
    public static final String CSS_CLASS_CURATION_NOT_READY = "curation-not-ready";

    private WorkloadCssClasses()
    {
        // No instances
    }
}
