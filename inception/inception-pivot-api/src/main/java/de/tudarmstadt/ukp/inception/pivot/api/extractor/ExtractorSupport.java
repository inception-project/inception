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

import de.tudarmstadt.ukp.inception.pivot.api.report.ExtractorDef;
import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;

/**
 * Builds an extractor for a given {@link ExtractorBinding}. A support handles exactly the binding
 * kinds for which {@link #accepts} returns {@code true}; both {@link #renderLabel} and
 * {@link #createExtractor} are only ever called with such a binding.
 * <p>
 * Implementations must return a <em>stable</em> {@link #getId() id}: it is persisted in saved
 * reports and used to look the support up again on load, so it must not change when the class is
 * renamed or moved (hence it is not derived from the class name).
 */
public interface ExtractorSupport
    extends Extension<ExtractorBinding>
{
    /**
     * The display label for this extractor when bound as given (e.g. {@code covered text} or the
     * feature name). Must not include the layer name - the layer is contributed by the binding's
     * {@link ExtractorBinding#groupLabel()}.
     */
    String renderLabel(ExtractorBinding aBinding);

    Extractor<?, ?> createExtractor(ExtractorBinding aBinding);

    /**
     * Reconstructs this extractor's binding from its persisted definition, resolving any referenced
     * layer/feature via the given context (which records a problem and yields {@code null} if a
     * reference is gone). The default handles layer-bound extractors; feature-bound or
     * layer-independent extractors override this.
     *
     * @return the resolved binding, or {@code null} if a referenced element no longer exists.
     */
    default ExtractorBinding bindingFromDef(ExtractorDef aDef,
            ExtractorBindingResolutionContext aContext)
    {
        var layer = aContext.resolveLayer(aDef.getLayer());
        return layer != null ? new LayerBinding(layer) : null;
    }
}
