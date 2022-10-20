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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;

import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseInfo;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownUtil;

public class KnowledgeBaseInfoPanel
    extends Panel
{
    private static final long serialVersionUID = 7448919085704708171L;

    public KnowledgeBaseInfoPanel(String aId, CompoundPropertyModel<KnowledgeBaseInfo> aModel)
    {
        super(aId, aModel);

        queue(new Label("description",
                aModel.bind("description")
                        .map(description -> MarkdownUtil.markdownToHtml((String) description)))
                                .setEscapeModelStrings(false)
                                .add(visibleWhen(() -> aModel.getObject() != null)));
        queue(new Label("hostInstitutionName", aModel.bind("hostInstitutionName"))
                .add(visibleWhen(() -> aModel.getObject() != null)));
        queue(new Label("authorName", aModel.bind("authorName"))
                .add(visibleWhen(() -> aModel.getObject() != null)));
        queue(new ExternalLink("websiteURL", aModel.bind("websiteURL"), aModel.bind("websiteURL"))
                .add(visibleWhen(() -> aModel.getObject() != null)));
    }
}
