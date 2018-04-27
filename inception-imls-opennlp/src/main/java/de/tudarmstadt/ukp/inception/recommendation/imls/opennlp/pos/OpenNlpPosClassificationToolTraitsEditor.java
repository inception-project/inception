/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.pos;

import java.io.Serializable;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationToolFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationToolRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public class OpenNlpPosClassificationToolTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = 1677462652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean ClassificationToolRegistry toolRegistry;
    
    private IModel<Recommender> recommender;
    private IModel<Traits> traits;
    private String classificationToolId;
    
    public OpenNlpPosClassificationToolTraitsEditor(String aId, ClassificationToolFactory aFactory,
            IModel<Recommender> aModel)
    {
        super(aId, aModel);
        
        recommender = aModel;
        traits = Model.of(readTraits());
        classificationToolId = aFactory.getId();

        Form<Traits> form = new Form<Traits>(MID_FORM, CompoundPropertyModel.of(traits))
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                writeTraits();
            }
        };
        add(form);
    }
    
    private Traits readTraits()
    {
        Traits result = new Traits();

        return result;
    }
    
    private void writeTraits()
    {
        OpenNlpPosClassificationToolTraits t = new OpenNlpPosClassificationToolTraits();
        getToolFactory().writeTraits(recommender.getObject(), t);
    }
    
    private ClassificationToolFactory getToolFactory()
    {
        return toolRegistry.getTool(classificationToolId);
    }

    private static class Traits implements Serializable
    {

    }
}
