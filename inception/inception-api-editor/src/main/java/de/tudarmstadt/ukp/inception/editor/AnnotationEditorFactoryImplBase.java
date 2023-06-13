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
package de.tudarmstadt.ukp.inception.editor;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Ordered;

import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;

public abstract class AnnotationEditorFactoryImplBase
    implements BeanNameAware, Ordered, AnnotationEditorFactory
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

    @Override
    public Optional<PolicyCollection> getPolicy() throws IOException
    {
        return Optional.empty();
    }
}
