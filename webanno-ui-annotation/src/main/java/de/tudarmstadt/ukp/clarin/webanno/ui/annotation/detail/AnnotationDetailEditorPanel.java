/*
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil.getAdapter;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.findWindowStartCenteringOnSelection;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getFeature;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getNextSentenceAddress;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isSame;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAt;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setFeature;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormValidatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;

import com.googlecode.wicket.kendo.ui.form.TextField;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.Selection;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.SpanAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.JavascriptUtils;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.Evaluator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.RulesIndicator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ValuesGenerator;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.DefaultFocusBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.DefaultFocusBehavior2;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DeleteOrReplaceAnnotationModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.editor.BooleanFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.editor.LinkFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.editor.NumberFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.editor.TextFeatureEditor;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Annotation Detail Editor Panel.
 *
 */
public class AnnotationDetailEditorPanel
    extends Panel
    implements AnnotationActionHandler
{
    private static final long serialVersionUID = 7324241992353693848L;
    private static final Logger LOG = LoggerFactory.getLogger(AnnotationDetailEditorPanel.class);

    private @SpringBean ProjectService projectService;
    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean AnnotationSchemaService annotationService;

    private AnnotationFeatureForm annotationFeatureForm;

    /**
     * Function to return tooltip using jquery
     * Docs for the JQuery tooltip widget that we configure below:
     * https://api.jqueryui.com/tooltip/
     */
    public static final String FUNCTION_FOR_TOOLTIP = "function() { return "
            + "'<div class=\"tooltip-title\">'+($(this).text() "
            + "? $(this).text() : 'no title')+'</div>"
            + "<div class=\"tooltip-content tooltip-pre\">'+($(this).attr('title') "
            + "? $(this).attr('title') : 'no description' )+'</div>' }";

    public AnnotationDetailEditorPanel(String id, IModel<AnnotatorState> aModel)
    {
        super(id, aModel);
        
        setOutputMarkupId(true);
        
        annotationFeatureForm = new AnnotationFeatureForm("annotationFeatureForm", getModel());
        annotationFeatureForm.setOutputMarkupId(true);
        annotationFeatureForm.add(new AjaxFormValidatingBehavior("submit") { 
			private static final long serialVersionUID = -5642108496844056023L;

			@Override 
            protected void onSubmit(AjaxRequestTarget aTarget) { 
                try {
                    JCas jCas = getEditorCas();
                    actionCreateOrUpdate(aTarget, jCas);
                }
                catch (Exception e) {
                    handleException(annotationFeatureForm, aTarget, e);
                }
            } 
        }); 
        add(annotationFeatureForm);
    }

    private boolean isAnnotationFinished()
    {
        AnnotatorState state = getModelObject();
        
        if (state.getMode().equals(Mode.CURATION)) {
            return state.getDocument().getState().equals(SourceDocumentState.CURATION_FINISHED);
        }
        else {
            return documentService.isAnnotationFinished(state.getDocument(), state.getUser());
        }
    }

    public class AnnotationFeatureForm
        extends Form<AnnotatorState>
    {
        private static final long serialVersionUID = 3635145598405490893L;
        
        // Add "featureEditorPanel" to AjaxRequestTargets instead of "featureEditorPanelContent"
        private WebMarkupContainer featureEditorPanel;
        private FeatureEditorPanelContent featureEditorPanelContent;
        
        private CheckBox forwardAnnotationCheck;
        private AjaxButton deleteButton;
        private AjaxButton reverseButton;
        private LayerSelector layerSelector;   
        private String selectedTag = "";
        private Label selectedAnnotationLayer;
        private TextField<String> forwardAnnotationText;
        private ModalWindow deleteModal;
        private Label selectedTextLabel;
        private List<AnnotationLayer> annotationLayers = new ArrayList<AnnotationLayer>();

        public AnnotationFeatureForm(String id, IModel<AnnotatorState> aBModel)
        {
            super(id, new CompoundPropertyModel<AnnotatorState>(aBModel));

            add(forwardAnnotationCheck = new CheckBox("forwardAnnotation")
            {
                private static final long serialVersionUID = 8908304272310098353L;

                {
                    setOutputMarkupId(true);
                    add(new AjaxFormComponentUpdatingBehavior("change")
                    {
                        private static final long serialVersionUID = 5179816588460867471L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget aTarget)
                        {
                            updateForwardAnnotation();
                            if(AnnotationFeatureForm.this.getModelObject().isForwardAnnotation()){
                                aTarget.appendJavaScript(JavascriptUtils.getFocusScript(forwardAnnotationText));
                                selectedTag = "";
                            }
                        }
                    });
                }
                
                @Override
                protected void onConfigure()
                {
                    super.onConfigure();

                    setEnabled(isForwardable());
                    updateForwardAnnotation();
                }
            });

            add(new Label("noAnnotationWarning", "No annotation selected!")
            {
                private static final long serialVersionUID = -6046409838139863541L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(!AnnotationFeatureForm.this.getModelObject().getSelection()
                            .getAnnotation().isSet());
                }
            });

            add(deleteButton = new AjaxButton("delete")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    
                    AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
                    
                    setVisible(state.getSelection().getAnnotation().isSet());

                    // Avoid deleting in read-only layers
                    setEnabled(state.getSelectedAnnotationLayer() != null
                            && !state.getSelectedAnnotationLayer().isReadonly());
                }

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    try {
                        AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
                        
                        JCas jCas = getEditorCas();
                        AnnotationFS fs = selectByAddr(jCas, state.getSelection().getAnnotation().getId());

                        AnnotationLayer layer = state.getSelectedAnnotationLayer();
                        TypeAdapter adapter = getAdapter(annotationService, layer);
                        if (adapter instanceof SpanAdapter && getAttachedRels(jCas, fs, layer).size() > 0) {
                            deleteModal.setTitle("Are you sure you like to delete all attached relations to this span annotation?");
                            deleteModal.setContent(new DeleteOrReplaceAnnotationModalPanel(
                                    deleteModal.getContentId(), state, deleteModal,
                                    AnnotationDetailEditorPanel.this,
                                    state.getSelectedAnnotationLayer(), false));
                            deleteModal.show(aTarget);
                        }
                        else {
                            actionDelete(aTarget);
                        }
                    }
                    catch (Exception e) {
                        handleException(this, aTarget, e);
                    }
                }
            });

            add(reverseButton = new AjaxButton("reverse")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();

                    AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
                    
                    setVisible(state.getSelection().getAnnotation().isSet()
                            && state.getSelection().isArc()
                            && state.getSelectedAnnotationLayer().getType()
                                    .equals(WebAnnoConst.RELATION_TYPE));

                    // Avoid reversing in read-only layers
                    setEnabled(state.getSelectedAnnotationLayer() != null
                            && !state.getSelectedAnnotationLayer().isReadonly());
                }

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                    try {
                        actionReverse(aTarget);
                    }
                    catch (Exception e) {
                        handleException(this, aTarget, e);
                    }
                }
            });
            reverseButton.setOutputMarkupPlaceholderTag(true);
            
            add(new AjaxButton("clear")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(AnnotationFeatureForm.this.getModelObject().getSelection()
                            .getAnnotation().isSet());
                }

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    aTarget.addChildren(getPage(), FeedbackPanel.class);

                    try {
                        actionClear(aTarget);
                    }
                    catch (Exception e) {
                        handleException(this, aTarget, e);
                    }
                }
            });

            add(layerSelector = new LayerSelector("defaultAnnotationLayer", annotationLayers));

            featureEditorPanel = new WebMarkupContainer("featureEditorsContainer")
            {
                private static final long serialVersionUID = 8908304272310098353L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(AnnotationFeatureForm.this.getModelObject().getSelection()
                            .getAnnotation().isSet());
                }
            };
            // Add placeholder since wmc might start out invisible. Without the placeholder we
            // cannot make it visible in an AJAX call
            featureEditorPanel.setOutputMarkupPlaceholderTag(true);
            featureEditorPanel.setOutputMarkupId(true);
            
            featureEditorPanel.add(new Label("noFeaturesWarning", "No features available!") {
                private static final long serialVersionUID = 4398704672665066763L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(AnnotationFeatureForm.this.getModelObject().getFeatureStates()
                            .isEmpty());
                }
            });
            
            featureEditorPanelContent = new FeatureEditorPanelContent("featureValues");
            featureEditorPanel.add(featureEditorPanelContent);
            
			forwardAnnotationText = new TextField<String>("forwardAnno");
			forwardAnnotationText.setOutputMarkupId(true);
			forwardAnnotationText.add(new AjaxFormComponentUpdatingBehavior("keyup") {
				private static final long serialVersionUID = 4554834769861958396L;
				
                @Override
                protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
                {
                    super.updateAjaxAttributes(attributes);

                    IAjaxCallListener listener = new AjaxCallListener()
                    {
                        private static final long serialVersionUID = -7968540662654079601L;

                        @Override
                        public CharSequence getPrecondition(Component component)
                        {
                            return "var keycode = Wicket.Event.keyCode(attrs.event);"
                                    + "    return true;";
                        }
                    };
                    attributes.getAjaxCallListeners().add(listener);

                    attributes.getDynamicExtraParameters()
                            .add("var eventKeycode = Wicket.Event.keyCode(attrs.event);"
                                    + "return {keycode: eventKeycode};");
                    attributes.setAllowDefault(true);
                }
				   
				@Override
                protected void onUpdate(AjaxRequestTarget aTarget) {	
					final Request request = RequestCycle.get().getRequest();
		            final String jsKeycode = request.getRequestParameters()
		                            .getParameterValue("keycode").toString("");
		            if (jsKeycode.equals("32")){
                        try {
                            JCas jCas = getEditorCas();
                            actionCreateOrUpdate(aTarget, jCas);
                            selectedTag = "";
                        }
                        catch (Exception e) {
                            handleException(forwardAnnotationText, aTarget, e);
                        }
		            	return;
		            }
		            if (jsKeycode.equals("13")){
		            	selectedTag ="";
		            	return;
		            }
					selectedTag = (forwardAnnotationText.getModelObject() == null ? ""
							: forwardAnnotationText.getModelObject().charAt(0)) + selectedTag;
					Map<String, String> bindTags = getBindTags();
					if (!bindTags.isEmpty()) {
				        List<FeatureState> featureStates = getModelObject().getFeatureStates();
					    featureStates.get(0).value = getKeyBindValue(selectedTag, bindTags);
					}
					aTarget.add(forwardAnnotationText);
					aTarget.add(featureEditorPanelContent.get(0));
				}
			});
            forwardAnnotationText.setOutputMarkupId(true);
            forwardAnnotationText.add(new AttributeAppender("style", "opacity:0", ";"));
            // forwardAnno.add(new AttributeAppender("style", "filter:alpha(opacity=0)", ";"));
            add(forwardAnnotationText);
            
            // the selected text for annotation
            selectedTextLabel = new Label("selectedText", PropertyModel.of(getModelObject(),
                    "selection.text"));
            selectedTextLabel.setOutputMarkupId(true);
            featureEditorPanel.add(selectedTextLabel);
            
            featureEditorPanel.add(new Label("layerName","Layer"){
                private static final long serialVersionUID = 6084341323607243784L;
                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(AnnotationFeatureForm.this.getModelObject().getPreferences()
                            .isRememberLayer());
                }
                
            });
            featureEditorPanel.setOutputMarkupId(true);

            // the annotation layer for the selected annotation
            selectedAnnotationLayer = new Label("selectedAnnotationLayer", new Model<String>())
            {
                private static final long serialVersionUID = 4059460390544343324L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(AnnotationFeatureForm.this.getModelObject().getPreferences()
                            .isRememberLayer());
                }

            };
            selectedAnnotationLayer.setOutputMarkupId(true);
            featureEditorPanel.add(selectedAnnotationLayer);
            
            add(featureEditorPanel);
            
            add(deleteModal = new ModalWindow("yesNoModal"));
            deleteModal.setOutputMarkupId(true);

            deleteModal.setInitialWidth(600);
            deleteModal.setInitialHeight(50);
            deleteModal.setResizable(true);
            deleteModal.setWidthUnit("px");
            deleteModal.setHeightUnit("px");
            deleteModal.setTitle("Are you sure you want to delete the existing annotation?");
        }
        
        @Override
        protected void onConfigure()
        {
            super.onConfigure();

            // Avoid reversing in read-only layers
            setEnabled(getModelObject().getDocument() != null && !isAnnotationFinished());
        }
        
        private String getKeyBindValue(String aKey, Map<String, String> aBindTags){
            // check if all the key pressed are the same character
            // if not, just check a Tag for the last char pressed
            if(aKey.isEmpty()){
                return aBindTags.get(aBindTags.keySet().iterator().next());
            }
            char prevC = aKey.charAt(0);
            for(char ch:aKey.toCharArray()){
                if(ch!=prevC){
                    break;
                }
            }
            
            if (aBindTags.get(aKey)!=null){
                return aBindTags.get(aKey);
            }
            // re-cycle suggestions
            if(aBindTags.containsKey(aKey.substring(0,1))){
                selectedTag = aKey.substring(0,1);
                return aBindTags.get(aKey.substring(0,1));
            }
            // set it to the first in the tag list , when arbitrary key is pressed
            return aBindTags.get(aBindTags.keySet().iterator().next());
        }
        
        private Map<String, String> getBindTags()
        {
            AnnotationFeature f = annotationService
                    .listAnnotationFeature(getModelObject().getSelectedAnnotationLayer()).get(0);
            TagSet tagSet = f.getTagset();
            Map<Character, String> tagNames = new LinkedHashMap<>();
            Map<String, String> bindTag2Key = new LinkedHashMap<>();
            for (Tag tag : annotationService.listTags(tagSet)) {
                if (tagNames.containsKey(tag.getName().toLowerCase().charAt(0))) {
                    String oldBinding = tagNames.get(tag.getName().toLowerCase().charAt(0));
                    String newBinding = oldBinding + tag.getName().toLowerCase().charAt(0);
                    tagNames.put(tag.getName().toLowerCase().charAt(0), newBinding);
                    bindTag2Key.put(newBinding, tag.getName());
                }
                else {
                    tagNames.put(tag.getName().toLowerCase().charAt(0),
                            tag.getName().toLowerCase().substring(0, 1));
                    bindTag2Key.put(tag.getName().toLowerCase().substring(0, 1), tag.getName());
                }
            }
            return bindTag2Key;

        }
        
        private void updateForwardAnnotation()
        {
            AnnotatorState state = getModelObject();

            if (state.getSelectedAnnotationLayer() != null
                    && !state.getSelectedAnnotationLayer().isLockToTokenOffset()) {
                state.setForwardAnnotation(false);// no forwarding for
                                                  // sub-/multitoken annotation
            }
            else {
                state.setForwardAnnotation(state.isForwardAnnotation());
            }
        }
        
        private void updateLayersDropdown()
        {
            LOG.trace(String.format("updateLayersDropdown()"));
            
            AnnotatorState state = getModelObject();
            
            annotationLayers.clear();
            AnnotationLayer l = null;
            for (AnnotationLayer layer : state.getAnnotationLayers()) {
                if (!layer.isEnabled() || layer.isReadonly()
                        || layer.getName().equals(Token.class.getName())) {
                    continue;
                }
                if (layer.getType().equals(WebAnnoConst.SPAN_TYPE)) {
                    annotationLayers.add(layer);
                    l = layer;
                }
                // manage chain type
                else if (layer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                    for (AnnotationFeature feature : annotationService.listAnnotationFeature(layer)) {
                        if (!feature.isEnabled()) {
                            continue;
                        }
                        if (feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
                            annotationLayers.add(layer);
                        }

                    }
                }
                // chain
            }
            if (state.getDefaultAnnotationLayer() != null) {
                state.setSelectedAnnotationLayer(state.getDefaultAnnotationLayer());
            }
            else if (l != null) {
                state.setSelectedAnnotationLayer(l);
            }
        }
        
        private void updateRememberLayer()
        {
            LOG.trace(String.format("updateRememberLayer()"));
            
            AnnotatorState state = getModelObject();
            
            if (state.getPreferences().isRememberLayer()) {
                if (state.getDefaultAnnotationLayer() == null) {
                    state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
                }
            } 
            else if (!state.getSelection().isArc()) {
                state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
            }
            
            // if no layer is selected in Settings
            if (state.getSelectedAnnotationLayer() != null) {
                selectedAnnotationLayer.setDefaultModelObject(
                        state.getSelectedAnnotationLayer().getUiName());
            } 
        }
        
        public class LayerSelector
            extends DropDownChoice<AnnotationLayer>
        {
            private static final long serialVersionUID = 2233133653137312264L;

            public LayerSelector(String aId, List<? extends AnnotationLayer> aChoices)
            {
                super(aId, aChoices);
                setOutputMarkupId(true);
                setChoiceRenderer(new ChoiceRenderer<AnnotationLayer>("uiName"));
                add(new AjaxFormComponentUpdatingBehavior("change")
                {
                    private static final long serialVersionUID = 5179816588460867471L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget aTarget)
                    {
                        AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
                        
                        // If "remember layer" is set, the we really just update the selected
                        // layer...
                        // we do not touch the selected annotation not the annotation detail panel
                        if (state.getPreferences().isRememberLayer()) {
                            state.setSelectedAnnotationLayer(getModelObject());
                        }
                        // If "remember layer" is not set, then changing the layer means that we
                        // want to change the type of the currently selected annotation
                        else if (!state.getSelectedAnnotationLayer().equals(getModelObject())
                                && state.getSelection().getAnnotation().isSet()) {
                            if (state.getSelection().isArc()) {
                                try {
                                    actionClear(aTarget);
                                }
                                catch (Exception e) {
                                    handleException(LayerSelector.this, aTarget, e);
                                }
                            }
                            else {
                                AnnotationFeatureForm.this.deleteModal
                                        .setContent(new DeleteOrReplaceAnnotationModalPanel(
                                                AnnotationFeatureForm.this.deleteModal.getContentId(),
                                                state, AnnotationFeatureForm.this.deleteModal,
                                                AnnotationDetailEditorPanel.this, getModelObject(),
                                                true));

                                AnnotationFeatureForm.this.deleteModal.setWindowClosedCallback(
                                        new ModalWindow.WindowClosedCallback()
                                        {
                                            private static final long serialVersionUID = 4364820331676014559L;

                                            @Override
                                            public void onClose(AjaxRequestTarget target)
                                            {
                                                target.add(AnnotationFeatureForm.this);
                                            }
                                        });
                                AnnotationFeatureForm.this.deleteModal.show(aTarget);
                            }
                        }
                        // If no annotation is selected, then prime the annotation detail panel for
                        // the new type
                        else {
                            state.setSelectedAnnotationLayer(getModelObject());
                            AnnotationFeatureForm.this.selectedAnnotationLayer
                                    .setDefaultModelObject(getModelObject().getUiName());
                            aTarget.add(AnnotationFeatureForm.this.selectedAnnotationLayer);
                            clearFeatureEditorModels(aTarget);
                        }
                    }
                });
            }
        }
        
        public class FeatureEditorPanelContent
            extends RefreshingView<FeatureState>
        {
            private static final long serialVersionUID = -8359786805333207043L;

            public FeatureEditorPanelContent(String aId)
            {
                super(aId);
                setOutputMarkupId(true);
                // This strategy caches items as long as the panel exists. This is important to
                // allow the Kendo ComboBox datasources to be re-read when constraints change the
                // available tags.
                setItemReuseStrategy(new CachingReuseStrategy());
            }

            @SuppressWarnings("rawtypes")
            @Override
            protected void populateItem(final Item<FeatureState> item)
            {
                LOG.trace(String.format("FeatureEditorPanelContent.populateItem("
                        + item.getModelObject().feature.getUiName() + ": "
                        + item.getModelObject().value + ")"));
                
                // Feature editors that allow multiple values may want to update themselves,
                // e.g. to add another slot.
                item.setOutputMarkupId(true);

                final FeatureState featureState = item.getModelObject();

                final FeatureEditor frag;
                switch (featureState.feature.getMultiValueMode()) {
                case NONE: {
                    switch (featureState.feature.getType()) {
                    case CAS.TYPE_NAME_INTEGER: {
                        frag = new NumberFeatureEditor("editor", "numberFeatureEditor",
                                AnnotationFeatureForm.this, item.getModel());
                        break;
                    }
                    case CAS.TYPE_NAME_FLOAT: {
                        frag = new NumberFeatureEditor("editor", "numberFeatureEditor",
                                AnnotationFeatureForm.this, item.getModel());
                        break;
                    }
                    case CAS.TYPE_NAME_BOOLEAN: {
                        frag = new BooleanFeatureEditor("editor", "booleanFeatureEditor",
                                AnnotationFeatureForm.this, item.getModel());
                        break;
                    }
                    case CAS.TYPE_NAME_STRING: {
                        frag = new TextFeatureEditor("editor", "textFeatureEditor",
                                AnnotationFeatureForm.this, item.getModel());
                        break;
                    }
                    default:
                        throw new IllegalArgumentException(
                            "Unsupported type [" + featureState.feature.getType()
                                    + "] on feature [" + featureState.feature.getName() + "]");
                    }
                    break;
                }
                case ARRAY: {
                    switch (featureState.feature.getLinkMode()) {
                    case WITH_ROLE: {
                        // If it is none of the primitive types, it must be a link feature
                        frag = new LinkFeatureEditor("editor", "linkFeatureEditor",
                                AnnotationDetailEditorPanel.this, item.getModel());
                        break;

                    }
                    default:
                    throw new IllegalArgumentException(
                            "Unsupported link mode [" + featureState.feature.getLinkMode()
                                    + "] on feature [" + featureState.feature.getName() + "]");
                    }
                    break;
                }
                default:
                throw new IllegalArgumentException("Unsupported multi-value mode ["
                        + featureState.feature.getMultiValueMode() + "] on feature ["
                        + featureState.feature.getName() + "]");
                }

                if (!featureState.feature.getLayer().isReadonly()) {
                    AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
                    
                    // Whenever it is updating an annotation, it updates automatically when a
                    // component for the feature lost focus - but updating is for every component
                    // edited LinkFeatureEditors must be excluded because the auto-update will break
                    // the ability to add slots. Adding a slot is NOT an annotation action.
                    if (state.getSelection().getAnnotation().isSet()
                            && !(frag instanceof LinkFeatureEditor)) {
                        addAnnotateActionBehavior(frag, "change");
                    }
                    else if (!(frag instanceof LinkFeatureEditor)) {
                        addRefreshFeaturePanelBehavior(frag, "change");
                    }

                    // Put focus on hidden input field if we are in forward-mode
                    if (state.isForwardAnnotation()) {
                        AnnotationFeatureForm.this.forwardAnnotationText
                                .add(new DefaultFocusBehavior2());
                    }
                    // Put focus on first component if we select an existing annotation or create a
                    // new one
                    else if (item.getIndex() == 0
                            && SpanAnnotationResponse.is(state.getAction().getUserAction())) {
                        frag.getFocusComponent().add(new DefaultFocusBehavior());
                    }
                    // Restore/preserve focus when tabbing through the feature editors
                    else if (state.getAction().getUserAction() == null) {
                        AjaxRequestTarget target = RequestCycle.get()
                                .find(AjaxRequestTarget.class);
                        if (target != null && frag.getFocusComponent().getMarkupId()
                                .equals(target.getLastFocusedElementId())) {
                            target.focusComponent(frag.getFocusComponent());
                        }
                    }

                    // Add tooltip on label
                    StringBuilder tooltipTitle = new StringBuilder();
                    tooltipTitle.append(featureState.feature.getUiName());
                    if (featureState.feature.getTagset() != null) {
                        tooltipTitle.append(" (");
                        tooltipTitle.append(featureState.feature.getTagset().getName());
                        tooltipTitle.append(')');
                    }

                    Component labelComponent = frag.getLabelComponent();
                    labelComponent.add(new AttributeAppender("style", "cursor: help", ";"));
                    labelComponent.add(new DescriptionTooltipBehavior(tooltipTitle.toString(),
                            featureState.feature.getDescription()));
                }
                else {
                    frag.getFocusComponent().setEnabled(false);
                }

                // We need to enable the markup ID here because we use it during the AJAX behavior
                // that
                // automatically saves feature editors on change/blur. Check
                // addAnnotateActionBehavior.
                frag.setOutputMarkupId(true);
                item.add(frag);
            }

            private void addRefreshFeaturePanelBehavior(final FeatureEditor aFrag, String aEvent)
            {
                aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior(aEvent)
                {
                    private static final long serialVersionUID = 5179816588460867471L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget aTarget)
                    {
                        aTarget.add(featureEditorPanel);
                    }
                });
            }

            private void addAnnotateActionBehavior(final FeatureEditor aFrag, String aEvent)
            {
                aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior(aEvent)
                {
                    private static final long serialVersionUID = 5179816588460867471L;

                    @Override
                    protected void updateAjaxAttributes(AjaxRequestAttributes aAttributes)
                    {
                        super.updateAjaxAttributes(aAttributes);
                        // When focus is on a feature editor and the user selects a new annotation,
                        // there is a race condition between the saving the value of the feature
                        // editor and the loading of the new annotation. Delay the feature editor
                        // save to give preference to loading the new annotation.
                        aAttributes.setThrottlingSettings(new ThrottlingSettings(getMarkupId(),
                                Duration.milliseconds(250), true));
                        aAttributes.getAjaxCallListeners().add(new AjaxCallListener()
                        {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public CharSequence getPrecondition(Component aComponent)
                            {
                                // If the panel refreshes because the user selects a new annotation,
                                // the annotation editor panel is updated for the new annotation
                                // first (before saving values) because of the delay set above. When
                                // the delay is over, we can no longer save the value because the
                                // old component is no longer there. We use the markup id of the
                                // editor fragments to check if the old component is still there
                                // (i.e. if the user has just tabbed to a new field) or if the old
                                // component is gone (i.e. the user selected/created another
                                // annotation). If the old component is no longer there, we abort
                                // the delayed save action.
                                return "return $('#" + aFrag.getMarkupId() + "').length > 0;";
                            }
                        });
                    }

                    @Override
                    protected void onUpdate(AjaxRequestTarget aTarget)
                    {
                        try {
                            AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
                            if (state.getConstraints() != null) {
                                // Make sure we update the feature editor panel because due to
                                // constraints the contents may have to be re-rendered
                                aTarget.add(AnnotationFeatureForm.this.featureEditorPanel);
                            }
                            JCas jCas = getEditorCas();
                            actionCreateOrUpdate(aTarget, jCas);
                        }
                        catch (Exception e) {
                            handleException(FeatureEditorPanelContent.this, aTarget, e);
                        }
                    }
                });
            }

            @Override
            protected Iterator<IModel<FeatureState>> getItemModels()
            {
                List<FeatureState> featureStates = AnnotationFeatureForm.this.getModelObject()
                        .getFeatureStates();
                
                ModelIteratorAdapter<FeatureState> i = new ModelIteratorAdapter<FeatureState>(
                        featureStates)
                {
                    @Override
                    protected IModel<FeatureState> model(FeatureState aObject)
                    {
                        return FeatureStateModel.of(AnnotationFeatureForm.this.getModel(), aObject);
                    }
                };
                return i;
            }
        }
    }

    private void createNewAnnotation(AjaxRequestTarget aTarget, TypeAdapter aAdapter, JCas aJCas)
        throws AnnotationException, UIMAException, ClassNotFoundException, IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getSelection().isArc()) {
            if (aAdapter instanceof SpanAdapter) {
                error("Layer [" + aAdapter.getLayer().getUiName()
                        + "] does not support arc annotation.");
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                return;
            }
            else if (aAdapter instanceof ArcAdapter) {
                createNewRelationAnnotation(aTarget, (ArcAdapter) aAdapter, aJCas);
            }
            else if (aAdapter instanceof ChainAdapter) {
                createNewChainLinkAnnotation(aTarget, (ChainAdapter) aAdapter, aJCas);
            }
            else {
                throw new IllegalStateException("I don't know how to use ["
                        + aAdapter.getClass().getSimpleName() + "] in this situation.");
            }
        }
        else {
            if (aAdapter instanceof SpanAdapter) {
                createNewSpanAnnotation(aTarget, (SpanAdapter) aAdapter, aJCas);
            }
            else if (aAdapter instanceof ChainAdapter) {
                createNewChainElement(aTarget, (ChainAdapter) aAdapter, aJCas);
            }
            else {
                throw new IllegalStateException("I don't know how to use ["
                        + aAdapter.getClass().getSimpleName() + "] in this situation.");
            }
        }
    }
    
    private void createNewRelationAnnotation(AjaxRequestTarget aTarget, ArcAdapter aAdapter,
            JCas aJCas)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        LOG.trace(String.format("createNewRelationAnnotation()"));
        
        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();

        AnnotationFS originFs = selectByAddr(aJCas, selection.getOrigin());
        AnnotationFS targetFs = selectByAddr(aJCas, selection.getTarget());
        
        // Creating a relation
        AnnotationFS arc = aAdapter.add(originFs, targetFs, aJCas, state.getWindowBeginOffset(),
                state.getWindowEndOffset(), null, null);
        selection.selectArc(new VID(arc), originFs, targetFs);
    }
    
    private void createNewSpanAnnotation(AjaxRequestTarget aTarget, SpanAdapter aAdapter,
            JCas aJCas)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        LOG.trace(String.format("createNewSpanAnnotation()"));

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();
        List<FeatureState> featureStates = state.getFeatureStates();
        
        for (FeatureState featureState : featureStates) {
            Serializable spanValue = aAdapter.getSpan(aJCas, selection.getBegin(),
                    selection.getEnd(), featureState.feature, null);
            if (spanValue != null) {
                // allow modification for forward annotation
                if (state.isForwardAnnotation()) {
                    featureState.value = spanValue;
                    featureStates.get(0).value = spanValue;
                    annotationFeatureForm.selectedTag = annotationFeatureForm.getBindTags()
                            .entrySet().stream().filter(e -> e.getValue().equals(spanValue))
                            .map(Map.Entry::getKey).findFirst().orElse(null);
                }
                else {
                    actionClear(aTarget);
                    throw new AnnotationException("Cannot create another annotation of layer ["
                            + state.getSelectedAnnotationLayer().getUiName() + "] at this"
                            + " location - stacking is not enabled for this layer.");
                }
            }
        }
        int annoId = aAdapter.add(aJCas, selection.getBegin(), selection.getEnd(), null, null);
        AnnotationFS annoFs = WebAnnoCasUtil.selectByAddr(aJCas, annoId);
        selection.selectSpan(new VID(annoId), aJCas, annoFs.getBegin(), annoFs.getEnd());
    }
    
    private void createNewChainElement(AjaxRequestTarget aTarget, ChainAdapter aAdapter,
            JCas aJCas)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        LOG.trace(String.format("createNewChainElement()"));
        
        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();
        List<FeatureState> featureStates = state.getFeatureStates();
        
        for (FeatureState featureState : featureStates) {
            Serializable spanValue = aAdapter.getSpan(aJCas,
                    selection.getBegin(), selection.getEnd(), featureState.feature, null);
            if (spanValue != null) {
                // allow modification for forward annotation
                if (state.isForwardAnnotation()) {
                    featureState.value = spanValue;
                    featureStates.get(0).value = spanValue;
                    annotationFeatureForm.selectedTag = annotationFeatureForm.getBindTags()
                            .entrySet().stream().filter(e -> e.getValue().equals(spanValue))
                            .map(Map.Entry::getKey).findFirst().orElse(null);
                }
            }
        }
        selection.setAnnotation(new VID(
                aAdapter.addSpan(aJCas, selection.getBegin(), selection.getEnd(), null, null)));
        selection.setText(
                aJCas.getDocumentText().substring(selection.getBegin(), selection.getEnd()));
    }
    
    private void createNewChainLinkAnnotation(AjaxRequestTarget aTarget, ChainAdapter aAdapter,
            JCas aJCas)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        LOG.trace(String.format("createNewChainLinkAnnotation()"));
        
        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();
        
        AnnotationFS originFs = selectByAddr(aJCas, selection.getOrigin());
        AnnotationFS targetFs = selectByAddr(aJCas, selection.getTarget());
        
        // Creating a new chain link
        int addr = aAdapter.addArc(aJCas, originFs, targetFs, null, null);
        selection.selectArc(new VID(addr), originFs, targetFs);
    }
    
    @Override
    public void actionFillSlot(AjaxRequestTarget aTarget, JCas aJCas, int aBegin, int aEnd,
            VID aVID)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        assert aJCas != null;

        AnnotatorState state = getModelObject();

        // If this method is called when no slot is armed, it must be a bug!
        if (!state.isSlotArmed()) {
            throw new IllegalStateException("No slot is armed.");
        }
        
        // Fill slot with new annotation (only works if a concrete type is set for the link feature!
        int id;
        if (aVID.isNotSet()) {
            if (!CAS.TYPE_NAME_ANNOTATION.equals(state.getArmedFeature().getType())) {
                SpanAdapter adapter = (SpanAdapter) getAdapter(annotationService, annotationService
                        .getLayer(state.getArmedFeature().getType(), state.getProject()));

                id = adapter.add(aJCas, aBegin, aEnd, null, null);
            }
            else {
                throw new AnnotationException(
                        "Unable to create annotation of type [" + CAS.TYPE_NAME_ANNOTATION
                                + "]. Please click an annotation in stead of selecting new text.");
            } 
        }
        else {
            id = aVID.getId();
        }
        
        // Fill the annotation into the slow
        try {
            setSlot(aTarget, aJCas, id);
        }
        catch (Exception e) {
            handleException(this, aTarget, e);
        }
    }
    
    @Override
    public void actionSelect(AjaxRequestTarget aTarget, JCas aJCas)
        throws AnnotationException
    {
        // Edit existing annotation
        loadFeatureEditorModels(aJCas, aTarget);

        // Ensure we re-render and update the highlight
        onChange(aTarget);
    }

    @Override
    public void actionCreateOrUpdate(AjaxRequestTarget aTarget, JCas aJCas)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        actionCreateOrUpdate(aTarget, aJCas, false);
    }

    private void actionCreateOrUpdate(AjaxRequestTarget aTarget, JCas aJCas, boolean aIsForwarded)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        LOG.trace("actionAnnotate(isForwarded: {})", aIsForwarded);
    
        if (isAnnotationFinished()) {
            throw new AnnotationException("This document is already closed. Please ask your "
                    + "project manager to re-open it via the Monitoring page");
        }
        
        AnnotatorState state = getModelObject();
        state.getAction().setAnnotate(true);
        
        // Note that refresh changes the selected layer if a relation is created. Then the layer
        // switches from the selected span layer to the relation layer that is attached to the span
        if (state.getSelection().isArc()) {
            LOG.trace("actionAnnotate() relation annotation - looking for attached layer");
    
            // FIXME REC I think this whole section which meddles around with the selected annotation
            // layer should be moved out of there to the place where we originally set the annotation
            // layer...!
            AnnotationFS originFS = selectByAddr(aJCas, state.getSelection().getOrigin());
            AnnotationLayer spanLayer = TypeUtil.getLayer(annotationService, state.getProject(),
                    originFS);
            if (
                    state.getPreferences().isRememberLayer() && 
                    state.getAction().isAnnotate() && 
                    !spanLayer.equals(state.getDefaultAnnotationLayer())) 
            {
                throw new AnnotationException(
                        "No relation annotation allowed ["+ spanLayer.getUiName() +"]");
            }
    
            AnnotationLayer previousLayer = state.getSelectedAnnotationLayer();
            
            // If we are creating a relation annotation, we have to set the current layer depending
            // on the type of relation that is permitted between the source/target span. This is
            // necessary because we have no separate UI control to set the relation annotation type.
            // It is possible because currently only a single relation layer is allowed to attach to
            // any given span layer.
            
            // If we drag an arc between POS annotations, then the relation must be a dependency
            // relation.
            // FIXME - Actually this case should be covered by the last case - the database lookup!
            if (
                    spanLayer.isBuiltIn() && 
                    spanLayer.getName().equals(POS.class.getName())) 
            {
                AnnotationLayer depLayer = annotationService.getLayer(Dependency.class.getName(),
                        state.getProject());
                if (state.getAnnotationLayers().contains(depLayer)) {
                    state.setSelectedAnnotationLayer(depLayer);
                }
                else {
                    state.setSelectedAnnotationLayer(null);
                }
            }
            // If we drag an arc in a chain layer, then the arc is of the same layer as the span
            // Chain layers consist of arcs and spans
            else if (spanLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                // one layer both for the span and arc annotation
                state.setSelectedAnnotationLayer(spanLayer);
            }
            // Otherwise, look up the possible relation layer(s) in the database.
            else {
                for (AnnotationLayer l : annotationService.listAnnotationLayer(state
                        .getProject())) {
                    if (l.getAttachType() != null && l.getAttachType().equals(spanLayer)) {
                        if (state.getAnnotationLayers().contains(l)) {
                            state.setSelectedAnnotationLayer(l);
                        }
                        else {
                            state.setSelectedAnnotationLayer(null);
                        }
                        break;
                    }
                }
            }
    
            state.setDefaultAnnotationLayer(spanLayer);
         
            // If we switched layers, we need to initialize the feature editors for the new layer
            if (!ObjectUtils.equals(previousLayer, state.getSelectedAnnotationLayer())) {
                loadFeatureEditorModels(aJCas, aTarget);
            }
        }
    
        LOG.trace("actionAnnotate() selectedLayer: {}",
                state.getSelectedAnnotationLayer().getUiName());
        LOG.trace("actionAnnotate() defaultLayer: {}",
                state.getDefaultAnnotationLayer().getUiName());
        
        if (state.getSelectedAnnotationLayer() == null) {
            error("No layer is selected. First select a layer.");
            aTarget.addChildren(getPage(), FeedbackPanel.class);
            return;
        }
    
        if (state.getSelectedAnnotationLayer().isReadonly()) {
            error("Layer is not editable.");
            aTarget.addChildren(getPage(), FeedbackPanel.class);
            return;
        }
        
        // Verify if input is valid according to tagset
        LOG.trace("actionAnnotate() verifying feature values in editors");
        List<FeatureState> featureStates = getModelObject().getFeatureStates();
        for (int i = 0; i < featureStates.size(); i++) {
            AnnotationFeature feature = featureStates.get(i).feature;
            if (CAS.TYPE_NAME_STRING.equals(feature.getType())) {
                String value = (String) featureStates.get(i).value;
            	
                // Check if tag is necessary, set, and correct
                if (
                    value != null &&
                    feature.getTagset() != null && 
                    !feature.getTagset().isCreateTag() && 
                    !annotationService.existsTag(value, feature.getTagset())
                ) {
                    error("[" + value
                            + "] is not in the tag list. Please choose from the existing tags");
                    return;
                }
            }
        }
        
        // #186 - After filling a slot, the annotation detail panel is not updated 
        aTarget.add(annotationFeatureForm.featureEditorPanel);
    
        TypeAdapter adapter = getAdapter(annotationService, state.getSelectedAnnotationLayer());
        
        // If this is an annotation creation action, create the annotation
    	if (state.getSelection().getAnnotation().isNotSet()) {
    	    // Load the feature editors with the remembered values (if any)
    	    loadFeatureEditorModels(aJCas, aTarget);
    	    createNewAnnotation(aTarget, adapter, aJCas);
    	}
    
        // Update the features of the selected annotation from the values presently in the
    	// feature editors
    	writeFeatureEditorModelsToCas(adapter, aJCas);
    
        // Update progress information
        LOG.trace("actionAnnotate() updating progress information");
        int sentenceNumber = getSentenceNumber(aJCas, state.getSelection().getBegin());
        state.setFocusUnitIndex(sentenceNumber);
        state.getDocument().setSentenceAccessed(sentenceNumber);
    
        // persist changes
        writeEditorCas(aJCas);
    
        // Remember the current feature values independently for spans and relations
        LOG.trace("actionAnnotate() remembering feature editor values");
        state.rememberFeatures();
    
    	// Loading feature editor values from CAS
    	loadFeatureEditorModels(aJCas, aTarget);
    	
    	// onAnnotate callback
        LOG.trace("onAnnotate()");
    	onAnnotate(aTarget);
    
    	// Handle auto-forward if it is enabled
    	if (state.isForwardAnnotation() && !aIsForwarded && featureStates.get(0).value != null) {
    		if (state.getSelection().getEnd() >= state.getFirstVisibleUnitEnd()) {
    			autoScroll(aJCas, true);
    		}
    
            LOG.info("BEGIN auto-forward annotation");
    
            AnnotationFS nextToken = WebAnnoCasUtil.getNextToken(aJCas, state.getSelection().getBegin(),
                    state.getSelection().getEnd());
            if (nextToken != null) {
                if (getModelObject().getWindowEndOffset() > nextToken.getBegin()) {
                    state.getSelection().selectSpan(aJCas, nextToken.getBegin(), nextToken.getEnd());
                    actionCreateOrUpdate(aTarget, aJCas, true);
                }
            }
    
            LOG.trace("onAutoForward()");
    		onAutoForward(aTarget);
    		
            LOG.info("END auto-forward annotation");
    	} 
    	// Perform auto-scroll if it is enabled
    	else if (state.getPreferences().isScrollPage()) {
    		autoScroll(aJCas, false);
    	}
    	
    	annotationFeatureForm.forwardAnnotationText.setModelObject(null);
    	
        LOG.trace("onChange()");
    	onChange(aTarget);
    	
    	if (state.isForwardAnnotation() && state.getFeatureStates().get(0).value != null) {
    		aTarget.add(annotationFeatureForm);
    	}
    	
        // If we created a new annotation, then refresh the available annotation layers in the
        // detail panel.
        if (state.getSelection().getAnnotation().isNotSet()) {
            // This already happens in loadFeatureEditorModels() above - probably not needed
            // here again
            // annotationFeatureForm.updateLayersDropdown();
            
            LOG.trace("actionAnnotate() setting selected layer (not sure why)");
            if (annotationFeatureForm.annotationLayers.isEmpty()) {
                state.setSelectedAnnotationLayer(new AnnotationLayer());
            }
            else if (state.getSelectedAnnotationLayer() == null) {
                if (state.getRememberedSpanLayer() == null) {
                    state.setSelectedAnnotationLayer(annotationFeatureForm.annotationLayers.get(0));
                }
                else {
                    state.setSelectedAnnotationLayer(state.getRememberedSpanLayer());
                }
            }
            LOG.trace("actionAnnotate() selectedLayer: {}",
                    state.getSelectedAnnotationLayer().getUiName());
            
            // Actually not sure why we would want to clear these here - in fact, they should
            // still be around for the rendering phase of the feature editors...
            //clearFeatureEditorModels(aTarget);
            
            // This already happens in loadFeatureEditorModels() above - probably not needed
            // here again
            // annotationFeatureForm.updateRememberLayer();
        }
    }

    @Override
    public void actionDelete(AjaxRequestTarget aTarget)
        throws IOException, UIMAException, ClassNotFoundException, CASRuntimeException,
        AnnotationException
    {
        JCas jCas = getEditorCas();
        
        AnnotatorState state = getModelObject();
        
        AnnotationFS fs = selectByAddr(jCas, state.getSelection().getAnnotation().getId());

        // TODO We assume here that the selected annotation layer corresponds to the type of the
        // FS to be deleted. It would be more robust if we could get the layer from the FS itself.
        AnnotationLayer layer = state.getSelectedAnnotationLayer();
        TypeAdapter adapter = getAdapter(annotationService, layer);

        // == DELETE ATTACHED RELATIONS ==
        // If the deleted FS is a span, we must delete all relations that
        // point to it directly or indirectly via the attachFeature.
        //
        // NOTE: It is important that this happens before UNATTACH SPANS since the attach feature
        // is no longer set after UNATTACH SPANS!
        if (adapter instanceof SpanAdapter) {
            for (AnnotationFS attachedFs : getAttachedRels(jCas, fs, layer)) {
                jCas.getCas().removeFsFromIndexes(attachedFs);
                info("The attached annotation for relation type [" + annotationService
                        .getLayer(attachedFs.getType().getName(), state.getProject()).getUiName()
                        + "] is deleted");
            }
        }

        // == DELETE ATTACHED SPANS ==
        // This case is currently not implemented because WebAnno currently does not allow to
        // create spans that attach to other spans. The only span type for which this is relevant
        // is the Token type which cannot be deleted.

        // == UNATTACH SPANS ==
        // If the deleted FS is a span that is attached to another span, the
        // attachFeature in the other span must be set to null. Typical example: POS is deleted, so
        // the pos feature of Token must be set to null. This is a quick case, because we only need
        // to look at span annotations that have the same offsets as the FS to be deleted.
        if (adapter instanceof SpanAdapter && layer.getAttachType() != null) {
            Type spanType = CasUtil.getType(jCas.getCas(), layer.getAttachType().getName());
            Feature attachFeature = spanType.getFeatureByBaseName(layer.getAttachFeature()
                    .getName());

            for (AnnotationFS attachedFs : selectAt(jCas.getCas(), spanType, fs.getBegin(),
                    fs.getEnd())) {
                if (isSame(attachedFs.getFeatureValue(attachFeature), fs)) {
                    attachedFs.setFeatureValue(attachFeature, null);
                    LOG.debug("Unattached [" + attachFeature.getShortName() + "] on annotation ["
                            + getAddr(attachedFs) + "]");
                }
            }
        }

        // == CLEAN UP LINK FEATURES ==
        // If the deleted FS is a span that is the target of a link feature, we must unset that
        // link and delete the slot if it is a multi-valued link. Here, we have to scan all
        // annotations from layers that have link features that could point to the FS
        // to be deleted: the link feature must be the type of the FS or it must be generic.
        if (adapter instanceof SpanAdapter) {
            for (AnnotationFeature linkFeature : annotationService.listAttachedLinkFeatures(layer)) {
                Type linkType = CasUtil.getType(jCas.getCas(), linkFeature.getLayer().getName());

                for (AnnotationFS linkFS : CasUtil.select(jCas.getCas(), linkType)) {
                    List<LinkWithRoleModel> links = getFeature(linkFS, linkFeature);
                    Iterator<LinkWithRoleModel> i = links.iterator();
                    boolean modified = false;
                    while (i.hasNext()) {
                        LinkWithRoleModel link = i.next();
                        if (link.targetAddr == getAddr(fs)) {
                            i.remove();
                            LOG.debug("Cleared slot [" + link.role + "] in feature ["
                                    + linkFeature.getName() + "] on annotation [" + getAddr(linkFS)
                                    + "]");
                            modified = true;
                        }
                    }
                    if (modified) {
                        setFeature(linkFS, linkFeature, links);
                    }
                }
            }
        }

        // If the deleted FS is a relation, we don't have to do anything. Nothing can point to a
        // relation.
        if (adapter instanceof ArcAdapter) {
            // Do nothing ;)
        }

        // Actually delete annotation
        adapter.delete(jCas, state.getSelection().getAnnotation());

        // Store CAS again
        writeEditorCas(jCas);

        // Update progress information
        int sentenceNumber = getSentenceNumber(jCas, state.getSelection().getBegin());
        state.setFocusUnitIndex(sentenceNumber);
        state.getDocument().setSentenceAccessed(sentenceNumber);

        // Auto-scroll
        if (state.getPreferences().isScrollPage()) {
            autoScroll(jCas, false);
        }

        state.rememberFeatures();
        state.getAction().setAnnotate(false);

        info(generateMessage(state.getSelectedAnnotationLayer(), null, true));

        state.getSelection().clear();

        // after delete will follow annotation
        state.getAction().setAnnotate(true);
        aTarget.add(annotationFeatureForm);

        onChange(aTarget);
        onDelete(aTarget, fs);
    }

    @Override
    public void actionReverse(AjaxRequestTarget aTarget)
        throws IOException, UIMAException, ClassNotFoundException, AnnotationException
    {
        JCas jCas = getEditorCas();

        AnnotatorState state = getModelObject();
        
        AnnotationFS idFs = selectByAddr(jCas, state.getSelection().getAnnotation().getId());

        jCas.removeFsFromIndexes(idFs);

        AnnotationFS originFs = selectByAddr(jCas, state.getSelection().getOrigin());
        AnnotationFS targetFs = selectByAddr(jCas, state.getSelection().getTarget());

        List<FeatureState> featureStates = getModelObject().getFeatureStates();
        
        TypeAdapter adapter = getAdapter(annotationService, state.getSelectedAnnotationLayer());
        if (adapter instanceof ArcAdapter) {
            if (featureStates.isEmpty()) {
                // If no features, still create arc #256
                AnnotationFS arc = ((ArcAdapter) adapter).add(targetFs, originFs, jCas,
                        state.getWindowBeginOffset(), state.getWindowEndOffset(), null, null);
                state.getSelection().setAnnotation(new VID(getAddr(arc)));
            }
            else {
                for (FeatureState featureState : featureStates) {
                    AnnotationFS arc = ((ArcAdapter) adapter).add(targetFs, originFs, jCas,
                            state.getWindowBeginOffset(), state.getWindowEndOffset(),
                            featureState.feature, featureState.value);
                    state.getSelection().setAnnotation(new VID(getAddr(arc)));
                }
            }
        }
        else {
            error("chains cannot be reversed");
            return;
        }

        // persist changes
        writeEditorCas(jCas);
        int sentenceNumber = getSentenceNumber(jCas, originFs.getBegin());
        state.setFocusUnitIndex(sentenceNumber);
        state.getDocument().setSentenceAccessed(sentenceNumber);

        if (state.getPreferences().isScrollPage()) {
            autoScroll(jCas, false);
        }

        info("The arc has been reversed");
        state.rememberFeatures();

        // in case the user re-reverse it
        state.getSelection().reverseArc();

        onChange(aTarget);
    }

    @Override
    public void actionClear(AjaxRequestTarget aTarget)
        throws IOException, UIMAException, ClassNotFoundException, AnnotationException
    {
        reset(aTarget);
        aTarget.add(annotationFeatureForm);
        onChange(aTarget);
    }
    
    public JCas getEditorCas()
        throws UIMAException, IOException, ClassNotFoundException
    {
        AnnotatorState state = getModelObject();

        if (state.getMode().equals(Mode.ANNOTATION) || state.getMode().equals(Mode.AUTOMATION)
                || state.getMode().equals(Mode.CORRECTION)) {

            return documentService.readAnnotationCas(state.getDocument(), state.getUser());
        }
        else {
            return curationDocumentService.readCurationCas(state.getDocument());
        }
    }
    
    public void writeEditorCas(JCas aJCas)
        throws IOException
    {
        AnnotatorState state = getModelObject();
        if (state.getMode().equals(Mode.ANNOTATION) || state.getMode().equals(Mode.AUTOMATION)
                || state.getMode().equals(Mode.CORRECTION)) {
            documentService.writeAnnotationCas(aJCas, state.getDocument(), state.getUser(), true);
        }
        else if (state.getMode().equals(Mode.CURATION)) {
            curationDocumentService.writeCurationCas(aJCas, state.getDocument(), true);
        }
    }

    /**
     * Scroll the window of visible annotations.
     * @param aForward
     *            instead of centering on the sentence that had the last editor, just scroll down
     *            one sentence. This is for forward-annotation mode.
     */
    private void autoScroll(JCas jCas, boolean aForward)
    {
        AnnotatorState state = getModelObject();
        
        if (aForward) {
            // Fetch the first sentence on screen
            Sentence sentence = selectByAddr(jCas, Sentence.class,
                    state.getFirstVisibleUnitAddress());
            // Find the following one
            int address = getNextSentenceAddress(jCas, sentence);
            // Move to it
            state.setFirstVisibleUnit(selectByAddr(jCas, Sentence.class, address));
        }
        else {
            // Fetch the first sentence on screen
            Sentence sentence = selectByAddr(jCas, Sentence.class,
                    state.getFirstVisibleUnitAddress());
            // Calculate the first sentence in the window in such a way that the annotation
            // currently selected is in the center of the window
            sentence = findWindowStartCenteringOnSelection(jCas, sentence,
                    state.getSelection().getBegin(), state.getProject(), state.getDocument(),
                    state.getPreferences().getWindowSize());
            // Move to it
            state.setFirstVisibleUnit(sentence);
        }
    }

    @SuppressWarnings("unchecked")
    public void setSlot(AjaxRequestTarget aTarget, JCas aJCas, int aAnnotationId)
    {
        AnnotatorState state = getModelObject();
        
        // Set an armed slot
        if (!state.getSelection().isArc() && state.isSlotArmed()) {
            List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) state.getFeatureState(state
                    .getArmedFeature()).value;
            LinkWithRoleModel link = links.get(state.getArmedSlot());
            link.targetAddr = aAnnotationId;
            link.label = selectByAddr(aJCas, aAnnotationId).getCoveredText();
        }

        // Auto-commit if working on existing annotation
        if (state.getSelection().getAnnotation().isSet()) {
            try {
                actionCreateOrUpdate(aTarget, aJCas, false);
            }
            catch (Exception e) {
                handleException(this, aTarget, e);
            }
        }
        
        state.clearArmedSlot();
    }
    
    public void loadFeatureEditorModels(AjaxRequestTarget aTarget) 
        throws AnnotationException
    {
        try {
            JCas annotationCas = getEditorCas();
            loadFeatureEditorModels(annotationCas, aTarget);
        }
        catch (AnnotationException e) {
            throw e;
        }
        catch (Exception e) {
            throw new AnnotationException(e);
        }
    }
    
    public void loadFeatureEditorModels(JCas aJCas, AjaxRequestTarget aTarget)
        throws AnnotationException
    {
        LOG.trace(String.format("loadFeatureEditorModels()"));
        
        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();

        List<FeatureState> featureStates = state.getFeatureStates();
        for (FeatureState featureState : featureStates) {
            if (StringUtils.isNotBlank(featureState.feature.getLinkTypeName())) {
                featureState.value = new ArrayList<>();
            }
        }
        
        try {
            if (selection.isSpan()) {
                annotationFeatureForm.updateLayersDropdown();
            }
            
            if (selection.getAnnotation().isSet()) {
                // If an existing annotation was selected, take the feature editor model values from
                // there
                AnnotationFS annoFs = selectByAddr(aJCas, state.getSelection().getAnnotation()
                        .getId());
                
                // Try obtaining the layer from the feature structure
                AnnotationLayer layer;
                try {
                    layer = TypeUtil.getLayer(annotationService, state.getProject(), annoFs);
                    state.setSelectedAnnotationLayer(layer);
                    LOG.trace(String.format(
                            "loadFeatureEditorModels() selectedLayer set from selection: %s",
                            state.getSelectedAnnotationLayer().getUiName()));
                }
                catch (NoResultException e) {
                    clearFeatureEditorModels(aTarget);
                    throw new IllegalStateException(
                            "Unknown layer [" + annoFs.getType().getName() + "]", e);
                }

                // If remember layer is off, then the current layer follows the selected annotations
                // This is only relevant for span annotations because we only have these in the 
                // dropdown - relation annotations are automatically determined based on the 
                // selected span annotation
                if (!selection.isArc() && !state.getPreferences().isRememberLayer()) {
                    state.setSelectedAnnotationLayer(layer);
                }
                
                loadFeatureEditorModelsCommon(aTarget, aJCas, layer, annoFs, null);
            }
            else {
                // If a new annotation is being created, populate the feature editors from the
                // remembered values (if any)
                
                if (selection.isArc()) {
                    // Avoid creation of arcs on locked layers
                    if (state.getSelectedAnnotationLayer() != null
                            && state.getSelectedAnnotationLayer().isReadonly()) {
                        state.setSelectedAnnotationLayer(new AnnotationLayer());
                    }
                    else {
                        loadFeatureEditorModelsCommon(aTarget, aJCas,
                                state.getSelectedAnnotationLayer(), null,
                                state.getRememberedArcFeatures());
                    }
                }
                else { 
                    loadFeatureEditorModelsCommon(aTarget, aJCas,
                            state.getSelectedAnnotationLayer(), null,
                            state.getRememberedSpanFeatures());
                }
            }

            annotationFeatureForm.updateRememberLayer();
            
            if (aTarget != null) {
                aTarget.add(annotationFeatureForm);
            }
        }
        catch (Exception e) {
            throw new AnnotationException(e);
        }
    }

    private void loadFeatureEditorModelsCommon(AjaxRequestTarget aTarget, JCas aJCas,
            AnnotationLayer aLayer, FeatureStructure aFS,
            Map<AnnotationFeature, Serializable> aRemembered)
    {
        clearFeatureEditorModels(aTarget);

        AnnotatorState state = AnnotationDetailEditorPanel.this.getModelObject();

        // Populate from feature structure
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(aLayer)) {
            if (!feature.isEnabled()) {
                continue;
            }
            
            Serializable value = null;
            if (aFS != null) {
                value = (Serializable) WebAnnoCasUtil.getFeature(aFS, feature);
            }
            else if (aRemembered != null) {
                value = aRemembered.get(feature);
            }
            
            FeatureState featureState = null;
            if (WebAnnoConst.CHAIN_TYPE.equals(feature.getLayer().getType())) {
                if (state.getSelection().isArc()) {
                    if (feature.getLayer().isLinkedListBehavior()
                            && WebAnnoConst.COREFERENCE_RELATION_FEATURE.equals(feature
                                    .getName())) {
                        featureState = new FeatureState(feature, value);
                    }
                }
                else {
                    if (WebAnnoConst.COREFERENCE_TYPE_FEATURE.equals(feature.getName())) {
                        featureState = new FeatureState(feature, value);
                    }
                }

            }
            else {
                featureState = new FeatureState(feature, value);
            }
            
            if (featureState != null) {
                state.getFeatureStates().add(featureState);
                
                // verification to check whether constraints exist for this project or NOT
                if (state.getConstraints() != null && state.getSelection().getAnnotation().isSet()) {
                    // indicator.setRulesExist(true);
                    populateTagsBasedOnRules(aJCas, featureState);
                }
                else {
                    // indicator.setRulesExist(false);
                    featureState.tagset = annotationService.listTags(featureState.feature.getTagset());
                }
            }
        }
    }

    private void writeFeatureEditorModelsToCas(TypeAdapter aAdapter, JCas aJCas)
        throws IOException
    {
        AnnotatorState state = getModelObject();
        List<FeatureState> featureStates = state.getFeatureStates();

        LOG.trace(String.format("writeFeatureEditorModelsToCas()"));
        List<AnnotationFeature> features = new ArrayList<AnnotationFeature>();
        for (FeatureState featureState : featureStates) {
            features.add(featureState.feature);

            // For string features with extensible tagsets, extend the tagset
            if (CAS.TYPE_NAME_STRING.equals(featureState.feature.getType())) {
                String value = (String) featureState.value;

                if (value != null && featureState.feature.getTagset() != null
                        && featureState.feature.getTagset().isCreateTag()
                        && !annotationService.existsTag(value, featureState.feature.getTagset())) {
                    Tag selectedTag = new Tag();
                    selectedTag.setName(value);
                    selectedTag.setTagSet(featureState.feature.getTagset());
                    annotationService.createTag(selectedTag);
                }
            }
            LOG.trace(String.format("writeFeatureEditorModelsToCas() "
                    + featureState.feature.getUiName() + " = " + featureState.value));
            aAdapter.updateFeature(aJCas, featureState.feature,
                    state.getSelection().getAnnotation().getId(), featureState.value);
        }
        
        // Generate info message
        if (state.getSelection().getAnnotation().isSet()) {
            String bratLabelText = TypeUtil.getUiLabelText(aAdapter,
                    selectByAddr(aJCas, state.getSelection().getAnnotation().getId()), features);
            info(generateMessage(state.getSelectedAnnotationLayer(), bratLabelText, false));
        }
    }
        
    
    protected void onChange(AjaxRequestTarget aTarget)
    {
        // Overriden in CurationPanel
    }

    protected void onAutoForward(AjaxRequestTarget aTarget)
    {
        // Overriden in CurationPanel
    }

    protected void onAnnotate(AjaxRequestTarget aTarget)
    {
        // Overriden in AutomationPage
    }

    protected void onDelete(AjaxRequestTarget aTarget, AnnotationFS aFs)
    {
        // Overriden in AutomationPage
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }
    
    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    /**
     * Clear the values from the feature editors.
     */
    private void clearFeatureEditorModels(AjaxRequestTarget aTarget)
    {
        LOG.trace(String.format("clearFeatureEditorModels()"));
        getModelObject().getFeatureStates().clear();
        if (aTarget != null) {
            aTarget.add(annotationFeatureForm);
        }
    }
    
    /**
     * Adds and sorts tags based on Constraints rules
     */
    private void populateTagsBasedOnRules(JCas aJCas, FeatureState aModel)
    {
        LOG.trace(String
                .format("populateTagsBasedOnRules(feature: " + aModel.feature.getUiName() + ")"));
        
        AnnotatorState state = getModelObject();
        
        // Add values from rules
        String restrictionFeaturePath;
        switch (aModel.feature.getLinkMode()) {
        case WITH_ROLE:
            restrictionFeaturePath = aModel.feature.getName() + "."
                    + aModel.feature.getLinkTypeRoleFeatureName();
            break;
        case NONE:
            restrictionFeaturePath = aModel.feature.getName();
            break;
        default:
            throw new IllegalArgumentException("Unsupported link mode ["
                    + aModel.feature.getLinkMode() + "] on feature ["
                    + aModel.feature.getName() + "]");
        }
        
        aModel.indicator.reset();
        
        // Fetch possible values from the constraint rules
        List<PossibleValue> possibleValues;
        try {
            FeatureStructure featureStructure = selectByAddr(aJCas, state.getSelection()
                    .getAnnotation().getId());

            Evaluator evaluator = new ValuesGenerator();
            //Only show indicator if this feature can be affected by Constraint rules!
            aModel.indicator.setAffected(evaluator.isThisAffectedByConstraintRules(
                    featureStructure, restrictionFeaturePath, state.getConstraints()));
            
            possibleValues = evaluator.generatePossibleValues(
                    featureStructure, restrictionFeaturePath, state.getConstraints());

            LOG.debug("Possible values for [" + featureStructure.getType().getName() + "] ["
                    + restrictionFeaturePath + "]: " + possibleValues);
        }
        catch (Exception e) {
            error("Unable to evaluate constraints: " + ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Unable to evaluate constraints: " + e.getMessage(), e);
            possibleValues = new ArrayList<>();
        }

        // Fetch actual tagset
        List<Tag> valuesFromTagset = annotationService.listTags(aModel.feature.getTagset());
        
        // First add tags which are suggested by rules and exist in tagset
        List<Tag> tagset = compareSortAndAdd(possibleValues, valuesFromTagset, aModel.indicator);

        // Then add the remaining tags
        for (Tag remainingTag : valuesFromTagset) {
            if (!tagset.contains(remainingTag)) {
                tagset.add(remainingTag);
            }
        }
        
        // Record the possible values and the (re-ordered) tagset in the feature state
        aModel.possibleValues = possibleValues;
        aModel.tagset = tagset;
    }
    
    /*
     * Compares existing tagset with possible values resulted from rule evaluation Adds only which
     * exist in tagset and is suggested by rules. The remaining values from tagset are added
     * afterwards.
     */
    private static List<Tag> compareSortAndAdd(List<PossibleValue> possibleValues,
            List<Tag> valuesFromTagset, RulesIndicator rulesIndicator)
    {
        //if no possible values, means didn't satisfy conditions
        if(possibleValues.isEmpty())
        {
            rulesIndicator.didntMatchAnyRule();
        }
        List<Tag> returnList = new ArrayList<Tag>();
        // Sorting based on important flag
        // possibleValues.sort(null);
        // Comparing to check which values suggested by rules exists in existing
        // tagset and adding them first in list.
        for (PossibleValue value : possibleValues) {
            for (Tag tag : valuesFromTagset) {
                if (value.getValue().equalsIgnoreCase(tag.getName())) {
                    //Matching values found in tagset and shown in dropdown
                    rulesIndicator.rulesApplied();
                    // HACK BEGIN
                    tag.setReordered(true);
                    // HACK END
                    //Avoid duplicate entries
                    if(!returnList.contains(tag)){ 
                        returnList.add(tag); 
                    }
                }
            }
        }
        //If no matching tags found
        if(returnList.isEmpty()){
            rulesIndicator.didntMatchAnyTag();
        }
        return returnList;
    }

    public void reset(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        state.getSelection().clear();
        clearFeatureEditorModels(aTarget);
    }
    
    private Set<AnnotationFS> getAttachedRels(JCas aJCas, AnnotationFS aFs, AnnotationLayer aLayer)
        throws UIMAException, ClassNotFoundException, IOException
    {
        Set<AnnotationFS> toBeDeleted = new HashSet<AnnotationFS>();
        for (AnnotationLayer relationLayer : annotationService
                .listAttachedRelationLayers(aLayer)) {
            ArcAdapter relationAdapter = (ArcAdapter) getAdapter(annotationService,
                    relationLayer);
            Type relationType = CasUtil.getType(aJCas.getCas(), relationLayer.getName());
            Feature sourceFeature = relationType.getFeatureByBaseName(relationAdapter
                    .getSourceFeatureName());
            Feature targetFeature = relationType.getFeatureByBaseName(relationAdapter
                    .getTargetFeatureName());

            // This code is already prepared for the day that relations can go between
            // different layers and may have different attach features for the source and
            // target layers.
            Feature relationSourceAttachFeature = null;
            Feature relationTargetAttachFeature = null;
            if (relationAdapter.getAttachFeatureName() != null) {
                relationSourceAttachFeature = sourceFeature.getRange().getFeatureByBaseName(
                        relationAdapter.getAttachFeatureName());
                relationTargetAttachFeature = targetFeature.getRange().getFeatureByBaseName(
                        relationAdapter.getAttachFeatureName());
            }
            
            for (AnnotationFS relationFS : CasUtil.select(aJCas.getCas(), relationType)) {
                // Here we get the annotations that the relation is pointing to in the UI
                FeatureStructure sourceFS;
                if (relationSourceAttachFeature != null) {
                    sourceFS = relationFS.getFeatureValue(sourceFeature).getFeatureValue(
                            relationSourceAttachFeature);
                }
                else {
                    sourceFS = relationFS.getFeatureValue(sourceFeature);
                }

                FeatureStructure targetFS;
                if (relationTargetAttachFeature != null) {
                    targetFS = relationFS.getFeatureValue(targetFeature).getFeatureValue(
                            relationTargetAttachFeature);
                }
                else {
                    targetFS = relationFS.getFeatureValue(targetFeature);
                }

                if (isSame(sourceFS, aFs) || isSame(targetFS, aFs)) {
                    toBeDeleted.add(relationFS);
                    LOG.debug("Deleted relation [" + getAddr(relationFS) + "] from layer ["
                            + relationLayer.getName() + "]");
                }
            }
        }
        
        return toBeDeleted;
    }
    
    public AnnotationFeatureForm getAnnotationFeatureForm()
    {
        return annotationFeatureForm;
    }

    public Label getSelectedAnnotationLayer()
    {
        return annotationFeatureForm.selectedAnnotationLayer;
    }

    private boolean isForwardable()
    {
        AnnotatorState state = AnnotationDetailEditorPanel.this.getModelObject();
        AnnotationLayer selectedLayer = state.getSelectedAnnotationLayer();
        
        if (selectedLayer == null) {
            return false;
        }
        
		if (selectedLayer.getId() <= 0) {
            return false;
        }

		if (!selectedLayer.getType().equals(WebAnnoConst.SPAN_TYPE)) {
            return false;
        }
		
		if (!selectedLayer.isLockToTokenOffset()) {
			return false;
		}
		
		// no forward annotation for multifeature layers.
        if (annotationService.listAnnotationFeature(selectedLayer).size() > 1) {
            return false;
        }
		
        // if there are no features at all, no forward annotation
        if (annotationService.listAnnotationFeature(selectedLayer).isEmpty()) {
            return false;
        }
        
        // we allow forward annotation only for a feature with a tagset
        if (annotationService.listAnnotationFeature(selectedLayer).get(0).getTagset() == null) {
            return false;
        }
		
		// there should be at least one tag in the tagset
        TagSet tagSet = annotationService.listAnnotationFeature(selectedLayer).get(0).getTagset();
        if (annotationService.listTags(tagSet).size() == 0) {
            return false;
        }
		
		return true;
	}
    
    public static void handleException(Component aComponent, AjaxRequestTarget aTarget,
            Exception aException)
    {
        try {
            throw aException;
        }
        catch (AnnotationException e) {
            if (aTarget != null) {
                aTarget.prependJavaScript("alert('Error: " + e.getMessage() + "')");
            }
            else {
                aComponent.error("Error: " + e.getMessage());
            }
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
    
    private static String generateMessage(AnnotationLayer aLayer, String aLabel, boolean aDeleted)
    {
        String action = aDeleted ? "deleted" : "created/updated";

        String msg = "The [" + aLayer.getUiName() + "] annotation has been " + action + ".";
        if (StringUtils.isNotBlank(aLabel)) {
            msg += " Label: [" + aLabel + "]";
        }
        return msg;
    }
}
