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

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExternalPolicyCollection
{
    private String name;
    private String version;
    private boolean caseSensitive;
    private List<ExternalPolicy> policies;
    private boolean debug;
    private ElementAction defaultElementAction;
    private AttributeAction defaultAttributeAction;
    private String defaultNamespace;
    private boolean matchWithoutNamespace = false;
    private boolean useDefaultNamespaceForAttributes = true;

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String aVersion)
    {
        version = aVersion;
    }

    public boolean isCaseSensitive()
    {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean aCaseSensitive)
    {
        caseSensitive = aCaseSensitive;
    }

    public List<ExternalPolicy> getPolicies()
    {
        return policies;
    }

    public void setPolicies(List<ExternalPolicy> aPolicies)
    {
        policies = aPolicies;
    }

    public void setDebug(boolean aDebug)
    {
        debug = aDebug;
    }

    public boolean isDebug()
    {
        return debug;
    }

    public void setDefaultElementAction(ElementAction aDefaultElementAction)
    {
        defaultElementAction = aDefaultElementAction;
    }

    public ElementAction getDefaultElementAction()
    {
        return defaultElementAction;
    }

    public void setDefaultAttributeAction(AttributeAction aDefaultAttributeAction)
    {
        defaultAttributeAction = aDefaultAttributeAction;
    }

    public AttributeAction getDefaultAttributeAction()
    {
        return defaultAttributeAction;
    }

    public String getDefaultNamespace()
    {
        return defaultNamespace;
    }

    public void setDefaultNamespace(String aDefaultNamespace)
    {
        defaultNamespace = aDefaultNamespace;
    }

    public boolean isMatchWithoutNamespace()
    {
        return matchWithoutNamespace;
    }

    public void setMatchWithoutNamespace(boolean aMatchWithoutNamespace)
    {
        matchWithoutNamespace = aMatchWithoutNamespace;
    }

    public boolean isUseDefaultNamespaceForAttributes()
    {
        return useDefaultNamespaceForAttributes;
    }

    public void setUseDefaultNamespaceForAttributes(boolean aUseDefaultNamespaceForAttribues)
    {
        useDefaultNamespaceForAttributes = aUseDefaultNamespaceForAttribues;
    }
}
