/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.recommendation.api.recommender;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.markup.html.form.AbstractChoice;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public class TrainingStatesChoice
    extends CheckBoxMultipleChoice<AnnotationDocumentState>
{
    private static final long serialVersionUID = 7627977162174971025L;

    public TrainingStatesChoice(String aId,
            IModel<Recommender> recommenderModel)
    {
        super(aId);
        
        // We need to invert the states in documentStates, as the recommender stores the
        // ones to ignore, not the ones to consider
        IModel<Set<AnnotationDocumentState>> model = new IModel<Set<AnnotationDocumentState>>() {
            private static final long serialVersionUID = 2894838629097952859L;

            @Override
            public void setObject(Set<AnnotationDocumentState> states) {
                // The model can be null after save and delete
                if (recommenderModel.getObject() != null) {
                    recommenderModel.getObject().setStatesIgnoredForTraining(invert(states));
                }
            }

            @Override
            public Set<AnnotationDocumentState> getObject() {
                Set<AnnotationDocumentState> ignoredStates = recommenderModel.getObject()
                        .getStatesIgnoredForTraining();

                return invert(ignoredStates);
            }

            private Set<AnnotationDocumentState> invert(Set<AnnotationDocumentState> states) {
                Set<AnnotationDocumentState> result = getAllPossibleDocumentStates();

                if (states == null) {
                    return result;
                }

                result.removeAll(states);
                return result;
            }
        };
        setModel((IModel) model);
        setChoices(asList(AnnotationDocumentState.values()));
        setPrefix("<div class=\"checkbox\">");
        setSuffix("</div>");
        setLabelPosition(AbstractChoice.LabelPosition.WRAP_AFTER);
        setChoices(asList(AnnotationDocumentState.values()));
        setChoiceRenderer(new EnumChoiceRenderer<>(this));
    }

    private static Set<AnnotationDocumentState> getAllPossibleDocumentStates()
    {
        return new HashSet<>(asList(AnnotationDocumentState.values()));
    }
}
