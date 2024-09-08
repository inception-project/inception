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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.support.extensionpoint.ExtensionPoint_ImplBase;

@Component
public class AnnotationSidebarRegistryImpl
    extends ExtensionPoint_ImplBase<AnnotationPageBase, AnnotationSidebarFactory>
    implements AnnotationSidebarRegistry
{
    private static final Logger LOG = getLogger(lookup().lookupClass());

    public AnnotationSidebarRegistryImpl(
            @Lazy @Autowired(required = false) List<AnnotationSidebarFactory> aExtensions)
    {
        super(aExtensions);
    }

    @Deprecated
    @Override
    public List<AnnotationSidebarFactory> getSidebarFactories()
    {
        return getExtensions();
    }

    @Deprecated
    @Override
    public AnnotationSidebarFactory getSidebarFactory(String aId)
    {
        return getExtension(aId).orElseThrow(
                () -> new NoSuchElementException("No extension with id [" + aId + "] found"));
    }

    @Override
    public AnnotationSidebarFactory getDefaultSidebarFactory()
    {
        return getSidebarFactories().get(0);
    }

    /**
     * Builds a comparator that sorts first by the order, if specified, then by the display name to
     * break ties. It is assumed that the compared elements are all non-null
     * 
     * @return The comparator
     */
    @Override
    protected Comparator<AnnotationSidebarFactory> makeComparator()
    {
        return (asf1, asf2) -> new CompareToBuilder() //
                .appendSuper(AnnotationAwareOrderComparator.INSTANCE.compare(asf1, asf2)) //
                .append(asf1.getDisplayName(), asf2.getDisplayName()) //
                .toComparison();
    }
}
