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
package de.tudarmstadt.ukp.inception.workload.extension;

import java.io.Serializable;

/**
 * Describes why a document is not ready for curation. Carries the shortfall as numbers in addition
 * to the prose so that a compact indicator (e.g. a badge reading <code>1/3 incomplete</code>) and a
 * full explanation can be rendered from the same source without either side re-deriving the rule.
 *
 * @param missingAnnotatorCount
 *            how many annotators still have to finish the document for it to become ready.
 * @param requiredAnnotatorCount
 *            how many annotators are required in total.
 * @param message
 *            the full human-readable explanation.
 */
public record CurationReadinessWarning(long missingAnnotatorCount, long requiredAnnotatorCount,
        String message)
    implements Serializable
{
    /**
     * @return a compact label for a badge, e.g. <code>1/3</code>.
     */
    public String shortLabel()
    {
        return missingAnnotatorCount + "/" + requiredAnnotatorCount;
    }
}
