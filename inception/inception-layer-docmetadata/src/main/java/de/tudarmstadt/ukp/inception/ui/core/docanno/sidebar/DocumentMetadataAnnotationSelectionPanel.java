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
package de.tudarmstadt.ukp.inception.ui.core.docanno.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.inception.annotation.events.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.Renderer;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.adapter.TypeUtil;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerAdapter;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;

public class DocumentMetadataAnnotationSelectionPanel
    extends Panel
{
    private static final long serialVersionUID = 8318858582025740458L;

    private static final Logger LOG = LoggerFactory
            .getLogger(DocumentMetadataAnnotationSelectionPanel.class);

    private static final String CID_LABEL = "label";
    private static final String CID_TYPE = "type";
    private static final String CID_ANNOTATIONS = "annotations";
    private static final String CID_LAYER = "layer";
    private static final String CID_CREATE = "create";
    private static final String CID_ANNOTATIONS_CONTAINER = "annotationsContainer";
    private static final String CID_ANNOTATION_DETAILS = "annotationDetails";
    private static final String CID_DELETE = "delete";

    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean AnnotationSchemaService annotationService;

    private final AnnotationPage annotationPage;
    private final CasProvider jcasProvider;
    private final IModel<Project> project;
    private final IModel<SourceDocument> sourceDocument;
    private final IModel<String> username;
    private final IModel<AnnotationLayer> selectedLayer;
    private final IModel<List<AnnotationListItem>> annotations;
    private final WebMarkupContainer annotationsContainer;
    private final AnnotationActionHandler actionHandler;
    private WebMarkupContainer selectedAnnotation;
    private DocumentMetadataAnnotationDetailPanel selectedDetailPanel;
    private int createdAnnotationAddress;
    private final AnnotatorState state;

    public DocumentMetadataAnnotationSelectionPanel(String aId, IModel<Project> aProject,
            IModel<SourceDocument> aDocument, IModel<String> aUsername, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage, AnnotationActionHandler aActionHandler,
            AnnotatorState aState)
    {
        super(aId, aProject);

        setOutputMarkupPlaceholderTag(true);

        annotationPage = aAnnotationPage;
        sourceDocument = aDocument;
        username = aUsername;
        jcasProvider = aCasProvider;
        project = aProject;
        selectedLayer = Model.of(selectableMetadataLayers().stream().findFirst().orElse(null));
        IModel<List<AnnotationLayer>> availableLayers = LoadableDetachableModel
                .of(this::selectableMetadataLayers);
        actionHandler = aActionHandler;
        state = aState;
        annotations = LoadableDetachableModel.of(this::listAnnotations);

        WebMarkupContainer content = new WebMarkupContainer("content");
        add(content);

        annotationsContainer = new WebMarkupContainer(CID_ANNOTATIONS_CONTAINER);
        annotationsContainer.setOutputMarkupId(true);
        annotationsContainer.add(createAnnotationList());
        annotationsContainer
                .add(visibleWhen(() -> !availableLayers.map(List::isEmpty).orElse(true).getObject()
                        || !annotations.map(List::isEmpty).orElse(true).getObject()));
        content.add(annotationsContainer);

        DropDownChoice<AnnotationLayer> layer = new DropDownChoice<>(CID_LAYER);
        layer.setModel(selectedLayer);
        layer.setChoices(availableLayers);
        layer.setChoiceRenderer(new ChoiceRenderer<>("uiName"));
        layer.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        layer.add(visibleWhen(() -> !availableLayers.map(List::isEmpty).orElse(true).getObject()));
        content.add(layer);

        content.add(new LambdaAjaxLink(CID_CREATE, this::actionCreate)
                .add(enabledWhen(() -> annotationPage.isEditable())) //
                .add(visibleWhen(
                        () -> !availableLayers.map(List::isEmpty).orElse(true).getObject())));

        WebMarkupContainer noLayersWarning = new WebMarkupContainer("noLayersWarning");
        noLayersWarning
                .add(visibleWhen(() -> availableLayers.map(List::isEmpty).orElse(true).getObject()
                        && annotations.map(List::isEmpty).orElse(true).getObject()));
        add(noLayersWarning);

    }

    public Project getModelObject()
    {
        return (Project) getDefaultModelObject();
    }

    private void actionCreate(AjaxRequestTarget aTarget) throws AnnotationException, IOException
    {
        try {
            annotationPage.ensureIsEditable();

            DocumentMetadataLayerAdapter adapter = (DocumentMetadataLayerAdapter) annotationService
                    .getAdapter(selectedLayer.getObject());
            CAS cas = jcasProvider.get();
            AnnotationBaseFS fs = adapter.add(sourceDocument.getObject(), username.getObject(),
                    cas);

            createdAnnotationAddress = fs.getAddress();
            annotationPage.writeEditorCas(cas);

            aTarget.add(annotationsContainer);

            findParent(AnnotationPageBase.class).actionRefreshDocument(aTarget);
        }
        catch (Exception e) {
            handleException(this, aTarget, e);
        }
    }

    private void actionDelete(AjaxRequestTarget aTarget,
            DocumentMetadataAnnotationDetailPanel aDetailPanel)
    {
        try {
            annotationPage.ensureIsEditable();

            // Load the boiler-plate
            CAS cas = jcasProvider.get();
            FeatureStructure fs = ICasUtil.selectFsByAddr(cas,
                    aDetailPanel.getModelObject().getId());
            AnnotationLayer layer = annotationService.findLayer(project.getObject(), fs);
            TypeAdapter adapter = annotationService.getAdapter(layer);

            // Perform actual actions
            adapter.delete(sourceDocument.getObject(), username.getObject(), cas, new VID(fs));

            // persist changes
            annotationPage.writeEditorCas(cas);

            if (selectedDetailPanel == aDetailPanel) {
                selectedAnnotation = null;
                selectedDetailPanel = null;
            }
            remove(aDetailPanel);

            findParent(AnnotationPageBase.class).actionRefreshDocument(aTarget);
            aTarget.add(annotationsContainer);
        }
        catch (Exception e) {
            handleException(this, aTarget, e);
        }
    }

    private void actionSelect(AjaxRequestTarget aTarget, WebMarkupContainer container,
            DocumentMetadataAnnotationDetailPanel detailPanel)
    {
        if (selectedAnnotation != null) {
            aTarget.add(selectedAnnotation);
        }

        if (selectedDetailPanel != null) {
            aTarget.add(selectedDetailPanel);
        }

        if (selectedAnnotation == container) {
            // if container is already selected, deselect and close annotation
            selectedAnnotation = null;
        }
        else {
            selectedAnnotation = container;
        }

        if (selectedDetailPanel == detailPanel) {
            // if detail panel is already selected, deselect and close annotation
            selectedDetailPanel = null;
        }
        else {
            selectedDetailPanel = detailPanel;
        }

        aTarget.add(container);
        aTarget.add(detailPanel);
    }

    private ListView<AnnotationListItem> createAnnotationList()
    {
        return new ListView<AnnotationListItem>(CID_ANNOTATIONS, annotations)
        {
            private static final long serialVersionUID = -6833373063896777785L;

            /**
             * Determines if new annotations should be rendered visible or not. For the
             * initialization of existing annotations this value should be false. Afterwards when
             * manually creating new annotations it should be true to immediately open them
             * afterwards. If there are no annotations at initialization it is initialized with true
             * else false.
             */
            @Override
            protected void populateItem(ListItem<AnnotationListItem> aItem)
            {
                aItem.setModel(CompoundPropertyModel.of(aItem.getModel()));

                VID vid = new VID(aItem.getModelObject().addr);

                WebMarkupContainer container = new WebMarkupContainer("annotation");
                aItem.add(container);

                DocumentMetadataAnnotationDetailPanel detailPanel = new DocumentMetadataAnnotationDetailPanel(
                        CID_ANNOTATION_DETAILS, Model.of(vid), sourceDocument, username,
                        jcasProvider, project, annotationPage, actionHandler, state);
                aItem.add(detailPanel);

                container.add(new AjaxEventBehavior("click")
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onEvent(AjaxRequestTarget aTarget)
                    {
                        actionSelect(aTarget, container, detailPanel);
                    }
                });

                detailPanel.add(visibleWhen(
                        () -> selectedAnnotation == container || aItem.getModelObject().singleton));

                if (createdAnnotationAddress == vid.getId()) {
                    createdAnnotationAddress = -1;
                    selectedAnnotation = container;
                    selectedDetailPanel = detailPanel;
                }

                WebMarkupContainer close = new WebMarkupContainer("close");
                close.add(visibleWhen(
                        () -> !detailPanel.isVisible() && !aItem.getModelObject().singleton));
                close.setOutputMarkupId(true);
                container.add(close);

                WebMarkupContainer open = new WebMarkupContainer("open");
                open.add(visibleWhen(
                        () -> detailPanel.isVisible() && !aItem.getModelObject().singleton));
                open.setOutputMarkupId(true);
                container.add(open);

                container.add(new Label(CID_TYPE, aItem.getModelObject().layer.getUiName()));
                container.add(new Label(CID_LABEL)
                        .add(visibleWhen(() -> !aItem.getModelObject().singleton)));
                container.setOutputMarkupId(true);

                aItem.add(new LambdaAjaxLink(CID_DELETE,
                        _target -> actionDelete(_target, detailPanel))
                                .add(visibleWhen(() -> !aItem.getModelObject().singleton))
                                .add(enabledWhen(annotationPage::isEditable))
                                .add(AttributeAppender.append("class",
                                        () -> annotationPage.isEditable() ? "" : "disabled")));

                aItem.setOutputMarkupId(true);
            }
        };
    }

    private List<AnnotationLayer> listMetadataLayers()
    {
        return annotationService.listAnnotationLayer(getModelObject()).stream()
                .filter(layer -> DocumentMetadataLayerSupport.TYPE.equals(layer.getType())
                        && layer.isEnabled())
                .collect(Collectors.toList());
    }

    private List<AnnotationLayer> selectableMetadataLayers()
    {
        return listMetadataLayers().stream()
                .filter(layer -> !getLayerSupport(layer).readTraits(layer).isSingleton())
                .collect(toList());
    }

    private DocumentMetadataLayerSupport getLayerSupport(AnnotationLayer aLayer)
    {
        return (DocumentMetadataLayerSupport) layerSupportRegistry.getLayerSupport(aLayer);
    }

    private List<AnnotationListItem> listAnnotations()
    {
        CAS cas;
        try {
            cas = jcasProvider.get();
        }
        catch (IOException e) {
            LOG.error("Unable to load CAS", e);
            return emptyList();
        }

        List<AnnotationListItem> items = new ArrayList<>();
        for (AnnotationLayer layer : listMetadataLayers()) {
            List<AnnotationFeature> features = annotationService.listSupportedFeatures(layer);
            TypeAdapter adapter = annotationService.getAdapter(layer);
            LayerSupport<?, ?> layerSupport = layerSupportRegistry.getLayerSupport(layer);
            Renderer renderer = layerSupport.createRenderer(layer,
                    () -> annotationService.listAnnotationFeature(layer));
            boolean singleton = getLayerSupport(layer).readTraits(layer).isSingleton();

            for (FeatureStructure fs : cas.select(adapter.getAnnotationType(cas))) {
                Map<String, String> renderedFeatures = renderer.renderLabelFeatureValues(adapter,
                        fs, features);
                String labelText = TypeUtil.getUiLabelText(renderedFeatures);
                items.add(
                        new AnnotationListItem(ICasUtil.getAddr(fs), labelText, layer, singleton));
            }
        }

        return items;
    }

    @OnEvent
    public void onFeatureValueUpdated(FeatureValueUpdatedEvent aEvent)
    {
        // If a feature value is updated refresh the annotation list since it might mean that
        // a label has changed
        if (selectedAnnotation != null) {
            aEvent.getRequestTarget().add(selectedAnnotation);
        }
        if (selectedDetailPanel != null) {
            aEvent.getRequestTarget().add(selectedDetailPanel);
        }

        findParent(AnnotationPageBase.class).actionRefreshDocument(aEvent.getRequestTarget());
    }

    protected static void handleException(Component aComponent, AjaxRequestTarget aTarget,
            Exception aException)
    {
        if (aTarget != null) {
            aTarget.addChildren(aComponent.getPage(), IFeedback.class);
        }

        try {
            throw aException;
        }
        catch (AnnotationException e) {
            aComponent.error("Error: " + e.getMessage());
            LOG.error("Error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
        catch (UIMAException e) {
            aComponent.error("Error: " + ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Error: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
        catch (Exception e) {
            aComponent.error("Error: " + e.getMessage());
            LOG.error("Error: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    private class AnnotationListItem
    {
        final int addr;
        final String label;
        final AnnotationLayer layer;
        final boolean singleton;

        public AnnotationListItem(int aAddr, String aLabel, AnnotationLayer aLayer,
                boolean aSingleton)
        {
            super();
            addr = aAddr;
            label = aLabel;
            layer = aLayer;
            singleton = aSingleton;
        }
    }
}
