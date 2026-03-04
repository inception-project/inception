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
package de.tudarmstadt.ukp.inception.annotation.feature.hyperlink;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.UimaPrimitiveFeatureSupport_ImplBase;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

public class HyperlinkFeatureTraitsEditor
    extends GenericPanel<AnnotationFeature>
{
    private static final long serialVersionUID = 1L;

    private static final String MID_FORM = "form";

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    private String featureSupportId;
    private IModel<AnnotationFeature> feature;
    private CompoundPropertyModel<HyperlinkFeatureTraits> traits;

    public HyperlinkFeatureTraitsEditor(String aId,
            UimaPrimitiveFeatureSupport_ImplBase<HyperlinkFeatureTraits> aFS,
            IModel<AnnotationFeature> aFeature)
    {
        super(aId, aFeature);

        featureSupportId = aFS.getId();
        feature = aFeature;

        traits = CompoundPropertyModel.of(getFeatureSupport().readTraits(feature.getObject()));

        var form = new Form<HyperlinkFeatureTraits>(MID_FORM, traits)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                getFeatureSupport().writeTraits(feature.getObject(), traits.getObject());
            }
        };
        form.setOutputMarkupPlaceholderTag(true);
        add(form);

        // CRITICAL: Checkbox to enable/disable hyperlink mode
        var enabled = new CheckBox("enabled");
        enabled.setModel(PropertyModel.of(traits, "enabled"));
        form.add(enabled);

        // Checkbox for requiring protocol
        var requireProtocol = new CheckBox("requireProtocol");
        requireProtocol.setModel(PropertyModel.of(traits, "requireProtocol"));
        form.add(requireProtocol);

        // Checkbox for allowing relative URLs
        var allowRelativeUrls = new CheckBox("allowRelativeUrls");
        allowRelativeUrls.setModel(PropertyModel.of(traits, "allowRelativeUrls"));
        form.add(allowRelativeUrls);

        // Checkbox for opening in new tab
        var openInNewTab = new CheckBox("openInNewTab");
        openInNewTab.setModel(PropertyModel.of(traits, "openInNewTab"));
        form.add(openInNewTab);
    }

    @SuppressWarnings("unchecked")
    private UimaPrimitiveFeatureSupport_ImplBase<HyperlinkFeatureTraits> getFeatureSupport()
    {
        return (UimaPrimitiveFeatureSupport_ImplBase<HyperlinkFeatureTraits>) featureSupportRegistry
                .getExtension(featureSupportId).orElseThrow();
    }
}
