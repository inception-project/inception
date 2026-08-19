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
package de.tudarmstadt.ukp.inception.diam.editing;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * Thrown when a delete failed <b>after</b> the CAS had already been modified - e.g. when attached
 * relations or slots pointing at the annotation were cleaned up, but deleting the annotation itself
 * was then rejected.
 * <p>
 * The CAS is left in an inconsistent state when this is thrown. Callers must not persist it and
 * should present {@link #getMessages()} alongside the failure.
 * <p>
 * FIXME: Callers currently cannot properly recover. The half-modified CAS remains in the CAS
 * storage session cache, so simply not writing it is not enough - later actions in the same session
 * still see it and may persist it. Recovery requires invalidating the cache entry so the CAS is
 * re-read from storage.
 * <p>
 * A plain {@link AnnotationException} from {@link AnnotationEditingService#deleteAnnotation} means
 * nothing was modified and the CAS is still usable.
 */
public class PartialDeleteException
    extends AnnotationException
{
    private static final long serialVersionUID = 4098765132894572610L;

    private final List<LogMessage> messages;

    public PartialDeleteException(Throwable aCause, List<LogMessage> aMessages)
    {
        super(aCause != null ? aCause.getMessage() : null, aCause);
        messages = aMessages != null ? new ArrayList<>(aMessages) : emptyList();
    }

    /**
     * @return messages collected before the failure, e.g. about slots that were cleared. These
     *         describe modifications that were already applied to the CAS.
     */
    public List<LogMessage> getMessages()
    {
        return unmodifiableList(messages);
    }
}
