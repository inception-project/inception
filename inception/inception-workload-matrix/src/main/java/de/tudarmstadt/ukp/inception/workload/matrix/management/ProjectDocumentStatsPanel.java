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
package de.tudarmstadt.ukp.inception.workload.matrix.management;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.NEW;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.documents.api.SourceDocumentStateStats;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaClassAttributeModifier;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaStyleAttributeModifier;
import de.tudarmstadt.ukp.inception.support.wicket.SymbolLabel;

public class ProjectDocumentStatsPanel
    extends GenericPanel<SourceDocumentStateStats>
{
    private static final long serialVersionUID = 180736488675590080L;

    public ProjectDocumentStatsPanel(String aId, IModel<SourceDocumentStateStats> aModel)
    {
        super(aId, aModel);

        queue(new WebMarkupContainer("badgeNew") //
                .add(new LambdaClassAttributeModifier(classes -> {
                    if (getModelObject().getNewAnnotations() == 0) {
                        classes.add("text-opacity-25");
                    }
                    return classes;
                })));
        queue(new SymbolLabel("iconNew", NEW));
        queue(new Label("docsNew", getModel().map(SourceDocumentStateStats::getNewAnnotations)));
        queue(new Label("barNew") //
                .add(new LambdaStyleAttributeModifier(styles -> {
                    styles.put("width", getModelObject().getNewAnnotationsPerc() + "%");
                    return styles;
                })) //
                .add(visibleWhen(() -> getModelObject().getNewAnnotationsPerc() > 0)));

        queue(new WebMarkupContainer("badgeAnnotationInProgress") //
                .add(new LambdaClassAttributeModifier(classes -> {
                    if (getModelObject().getAnnotationsInProgress() == 0) {
                        classes.add("text-opacity-25");
                    }
                    return classes;
                })));
        queue(new SymbolLabel("iconAnnotationInProgress", ANNOTATION_IN_PROGRESS));
        queue(new Label("docsAnnotationInProgress",
                getModel().map(SourceDocumentStateStats::getAnnotationsInProgress)));
        queue(new Label("barAnnotationInProgress") //
                .add(new LambdaStyleAttributeModifier(styles -> {
                    styles.put("width", getModelObject().getAnnotationsInProgressPerc() + "%");
                    styles.put("background", ANNOTATION_IN_PROGRESS.getColor());
                    return styles;
                })) //
                .add(visibleWhen(() -> getModelObject().getAnnotationsInProgressPerc() > 0)));

        queue(new WebMarkupContainer("badgeAnnotationFinished") //
                .add(new LambdaClassAttributeModifier(classes -> {
                    if (getModelObject().getFinishedAnnotations() == 0) {
                        classes.add("text-opacity-25");
                    }
                    return classes;
                })));
        queue(new SymbolLabel("iconAnnotationFinished", ANNOTATION_FINISHED));
        queue(new Label("docsAnnotationFinished",
                getModel().map(SourceDocumentStateStats::getFinishedAnnotations)));
        queue(new Label("barAnnotationFinished") //
                .add(new LambdaStyleAttributeModifier(styles -> {
                    styles.put("width", getModelObject().getFinishedAnnotationsPerc() + "%");
                    styles.put("background", ANNOTATION_FINISHED.getColor());
                    return styles;
                })) //
                .add(visibleWhen(() -> getModelObject().getFinishedAnnotationsPerc() > 0)));

        queue(new WebMarkupContainer("badgeCurationInProgress") //
                .add(new LambdaClassAttributeModifier(classes -> {
                    if (getModelObject().getCurationsInProgress() == 0) {
                        classes.add("text-opacity-25");
                    }
                    return classes;
                })));
        queue(new SymbolLabel("iconCurationInProgress", CURATION_IN_PROGRESS));
        queue(new Label("docsCurationInProgress",
                getModel().map(SourceDocumentStateStats::getCurationsInProgress)));
        queue(new Label("barCurationInProgress") //
                .add(new LambdaStyleAttributeModifier(styles -> {
                    styles.put("width", getModelObject().getCurationsInProgressPerc() + "%");
                    styles.put("background", CURATION_IN_PROGRESS.getColor());
                    return styles;
                })) //
                .add(visibleWhen(() -> getModelObject().getCurationsInProgressPerc() > 0)));

        queue(new WebMarkupContainer("badgeCurationFinished") //
                .add(new LambdaClassAttributeModifier(classes -> {
                    if (getModelObject().getCurationsFinished() == 0) {
                        classes.add("text-opacity-25");
                    }
                    return classes;
                })));
        queue(new SymbolLabel("iconCurationFinished", CURATION_FINISHED));
        queue(new Label("docsCurationFinished",
                getModel().map(SourceDocumentStateStats::getCurationsFinished)));
        queue(new Label("barCurationFinished") //
                .add(new LambdaStyleAttributeModifier(styles -> {
                    styles.put("width", getModelObject().getCurationsFinishedPerc() + "%");
                    styles.put("background", CURATION_FINISHED.getColor());
                    return styles;
                })) //
                .add(visibleWhen(() -> getModelObject().getCurationsFinishedPerc() > 0)));
    }
}
