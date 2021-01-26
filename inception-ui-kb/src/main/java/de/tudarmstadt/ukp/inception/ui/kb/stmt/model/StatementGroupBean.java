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
package de.tudarmstadt.ukp.inception.ui.kb.stmt.model;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.StatementDetailPreference;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.StatementsPanel;

public class StatementGroupBean
    implements Serializable
{
    private static final long serialVersionUID = -2187168839723008724L;

    /**
     * Results in every {@link StatementGroupBean} being a unique object. Reason: It should be
     * possible to add multiple new statement groups in the UI at the same time. However, when
     * removing such new statement groups without having chosen a KBProperty beforehand, every one
     * of these new statement groups is equal to each other, because all attributes in their bean
     * model object are equal. This makes it difficult (if not impossible) to identify the right
     * statement group to remove from the list of statement groups (kept in the
     * {@link StatementsPanel}). To resolve this ambiguity, this UUID attribute is added, which
     * results in every {@link StatementGroupBean} being a unique object.
     */
    private final UUID id;

    private KnowledgeBase kb;
    private KBHandle instance;
    private KBProperty property;
    private List<KBStatement> statements;
    private StatementDetailPreference detailPreference;

    public StatementGroupBean()
    {
        id = UUID.randomUUID();
    }

    public KnowledgeBase getKb()
    {
        return kb;
    }

    public void setKb(KnowledgeBase aKB)
    {
        kb = aKB;
    }

    public KBHandle getInstance()
    {
        return instance;
    }

    public void setInstance(KBHandle aInstance)
    {
        instance = aInstance;
    }

    public KBProperty getProperty()
    {
        return property;
    }

    public void setProperty(KBProperty aProperty)
    {
        property = aProperty;
    }

    public List<KBStatement> getStatements()
    {
        return statements;
    }

    public void setStatements(List<KBStatement> aStatements)
    {
        statements = aStatements;
    }

    public StatementDetailPreference getDetailPreference()
    {
        return detailPreference;
    }

    public void setDetailPreference(StatementDetailPreference aDetailPreference)
    {
        detailPreference = aDetailPreference;
    }

    public boolean isNew()
    {
        return isEmpty(property.getIdentifier());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        StatementGroupBean other = (StatementGroupBean) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        }
        else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
