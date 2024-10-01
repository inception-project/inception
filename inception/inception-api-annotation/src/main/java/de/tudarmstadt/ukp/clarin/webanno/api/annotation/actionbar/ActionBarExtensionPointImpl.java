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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.support.extensionpoint.ExtensionPoint_ImplBase;

@Component
public class ActionBarExtensionPointImpl
    extends ExtensionPoint_ImplBase<AnnotationPageBase, ActionBarExtension>
    implements ActionBarExtensionPoint
{
    public ActionBarExtensionPointImpl(
            @Lazy @Autowired(required = false) List<ActionBarExtension> aExtensions)
    {
        super(aExtensions);
    }

    /**
     * Returns all extensions matching in the given context. Ensures that for a given
     * {@link ActionBarExtension#getRole() role}, only the extension with the highest
     * {@link ActionBarExtension#getPriority() priority} is returned.
     */
    @Override
    public List<ActionBarExtension> getExtensions(AnnotationPageBase aContext)
    {
        // Using a LinkedHashMap here because we want to preserve the order in which the extensions
        // are displayed in the action bar.
        var byRole = new LinkedHashMap<String, ActionBarExtension>();
        for (var extension : super.getExtensions(aContext)) {
            var existingExtension = byRole.computeIfAbsent(extension.getRole(), key -> extension);

            // If the previously found extension has a lower priority, then we replace it with the
            // current one
            if (existingExtension.getPriority() < extension.getPriority()) {
                byRole.put(extension.getRole(), extension);
            }
        }

        return new ArrayList<>(byRole.values());
    }
}
