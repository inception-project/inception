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
package de.tudarmstadt.ukp.inception.support.xml.sanitizer;

import java.util.Map;
import java.util.function.Supplier;

import javax.xml.namespace.QName;

public class ElementPolicyBuilder
{
    private final QName qName;
    private final ElementAction action;

    private final Map<QName, QNameAttributePolicy> attributePolicies;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ElementPolicyBuilder(QName aQname, ElementAction aAction,
            Supplier<? extends Map> aMapSupplier)
    {
        qName = aQname;
        action = aAction;
        attributePolicies = aMapSupplier.get();
    }

    public QNameElementPolicy build()
    {
        return new QNameElementPolicy(qName, action, attributePolicies);
    }

    public void attributePolicies(Map<QName, QNameAttributePolicy> aMap)
    {
        if (aMap != null) {
            attributePolicies.putAll(aMap);
        }
    }
}
