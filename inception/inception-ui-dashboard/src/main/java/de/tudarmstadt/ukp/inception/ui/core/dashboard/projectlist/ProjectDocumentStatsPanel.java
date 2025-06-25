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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

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

        var stats = aModel.getObject();
        var total = stats.getTotal();
        var newPerc = total > 0 ? (double) stats.getNewAnnotations() / total * 100 : 0;
        var annotationsInProgressPerc = total > 0
                ? (double) stats.getAnnotationsInProgress() / total * 100
                : 0;
        var finishedAnnotationsPerc = total > 0
                ? (double) stats.getFinishedAnnotations() / total * 100
                : 0;
        var curationsInProgressPerc = total > 0
                ? (double) stats.getCurationsInProgress() / total * 100
                : 0;
        var curationsFinishedPerc = total > 0 ? (double) stats.getCurationsFinished() / total * 100
                : 0;

        queue(new WebMarkupContainer("badgeNew") //
                .add(new LambdaClassAttributeModifier(classes -> {
                    if (stats.getNewAnnotations() == 0) {
                        classes.add("text-opacity-25");
                    }
                    return classes;
                })));
        queue(new SymbolLabel("iconNew", NEW));
        queue(new Label("docsNew", stats.getNewAnnotations()));
        queue(new Label("barNew") //
                .add(new LambdaStyleAttributeModifier(styles -> {
                    styles.put("width", newPerc + "%");
                    // styles.put("background", NEW.getColor());
                    return styles;
                })) //
                .add(visibleWhen(() -> newPerc > 0)));

        queue(new WebMarkupContainer("badgeAnnotationInProgress") //
                .add(new LambdaClassAttributeModifier(classes -> {
                    if (stats.getAnnotationsInProgress() == 0) {
                        classes.add("text-opacity-25");
                    }
                    return classes;
                })));
        queue(new SymbolLabel("iconAnnotationInProgress", ANNOTATION_IN_PROGRESS));
        queue(new Label("docsAnnotationInProgress", stats.getAnnotationsInProgress()));
        queue(new Label("barAnnotationInProgress") //
                .add(new LambdaStyleAttributeModifier(styles -> {
                    styles.put("width", annotationsInProgressPerc + "%");
                    styles.put("background", ANNOTATION_IN_PROGRESS.getColor());
                    return styles;
                })) //
                .add(visibleWhen(() -> annotationsInProgressPerc > 0)));

        queue(new WebMarkupContainer("badgeAnnotationFinished") //
                .add(new LambdaClassAttributeModifier(classes -> {
                    if (stats.getFinishedAnnotations() == 0) {
                        classes.add("text-opacity-25");
                    }
                    return classes;
                })));
        queue(new SymbolLabel("iconAnnotationFinished", ANNOTATION_FINISHED));
        queue(new Label("docsAnnotationFinished", stats.getFinishedAnnotations()));
        queue(new Label("barAnnotationFinished") //
                .add(new LambdaStyleAttributeModifier(styles -> {
                    styles.put("width", finishedAnnotationsPerc + "%");
                    styles.put("background", ANNOTATION_FINISHED.getColor());
                    return styles;
                })) //
                .add(visibleWhen(() -> finishedAnnotationsPerc > 0)));

        queue(new WebMarkupContainer("badgeCurationInProgress") //
                .add(new LambdaClassAttributeModifier(classes -> {
                    if (stats.getCurationsInProgress() == 0) {
                        classes.add("text-opacity-25");
                    }
                    return classes;
                })));
        queue(new SymbolLabel("iconCurationInProgress", CURATION_IN_PROGRESS));
        queue(new Label("docsCurationInProgress", stats.getCurationsInProgress()));
        queue(new Label("barCurationInProgress") //
                .add(new LambdaStyleAttributeModifier(styles -> {
                    styles.put("width", curationsInProgressPerc + "%");
                    styles.put("background", CURATION_IN_PROGRESS.getColor());
                    return styles;
                })) //
                .add(visibleWhen(() -> curationsInProgressPerc > 0)));

        queue(new WebMarkupContainer("badgeCurationFinished") //
                .add(new LambdaClassAttributeModifier(classes -> {
                    if (stats.getCurationsFinished() == 0) {
                        classes.add("text-opacity-25");
                    }
                    return classes;
                })));
        queue(new SymbolLabel("iconCurationFinished", CURATION_FINISHED));
        queue(new Label("docsCurationFinished", stats.getCurationsFinished()));
        queue(new Label("barCurationFinished") //
                .add(new LambdaStyleAttributeModifier(styles -> {
                    styles.put("width", curationsFinishedPerc + "%");
                    styles.put("background", CURATION_FINISHED.getColor());
                    return styles;
                })) //
                .add(visibleWhen(() -> curationsFinishedPerc > 0)));
    }
}
