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

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;

import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseInfo;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;

public class KnowledgeBaseInfoPanel
    extends Panel
{
    private static final long serialVersionUID = 7448919085704708171L;

    public KnowledgeBaseInfoPanel(String aId, CompoundPropertyModel<KnowledgeBaseInfo> aModel)
    {
        super(aId, aModel);

        var description = aModel.map(KnowledgeBaseInfo::getDescription);
        queue(new MarkdownLabel("description", description)
                .add(visibleWhen(description.map(StringUtils::isNotBlank))));

        var hostInstitutionName = aModel.map(KnowledgeBaseInfo::getHostInstitutionName);
        queue(new Label("hostInstitutionName", hostInstitutionName)
                .add(visibleWhen(hostInstitutionName.map(StringUtils::isNotBlank))));

        var authorName = aModel.map(KnowledgeBaseInfo::getAuthorName);
        queue(new Label("authorName", authorName)
                .add(visibleWhen(authorName.map(StringUtils::isNotBlank))));

        var websiteURL = aModel.map(KnowledgeBaseInfo::getWebsiteUrl);
        queue(new ExternalLink("websiteUrl", websiteURL, websiteURL)
                .add(visibleWhen(websiteURL.map(StringUtils::isNotBlank))));

        var licenseUrl = aModel.map(KnowledgeBaseInfo::getLicenseUrl);
        var licenseName = aModel.map(KnowledgeBaseInfo::getLicenseName)
                .orElseGet(licenseUrl::getObject);
        queue(new ExternalLink("licenseUrl", licenseUrl, licenseName)
                .add(visibleWhen(licenseUrl.map(StringUtils::isNotBlank))));
    }
}
