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
package de.tudarmstadt.ukp.inception.pivot.api.extractor;

import java.io.Serializable;

import de.tudarmstadt.ukp.inception.pivot.api.report.ExtractorDef;

/**
 * What an extractor is bound to (a layer, a feature, or nothing for layer-independent extractors).
 * <p>
 * The binding owns the concerns that are derived purely from the binding target - the display group
 * it belongs to and its serialised form - so that callers do not have to switch on the kind of
 * target. This is intentionally an open interface rather than a sealed one: new binding kinds may
 * be introduced by other modules, just like extractor supports.
 */
public interface ExtractorBinding
    extends Serializable
{
    /**
     * The label of the group this binding belongs to in the extractor picker (typically the layer
     * name).
     */
    String groupLabel();

    /**
     * The serialisable form of this binding for the given extractor.
     */
    ExtractorDef toDef(String aExtractorId);
}
