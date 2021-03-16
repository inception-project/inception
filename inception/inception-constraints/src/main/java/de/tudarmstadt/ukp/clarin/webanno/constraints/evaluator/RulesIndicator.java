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
package de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator;

import java.io.Serializable;

/**
 * Class for indicating whether Constraints affected a feature or not.
 * https://github.com/webanno/webanno/issues/46
 */
public class RulesIndicator
    implements Serializable
{
    private static final long serialVersionUID = -5606299056181945134L;

    private static final int STATUS_UNKNOWN = 0;
    private static final int STATUS_NO_TAGSET = 1;
    private static final int STATUS_NO_RULE_MATCH = 2;
    private static final int STATUS_RULE_MATCH = 3;

    private int status = STATUS_UNKNOWN;
    private boolean affected;

    public String getStatusColor()
    {
        switch (status) {
        case STATUS_NO_TAGSET:
            return "red";
        case STATUS_NO_RULE_MATCH:
            return "orange";
        case STATUS_RULE_MATCH:
            return "green";
        default:
            return "";
        }
    }

    public boolean isAffected()
    {
        return affected;
    }

    public void reset()
    {
        status = STATUS_UNKNOWN;
        affected = false;
    }

    /**
     * Sets if rules can affect or not.
     */
    public void setAffected(boolean existence)
    {
        affected = existence;
    }

    /**
     * If a feature is affected by a constraint but there is no tagset defined on the feature. In
     * such a case the constraints cannot reorder tags and have no effect.
     */
    public void didntMatchAnyTag()
    {
        if (affected && status != STATUS_NO_RULE_MATCH && status != STATUS_RULE_MATCH) {
            status = STATUS_NO_TAGSET;
        }
    }

    /**
     * if a feature is affected by a constraint but no rule covers the feature value, e.g.
     * <code>@Lemma.value = "go" -&gt; aFrame = "going"</code>. Here aFrame is affected by a
     * constraint. However, if the actual lemma annotated in the document is walk and there is no
     * rule that covers walk, then we should also indicate that.
     */
    public void didntMatchAnyRule()
    {
        if (affected && status != STATUS_RULE_MATCH && status != STATUS_NO_TAGSET) {
            status = STATUS_NO_RULE_MATCH;
        }
    }

    /**
     * For case that a constrained actually applied ok there should be a marker.
     */
    public void rulesApplied()
    {
        status = STATUS_RULE_MATCH;
    }

    public String getStatusSymbol()
    {
        switch (status) {
        case STATUS_NO_TAGSET:
            return "fa fa-exclamation-circle";
        case STATUS_NO_RULE_MATCH:
            return "fa fa-info-circle";
        case STATUS_RULE_MATCH:
            return "fa fa-check-circle";
        default:
            return "";
        }
    }

    public String getStatusDescription()
    {
        switch (status) {
        case STATUS_NO_TAGSET:
            return "Feature must be configured to use a tagset for constraint rules to work!";
        case STATUS_NO_RULE_MATCH:
            return "None of the constraint rules affecting this feature match.";
        case STATUS_RULE_MATCH:
            return "At least one constraint rule affecting this feature matches";
        default:
            return "";
        }
    }
}
