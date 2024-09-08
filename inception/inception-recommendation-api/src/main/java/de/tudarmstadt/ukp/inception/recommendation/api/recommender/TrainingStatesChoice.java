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
package de.tudarmstadt.ukp.inception.recommendation.api.recommender;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.markup.html.form.AbstractChoice;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.value.AttributeMap;
import org.apache.wicket.util.value.IValueMap;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public class TrainingStatesChoice
    extends CheckBoxMultipleChoice<AnnotationDocumentState>
{
    private static final long serialVersionUID = 7627977162174971025L;

    public TrainingStatesChoice(String aId, IModel<Recommender> recommenderModel)
    {
        super(aId);

        // We need to invert the states in documentStates, as the recommender stores the
        // ones to ignore, not the ones to consider
        var model = new IModel<Set<AnnotationDocumentState>>()
        {
            private static final long serialVersionUID = 2894838629097952859L;

            @Override
            public void setObject(Set<AnnotationDocumentState> states)
            {
                // The model can be null after save and delete
                if (recommenderModel.getObject() != null) {
                    recommenderModel.getObject().setStatesIgnoredForTraining(invert(states));
                }
            }

            @Override
            public Set<AnnotationDocumentState> getObject()
            {
                var ignoredStates = recommenderModel.getObject().getStatesIgnoredForTraining();

                return invert(ignoredStates);
            }

            private Set<AnnotationDocumentState> invert(Set<AnnotationDocumentState> states)
            {
                var result = getAllPossibleDocumentStates();

                if (states == null) {
                    return result;
                }

                result.removeAll(states);
                return result;
            }
        };
        setModel((IModel) model);
        setChoices(asList(AnnotationDocumentState.values()));
        setPrefix("<div class=\"form-check form-switch\">");
        setSuffix("</div>");
        setLabelPosition(AbstractChoice.LabelPosition.AFTER);
        setChoiceRenderer(new EnumChoiceRenderer<>(this));
    }

    @Override
    protected IValueMap getAdditionalAttributesForLabel(int aIndex, AnnotationDocumentState aChoice)
    {
        var attributes = new AttributeMap();
        attributes.put("class", "form-check-label");
        return attributes;
    }

    @Override
    protected IValueMap getAdditionalAttributes(int aIndex, AnnotationDocumentState aChoice)
    {
        var attributes = new AttributeMap();
        attributes.put("class", "form-check-input");
        return attributes;
    }

    private static Set<AnnotationDocumentState> getAllPossibleDocumentStates()
    {
        return new HashSet<>(asList(AnnotationDocumentState.values()));
    }
}
