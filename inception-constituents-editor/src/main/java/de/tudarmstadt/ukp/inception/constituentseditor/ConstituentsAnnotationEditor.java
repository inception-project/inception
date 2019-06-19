/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.constituentseditor;

import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

public class ConstituentsAnnotationEditor
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -3358207848681467993L;
    private static final Logger LOG = LoggerFactory.getLogger(ConstituentsAnnotationEditor.class);

    private Label vis;

    private @SpringBean PreRenderer preRenderer;
    private @SpringBean AnnotationSchemaService annotationService;

    public ConstituentsAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, JCasProvider aJCasProvider)
    {
        super(aId, aModel, aActionHandler, aJCasProvider);

        vis = new Label("vis");
        vis.setOutputMarkupId(true);
        vis.setEscapeModelStrings(false);
        add(vis);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
    }

    private void handleError(String aMessage, Throwable aCause)
    {
        LOG.error(aMessage, aCause);
        error(aMessage + ExceptionUtils.getRootCauseMessage(aCause));
        return;
    }

    private String toJson(Object result)
    {
        String json = "[]";
        try {
            json = JSONUtil.toInterpretableJsonString(result);
        }
        catch (IOException e) {
            error("Unable to produce JSON response " + ":" + ExceptionUtils.getRootCauseMessage(e));
        }
        return json;
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        aTarget.add(vis);
    }
}
