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
package de.tudarmstadt.ukp.inception.annotation.layer;

import java.io.Serializable;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;

public abstract class LayerTraitsEditor_ImplBase<T extends Serializable, S extends LayerSupport<?, T>>
    extends Panel
{
    private static final long serialVersionUID = 1721881254495474188L;

    private static final String MID_FORM = "form";

    private @SpringBean LayerSupportRegistry layerSupportRegistry;

    private CompoundPropertyModel<AnnotationLayer> layerModel;
    private CompoundPropertyModel<T> traitsModel;
    private String layerSupportId;
    private Form<T> form;

    public LayerTraitsEditor_ImplBase(String aId, LayerSupport<?, ?> aLayerSupport,
            IModel<AnnotationLayer> aLayerModel)
    {
        super(aId, aLayerModel);

        layerSupportId = aLayerSupport.getId();

        traitsModel = CompoundPropertyModel
                .of(getLayerSupport().readTraits(aLayerModel.getObject()));
        layerModel = CompoundPropertyModel.of(aLayerModel);

        form = new Form<T>(MID_FORM, getTraitsModel())
        {
            private static final long serialVersionUID = -3109239605783291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();

                LayerTraitsEditor_ImplBase.this.onSubmit();

                getLayerSupport().writeTraits(aLayerModel.getObject(), getTraitsModelObject());
            }
        };

        initializeForm(form);

        form.setOutputMarkupPlaceholderTag(true);
        add(form);
    }

    protected void onSubmit()
    {
        // Do not to anything be default
    }

    protected abstract void initializeForm(Form<T> aForm);

    public CompoundPropertyModel<AnnotationLayer> getLayerModel()
    {
        return layerModel;
    }

    public AnnotationLayer getLayerModelObject()
    {
        return layerModel.getObject();
    }

    public CompoundPropertyModel<T> getTraitsModel()
    {
        return traitsModel;
    }

    public T getTraitsModelObject()
    {
        return traitsModel.getObject();
    }

    @SuppressWarnings("unchecked")
    public S getLayerSupport()
    {
        return (S) layerSupportRegistry.getLayerSupport(layerSupportId);
    }
}
