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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.layers;

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenModelIsNotNull;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.LayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.SentenceLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapFileInputField;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.export.LayerImportExportUtils;
import de.tudarmstadt.ukp.inception.project.api.FeatureInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.schema.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.inception.support.wicket.WicketUtil;
import jakarta.persistence.NoResultException;

/**
 * A Panel Used to add Layers to a selected {@link Project} in the project settings page
 */
public class ProjectLayersPanel
    extends ProjectSettingsPanelBase
{
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = -7870526462864489252L;

    public static final String MID_FEATURE_SELECTION_FORM = "featureSelectionForm";
    public static final String MID_FEATURE_DETAIL_FORM = "featureDetailForm";
    private static final String MID_DIALOG = "dialog";

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;
    private @SpringBean AnnotationSchemaProperties annotationEditorProperties;

    private LayerSelectionPane layerSelectionPane;
    private FeatureSelectionForm featureSelectionForm;
    private LayerDetailForm layerDetailForm;
    private final FeatureDetailForm featureDetailForm;
    private final ImportLayerForm importLayerForm;

    private IModel<AnnotationLayer> selectedLayer;
    private IModel<AnnotationFeature> selectedFeature;

    public ProjectLayersPanel(String id, final IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);

        selectedLayer = Model.of();
        selectedFeature = Model.of();

        featureSelectionForm = new FeatureSelectionForm(MID_FEATURE_SELECTION_FORM,
                selectedFeature);
        queue(featureSelectionForm);

        featureDetailForm = new FeatureDetailForm(MID_FEATURE_DETAIL_FORM, selectedFeature);
        queue(featureDetailForm);

        layerSelectionPane = new LayerSelectionPane("layerSelectionPane", selectedLayer);
        queue(layerSelectionPane);

        layerDetailForm = new LayerDetailForm("layerDetailForm", selectedLayer,
                featureSelectionForm, featureDetailForm);
        queue(layerDetailForm);

        importLayerForm = new ImportLayerForm("importLayerForm");
        queue(importLayerForm);
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();

        layerDetailForm.setModelObject(null);
        featureDetailForm.setModelObject(null);
    }

    public class LayerSelectionPane
        extends WebMarkupContainer
    {
        private static final long serialVersionUID = -1L;

        private final Map<AnnotationLayer, String> colors = new HashMap<>();
        private final BootstrapModalDialog dialog;
        private final Select<AnnotationLayer> layerSelection;
        private final IModel<List<LayerInitializer>> layerInitializers;
        private final LambdaAjaxLink addButton;

        public LayerSelectionPane(String id, IModel<AnnotationLayer> aModel)
        {
            super(id, aModel);

            layerInitializers = LoadableDetachableModel.of(this::listLayerInitializers);
            add(LambdaBehavior.onDetach(layerInitializers::detach));

            dialog = new BootstrapModalDialog(MID_DIALOG);
            dialog.trapFocus();
            queue(dialog);

            queue(new DocLink("helpLinkLayers", "sect_projects_layers"));

            queue(new LambdaAjaxLink("create", this::actionCreateLayer));

            addButton = new LambdaAjaxLink("add", this::actionAddLayer);
            addButton.setOutputMarkupPlaceholderTag(true);
            addButton.add(visibleWhenNot(layerInitializers.map(List::isEmpty)));
            queue(addButton);

            layerSelection = new Select<>("layerSelection", aModel);
            var layers = new ListView<AnnotationLayer>("layers", LambdaModel.of(this::listLayers))
            {
                private static final long serialVersionUID = 8901519963052692214L;

                @Override
                protected void populateItem(final ListItem<AnnotationLayer> item)
                {
                    item.add(new SelectOption<AnnotationLayer>("layer",
                            new Model<>(item.getModelObject()))
                    {
                        private static final long serialVersionUID = 3095089418860168215L;

                        @Override
                        public void onComponentTagBody(MarkupStream markupStream,
                                ComponentTag openTag)
                        {
                            replaceComponentTagBody(markupStream, openTag,
                                    item.getModelObject().getUiName());
                        }
                    }.add(new AttributeModifier("style",
                            "color:" + colors.get(item.getModelObject()) + ";")));
                }
            };

            queue(layerSelection.add(layers));
            layerSelection.setOutputMarkupId(true);
            layerSelection.add(OnChangeAjaxBehavior.onChange(_target -> {
                featureDetailForm.setModelObject(null);

                // list and detail panel share the same model, but they are not
                // automatically notified of updates to the model unless the
                // updates go through their respective setModelObject() calls
                layerDetailForm.modelChanged();

                _target.add(layerDetailForm);
                _target.add(featureSelectionForm);
                _target.add(featureDetailForm);
            }));
        }

        private void actionCreateLayer(AjaxRequestTarget aTarget)
        {
            var layer = new AnnotationLayer();
            layer.setProject(ProjectLayersPanel.this.getModelObject());

            layerDetailForm.setModelObject(layer);
            featureDetailForm.setModelObject(null);

            aTarget.add(ProjectLayersPanel.this);

            // AjaxRequestTarget.focusComponent does not work. It sets the focus but the cursor
            // does not actually appear in the input field. However, using JQuery here works.
            aTarget.appendJavaScript(WicketUtil.wrapInTryCatch("$('#"
                    + layerDetailForm.getInitialFocusComponent().getMarkupId() + "').focus();"));
        }

        private void actionAddLayer(AjaxRequestTarget aTarget)
        {
            var dialogContent = new LayerTemplateSelectionDialogPanel(
                    BootstrapModalDialog.CONTENT_ID, getModel(), layerInitializers);
            dialog.open(dialogContent, aTarget);
        }

        private List<LayerInitializer> listLayerInitializers()
        {
            if (getModelObject() == null) {
                return emptyList();
            }

            return projectService.listProjectInitializers().stream()
                    .filter(initializer -> initializer instanceof LayerInitializer)
                    .map(LayerInitializer.class::cast)
                    .filter(initializer -> !initializer.alreadyApplied(getModelObject()))
                    .filter(initializer -> !(initializer instanceof TokenLayerInitializer)
                            || annotationEditorProperties.isTokenLayerEditable())
                    .filter(initializer -> !(initializer instanceof SentenceLayerInitializer)
                            || annotationEditorProperties.isSentenceLayerEditable())
                    .toList();
        }

        @OnEvent
        public void onLayerTemplateSelected(LayerTemplateSelectedEvent aEvent)
        {
            var target = aEvent.getTarget();
            var initializer = aEvent.getLayerInitializer();
            try {
                // target.add(initializersContainer);
                target.add(layerSelection);
                target.add(addButton);
                target.addChildren(getPage(), IFeedback.class);
                var request = ProjectInitializationRequest.builder().withProject(getModelObject())
                        .build();
                projectService.initializeProject(request, asList(initializer));
                success("Applied project initializer [" + initializer.getName() + "]");
            }
            catch (Exception e) {
                error("Error applying project initializer [" + initializer.getName() + "]: "
                        + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Error applying project initializer {}", initializer, e);
            }

            applicationEventPublisherHolder.get().publishEvent(new LayerConfigurationChangedEvent(
                    this, ProjectLayersPanel.this.getModelObject()));
        }

        private List<AnnotationLayer> listLayers()
        {
            var project = ProjectLayersPanel.this.getModelObject();

            if (project.getId() == null) {
                return new ArrayList<>();
            }

            var _layers = annotationService.listAnnotationLayer(project);

            if (!annotationEditorProperties.isTokenLayerEditable()) {
                try {
                    var tokenLayer = annotationService.findLayer(project, Token.class.getName());
                    _layers.remove(tokenLayer);
                }
                catch (NoResultException e) {
                    LOG.trace("Project has no Token type!", e);
                }
                catch (Exception e) {
                    LOG.error("Unexpected error trying to locate Token type", e);
                }
            }

            if (!annotationEditorProperties.isSentenceLayerEditable()) {
                try {
                    var sentenceLayer = annotationService.findLayer(project,
                            Sentence.class.getName());
                    _layers.remove(sentenceLayer);
                }
                catch (NoResultException e) {
                    LOG.trace("Project has no Sentence type!", e);
                }
                catch (Exception e) {
                    LOG.error("Unexpected error trying to locate Sentence type", e);
                }
            }

            for (var layer : _layers) {
                if (layer.isBuiltIn() && layer.isEnabled()) {
                    colors.put(layer, "green");
                }
                else if (layer.isEnabled()) {
                    colors.put(layer, "blue");
                }
                else {
                    colors.put(layer, "red");
                }
            }

            return _layers;
        }
    }

    private class ImportLayerForm
        extends Form<String>
    {
        private static final long serialVersionUID = -7777616763931128598L;

        private BootstrapFileInputField fileUpload;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ImportLayerForm(String id)
        {
            super(id);

            add(fileUpload = new BootstrapFileInputField("content", new ListModel<>()));
            fileUpload.getConfig().showPreview(false);
            fileUpload.getConfig().showUpload(false);
            fileUpload.getConfig().showRemove(false);
            fileUpload.setRequired(true);

            add(new LambdaAjaxButton("import", this::actionImport));
        }

        private void actionImport(AjaxRequestTarget aTarget, Form<String> aForm)
        {
            List<FileUpload> uploadedFiles = fileUpload.getFileUploads();
            Project project = ProjectLayersPanel.this.getModelObject();

            if (isEmpty(uploadedFiles)) {
                error("Please choose file with layer details before uploading");
                return;
            }
            else if (isNull(project.getId())) {
                error("Project not yet created, please save project details!");
                return;
            }
            for (FileUpload uploadedFile : uploadedFiles) {
                try (BufferedInputStream bis = IOUtils.buffer(uploadedFile.getInputStream())) {
                    byte[] buf = new byte[5];
                    bis.mark(buf.length + 1);
                    bis.read(buf, 0, buf.length);
                    bis.reset();

                    // If the file starts with an XML preamble, then we assume it is an UIMA
                    // type system file.
                    if (Arrays.equals(buf, new byte[] { '<', '?', 'x', 'm', 'l' })) {
                        importUimaTypeSystemFile(bis);
                    }
                    else {
                        User user = userRepository.getCurrentUser();
                        var layer = LayerImportExportUtils.importLayerFile(annotationService, user,
                                project, bis);
                        layerDetailForm.setModelObject(layer);
                    }
                }
                catch (Exception e) {
                    error("Error importing layers: " + ExceptionUtils.getRootCauseMessage(e));
                    aTarget.addChildren(getPage(), IFeedback.class);
                    LOG.error("Error importing layers", e);
                }
            }
            featureDetailForm.setVisible(false);
            aTarget.add(ProjectLayersPanel.this);
        }

        private void importUimaTypeSystemFile(InputStream aIS)
            throws IOException, InvalidXMLException, ResourceInitializationException
        {
            Project project = ProjectLayersPanel.this.getModelObject();
            TypeSystemDescription tsd = UIMAFramework.getXMLParser()
                    .parseTypeSystemDescription(new XMLInputSource(aIS, null));

            annotationService.importUimaTypeSystem(project, tsd);
        }
    }

    public class FeatureSelectionForm
        extends Form<AnnotationFeature>
    {
        private static final String CID_CREATE_FEATURE = "new";

        private static final long serialVersionUID = -1L;

        private LambdaAjaxLink btnMoveUp;
        private LambdaAjaxLink btnMoveDown;
        private ListChoice<AnnotationFeature> overviewList;
        private final BootstrapModalDialog dialog;
        private final IModel<List<FeatureInitializer>> featureInitializers;
        private final LambdaAjaxLink addButton;

        public FeatureSelectionForm(String id, IModel<AnnotationFeature> aModel)
        {
            super(id, aModel);

            setOutputMarkupPlaceholderTag(true);

            featureInitializers = LoadableDetachableModel.of(this::listFeatureInitializers);
            add(LambdaBehavior.onDetach(featureInitializers::detach));

            dialog = new BootstrapModalDialog(MID_DIALOG);
            dialog.trapFocus();
            queue(dialog);

            addButton = new LambdaAjaxLink("add", this::actionAddFeature);
            addButton.setOutputMarkupPlaceholderTag(true);
            addButton.add(visibleWhenNot(featureInitializers.map(List::isEmpty)));
            queue(addButton);

            add(new DocLink("featuresHelpLink", "sect_projects_layers_features"));

            overviewList = new ListChoice<AnnotationFeature>("feature")
            {
                private static final long serialVersionUID = 1L;
                {
                    setChoices(FeatureSelectionForm.this::listFeatures);
                    setModel(aModel);
                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = 4610648616450168333L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            return aObject.getUiName() + " : ["
                                    + StringUtils.substringAfterLast(aObject.getType(), ".") + "]";
                        }
                    });
                    setNullValid(false);
                    add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::onChange));
                }

                private void onChange(AjaxRequestTarget _target)
                {
                    // list and detail panel share the same model, but they are not
                    // automatically notified of updates to the model unless the
                    // updates go through their respective setModelObject() calls
                    featureDetailForm.modelChanged();
                    _target.add(featureDetailForm, btnMoveDown, btnMoveUp);
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            };
            add(overviewList);

            btnMoveUp = new LambdaAjaxLink("moveUp", this::moveTagUp);
            btnMoveUp.setOutputMarkupPlaceholderTag(true);
            btnMoveUp.add(visibleWhenModelIsNotNull(overviewList));
            add(btnMoveUp);

            btnMoveDown = new LambdaAjaxLink("moveDown", this::moveTagDown);
            btnMoveDown.setOutputMarkupPlaceholderTag(true);
            btnMoveDown.add(visibleWhenModelIsNotNull(overviewList));
            add(btnMoveDown);

            LambdaAjaxLink createButton = new LambdaAjaxLink(CID_CREATE_FEATURE,
                    this::actionCreateFeature);
            createButton.add(enabledWhen(() -> layerDetailForm.getModelObject() != null
                    && !layerDetailForm.getModelObject().isBuiltIn()
                    && !layerDetailForm.getModelObject().getType().equals(CHAIN_TYPE)

            ));
            add(createButton);
        }

        private void moveTagUp(AjaxRequestTarget aTarget)
        {
            @SuppressWarnings("unchecked")
            var tags = (List<AnnotationFeature>) overviewList.getChoices();
            int i = tags.indexOf(overviewList.getModelObject());

            if (i < 1) {
                return;
            }

            var tag = tags.remove(i);
            tags.add(i - 1, tag);

            updateFeatureRanks(aTarget, tags);
        }

        private void moveTagDown(AjaxRequestTarget aTarget)
        {
            @SuppressWarnings("unchecked")
            var tags = (List<AnnotationFeature>) overviewList.getChoices();
            int i = tags.indexOf(overviewList.getModelObject());

            if (i >= tags.size() - 1) {
                return;
            }

            var tag = tags.remove(i);
            tags.add(i + 1, tag);

            updateFeatureRanks(aTarget, tags);
        }

        private void updateFeatureRanks(AjaxRequestTarget aTarget,
                List<AnnotationFeature> aFeatures)
        {
            annotationService.updateFeatureRanks(selectedLayer.getObject(), aFeatures);

            var selected = overviewList.getModelObject();
            for (var t : aFeatures) {
                if (t.equals(selected)) {
                    selected.setRank(t.getRank());
                }
            }

            aTarget.add(overviewList, btnMoveUp, btnMoveDown);
        }

        private void actionCreateFeature(AjaxRequestTarget aTarget)
        {
            // cancel selection of feature list
            selectedFeature.setObject(null);

            var newFeature = new AnnotationFeature();
            newFeature.setLayer(layerDetailForm.getModelObject());
            newFeature.setProject(ProjectLayersPanel.this.getModelObject());
            featureDetailForm.setDefaultModelObject(newFeature);

            aTarget.add(featureSelectionForm);
            aTarget.add(featureDetailForm);
            // AjaxRequestTarget.focusComponent does not work. It sets the focus but the cursor does
            // not actually appear in the input field. However, using JQuery here works.
            aTarget.appendJavaScript(WicketUtil.wrapInTryCatch("$('#"
                    + featureDetailForm.getInitialFocusComponent().getMarkupId() + "').focus();"));
        }

        private void actionAddFeature(AjaxRequestTarget aTarget)
        {
            var dialogContent = new FeatureTemplateSelectionDialogPanel(
                    BootstrapModalDialog.CONTENT_ID, ProjectLayersPanel.this.getModel(),
                    featureInitializers);
            dialog.open(dialogContent, aTarget);
        }

        private List<AnnotationFeature> listFeatures()
        {
            var features = annotationService
                    .listAnnotationFeature(layerDetailForm.getModelObject());

            if (ChainLayerSupport.TYPE.equals(layerDetailForm.getModelObject().getType())
                    && !layerDetailForm.getModelObject().isLinkedListBehavior()) {
                var filtered = new ArrayList<AnnotationFeature>();
                for (var f : features) {
                    if (!COREFERENCE_RELATION_FEATURE.equals(f.getName())) {
                        filtered.add(f);
                    }
                }
                return filtered;
            }
            else {
                return features;
            }
        }

        private List<FeatureInitializer> listFeatureInitializers()
        {
            if (!selectedLayer.isPresent().getObject()) {
                return emptyList();
            }

            return projectService.listFeatureInitializers().stream()
                    .filter(initializer -> !initializer.alreadyApplied(selectedLayer.getObject()))
                    .toList();
        }

        @Override
        protected void onConfigure()
        {
            super.onConfigure();

            setVisible(selectedLayer.getObject() != null
                    && nonNull(selectedLayer.getObject().getId()));
        }

        @OnEvent
        public void onFeatureTemplateSelected(FeatureTemplateSelectedEvent aEvent)
        {
            var target = aEvent.getTarget();
            var initializer = aEvent.getLayerInitializer();
            try {
                target.add(overviewList);
                target.add(addButton);
                target.addChildren(getPage(), IFeedback.class);
                initializer.configure(selectedLayer.getObject());
                success("Applied feature initializer [" + initializer.getName() + "]");
            }
            catch (Exception e) {
                error("Error applying feature initializer [" + initializer.getName() + "]: "
                        + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Error applying feature initializer {}", initializer, e);
            }

            applicationEventPublisherHolder.get().publishEvent(new LayerConfigurationChangedEvent(
                    this, ProjectLayersPanel.this.getModelObject()));
        }
    }
}
