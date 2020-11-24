/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures;

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL;

import java.io.Serializable;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior;

public class DefaultAgreementTraits
    implements Serializable
{
    private static final long serialVersionUID = -2554578512915184789L;

    private boolean limitToFinishedDocuments = true;

    private LinkCompareBehavior linkCompareBehavior = LINK_TARGET_AS_LABEL;

    public LinkCompareBehavior getLinkCompareBehavior()
    {
        return linkCompareBehavior;
    }

    public void setLinkCompareBehavior(LinkCompareBehavior aLinkCompareBehavior)
    {
        linkCompareBehavior = aLinkCompareBehavior;
    }

    public boolean isLimitToFinishedDocuments()
    {
        return limitToFinishedDocuments;
    }

    public void setLimitToFinishedDocuments(boolean aLimitToFinishedDocuments)
    {
        limitToFinishedDocuments = aLimitToFinishedDocuments;
    }
}
