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
package de.tudarmstadt.ukp.inception.curation.merge;

/**
 * Indicates that a merge operation was skipped because the target position is already occupied by
 * an existing annotation (e.g. a decision made by a curator) which is preserved instead of being
 * overwritten. This differs from {@link AlreadyMergedException}, which indicates that the
 * annotation to be merged was already present in the target (i.e. the merge would have been a
 * no-op) - here the merged value genuinely differs but is deliberately not applied.
 *
 * @see CasMergeContext#isPreserveExisting()
 */
public class AnnotationPreservedException
    extends AlreadyMergedException
{
    private static final long serialVersionUID = -2571338462024862789L;

    public AnnotationPreservedException()
    {
        super();
    }

    public AnnotationPreservedException(String message)
    {
        super(message);
    }

    public AnnotationPreservedException(String aMessage, Throwable aCause)
    {
        super(aMessage, aCause);
    }

    public AnnotationPreservedException(Throwable aCause)
    {
        super(aCause);
    }
}
