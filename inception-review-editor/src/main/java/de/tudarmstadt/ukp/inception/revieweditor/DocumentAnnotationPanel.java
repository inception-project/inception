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

package de.tudarmstadt.ukp.inception.revieweditor;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class DocumentAnnotationPanel 
    extends Panel
{
    
    private static final String CID_ANNOTATION_TITLE = "annotationTitle";
    private static final String CID_FEATURES_CONTAINER = "featuresContainer";
    private static final String CID_FEATURES = "features";
    private static final String CID_FEATURE = "feature";

    public DocumentAnnotationPanel(String id, IModel<?> model, String aTitle) {
        super(id, model);
        
        add(new Label(CID_ANNOTATION_TITLE, aTitle));
    }
}
