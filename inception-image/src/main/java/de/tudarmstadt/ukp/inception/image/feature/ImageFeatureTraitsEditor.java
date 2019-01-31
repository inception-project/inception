/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.image.feature;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

/**
 * Component for editing the traits of image features in the feature detail editor
 * of the project settings.
 */
public class ImageFeatureTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = 824184844844414819L;

    public ImageFeatureTraitsEditor(String aId, ImageFeatureSupport aFS,
            IModel<AnnotationFeature> aFeatureModel)
    {
        super(aId, aFeatureModel);
    }
}
