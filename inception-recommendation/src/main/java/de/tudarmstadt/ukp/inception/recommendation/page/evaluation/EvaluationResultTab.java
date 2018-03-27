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
package de.tudarmstadt.ukp.inception.recommendation.page.evaluation;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.EvaluationResult;

public class EvaluationResultTab
    extends Panel
{
    private static final long serialVersionUID = 7151761976918606160L;

    public EvaluationResultTab(String id)
    {
        this(id, null);
    }

    public EvaluationResultTab(String id, IModel<EvaluationResult> iModel)
    {
        super(id, iModel);

        add(new EvaluationResultPanel("evaluationPanelKnownData",
                LambdaModel.of(() -> iModel.getObject().getKnownDataResults())));
        add(new EvaluationResultPanel("evaluationPanelUnknownData", 
                LambdaModel.of(() -> iModel.getObject().getUnknownDataResults())));
    }
}
