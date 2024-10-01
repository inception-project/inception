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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.layer;

import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;

import java.io.IOException;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.bootstrap.IconToggleBox;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class LayerVisibilitySidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = 6127948490101336779L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AnnotationSchemaProperties annotationEditorProperties;
    private @SpringBean UserPreferencesService userPreferencesService;
    private @SpringBean UserDao userService;

    public LayerVisibilitySidebar(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        add(createLayerContainer("annotationLayers", LoadableDetachableModel.of(this::listLayers)));
    }

    private List<AnnotationLayer> listLayers()
    {
        return annotationService.listSupportedLayers(getModelObject().getProject()).stream() //
                .filter(AnnotationLayer::isEnabled) //
                .filter(layer -> !annotationEditorProperties.isLayerBlocked(layer)) //
                .toList();
    }

    private void actionToggleVisibility(AnnotationLayer aLayer, boolean aHidden,
            AjaxRequestTarget aTarget)
        throws IOException
    {
        getModelObject().getPreferences().setLayerVisible(aLayer, !aHidden);

        var sessionOwner = userService.getCurrentUsername();
        userPreferencesService.savePreferences(getModelObject(), sessionOwner);
        userPreferencesService.loadPreferences(getModelObject(), sessionOwner);

        getAnnotationPage().actionRefreshDocument(aTarget);
    }

    private ListView<AnnotationLayer> createLayerContainer(String aId,
            IModel<List<AnnotationLayer>> aLayers)
    {
        return new ListView<AnnotationLayer>(aId, aLayers)
        {
            private static final long serialVersionUID = -4040731191748923013L;

            @Override
            protected void populateItem(ListItem<AnnotationLayer> aItem)
            {
                var prefs = LayerVisibilitySidebar.this.getModelObject().getPreferences();
                var layer = aItem.getModelObject();
                var hiddenLayerIds = prefs.getHiddenAnnotationLayerIds();

                var layerVisible = new IconToggleBox("visibleToggle") //
                        .setCheckedIcon(FontAwesome5IconType.eye_s)
                        .setCheckedTitle(Model.of("Visible"))
                        .setUncheckedIcon(FontAwesome5IconType.eye_slash_s)
                        .setUncheckedTitle(Model.of("Hidden"))
                        .setModel(Model.of(!hiddenLayerIds.contains(layer.getId())));
                layerVisible.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        _target -> actionToggleVisibility(layer, layerVisible.getModelObject(),
                                _target)));
                aItem.add(layerVisible);

                aItem.add(new Label("name", layer.getUiName()));
            }
        };
    }
}
