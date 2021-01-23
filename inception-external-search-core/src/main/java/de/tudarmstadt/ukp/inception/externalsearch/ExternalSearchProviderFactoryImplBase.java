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
package de.tudarmstadt.ukp.inception.externalsearch;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Ordered;

import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public abstract class ExternalSearchProviderFactoryImplBase
    implements BeanNameAware, Ordered, ExternalSearchProviderFactory<Object>
{
    private String beanName;

    @Override
    public void setBeanName(String aName)
    {
        beanName = aName;
    }

    @Override
    public String getBeanName()
    {
        return beanName;
    }

    @Override
    public int getOrder()
    {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * Returns a Wicket component to configure the specific traits of this provider. Every
     * {@link ExternalSearchProviderFactory} has to return a <b>different class</b> here. It is not
     * possible to simple return a Wicket {@link Panel} here, but it must be a subclass If this is
     * not done, then the properties editor in the UI will not be correctly updated.
     * 
     * @param aId
     *            a markup ID.
     * @param aDocumentRepository
     *            a model holding the document repository for which the properties editor should be
     *            created.
     * @return the properties editor component .
     */
    @Override
    public Panel createTraitsEditor(String aId, IModel<DocumentRepository> aDocumentRepository)
    {
        return new EmptyPanel(aId);
    }

    /**
     * Read the properties for the given {@link DocumentRepository}. If properties are supported,
     * then this method must be overwritten. A typical implementation would read the traits from a
     * JSON string stored {@link DocumentRepository#getProperties}, but it would also be possible to
     * load the traits from a database table.
     * 
     * @param aDocumentRepository
     *            the repository whose properties should be obtained.
     * @return the properties.
     */
    public Object readTraits(DocumentRepository aDocumentRepository)
    {
        return null;
    }

    /**
     * Write the properties for the given {@link DocumentRepository}. If properties are supported,
     * then this method must be overwritten. A typical implementation would write the properties to
     * the JSON string stored in {@link DocumentRepository#setProperties}, but it would also be
     * possible to store the traits into a database table.
     * 
     * @param aDocumentRepository
     *            the repository whose properties should be written.
     * @param aProperties
     *            the properties.
     */
    public void writeTraits(DocumentRepository aDocumentRepository, Object aProperties)
    {
        aDocumentRepository.setProperties(null);
    }

}
