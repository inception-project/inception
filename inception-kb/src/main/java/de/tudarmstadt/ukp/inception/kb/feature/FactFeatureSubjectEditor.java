package de.tudarmstadt.ukp.inception.kb.feature;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.kendo.ui.form.dropdown.DropDownList;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArgLink;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

/**
 *
 */
public class FactFeatureSubjectEditor extends FeatureEditor {

    private static final long serialVersionUID = 4230722501745589589L;
    private @SpringBean AnnotationSchemaService annotationService;
    private WebMarkupContainer content;

    @SuppressWarnings("rawtypes")
    private Component focusComponent;
    private boolean hideUnconstraintFeature;

    private AnnotationActionHandler actionHandler;
    private IModel<AnnotatorState> stateModel;

    @SuppressWarnings("unused")
    private String newRole = "subj";
    private LinkWithRoleModel subjectModel;

    private @SpringBean KnowledgeBaseService kbService;

    public FactFeatureSubjectEditor(String aId, MarkupContainer aOwner, AnnotationActionHandler aHandler,
                                    final IModel<AnnotatorState> aStateModel, final IModel<FeatureState> aFeatureStateModel) {
        super(aId, aOwner, CompoundPropertyModel.of(aFeatureStateModel));

        stateModel = aStateModel;
        actionHandler = aHandler;

        hideUnconstraintFeature = getModelObject().feature.isHideUnconstraintFeature();

        add(new Label("feature", getModelObject().feature.getUiName()));
        content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        add(content);

        FactFeatureSubjectEditor.this.getModelObject().feature.setLinkMode(LinkMode.WITH_ROLE);
        FactFeatureSubjectEditor.this.getModelObject().feature.setLinkTypeName(SemArgLink.class.getName());
        FactFeatureSubjectEditor.this.getModelObject().feature.setLinkTypeRoleFeatureName("role");
        FactFeatureSubjectEditor.this.getModelObject().feature.setLinkTypeTargetFeatureName("target");

        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) FactFeatureSubjectEditor.this
            .getModelObject().value;

        AnnotatorState state = FactFeatureSubjectEditor.this.stateModel.getObject();

        subjectModel = new LinkWithRoleModel();
        subjectModel.role = newRole;
        links.add(subjectModel);
        state.setArmedSlot(FactFeatureSubjectEditor.this.getModelObject().feature, links.size() - 1);

        content.add(new Label("role", subjectModel.role));
        content.add(createSubjectLabel());
        content.add(focusComponent = createFieldComboBox());
    }

    private Label createSubjectLabel() {
        AnnotatorState state = stateModel.getObject();
        final Label label;
        if (subjectModel.targetAddr == -1
            && state.isArmedSlot(getModelObject().feature, 0)) {
            label = new Label("label", "<Select to fill>");
        }
        else {
            label = new Label("label");
        }
        label.add(new AjaxEventBehavior("click")
        {
            private static final long serialVersionUID = 7633309278417475424L;

            @Override
            protected void onEvent(AjaxRequestTarget aTarget)
            {
                actionToggleArmedState(aTarget);
            }
        });
        label.add(new AttributeAppender("style", new Model<String>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject()
            {
                if (state.isArmedSlot(getModelObject().feature, 0)) {
                    return "; background: orange";
                }
                else {
                    return "";
                }
            }
        }));
        return  label;
    }

    private DropDownList<KBHandle> createFieldComboBox()
    {
        DropDownList<KBHandle> field = new DropDownList<>("value", LambdaModel.of(() -> {
            AnnotationFeature feat = getModelObject().feature;
            List<KBHandle> handles = new LinkedList<>();
            for (KnowledgeBase kb : kbService.getKnowledgeBases(feat.getProject())) {
                handles.addAll(kbService.listConcepts(kb, true));
            }
            return new ArrayList<>(handles);
        }), new ChoiceRenderer<>("uiLabel"));
        field.setOutputMarkupId(true);
        field.setMarkupId(ID_PREFIX + getModelObject().feature.getId());
        return field;
    }


    private void actionToggleArmedState(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = FactFeatureSubjectEditor.this.stateModel.getObject();

        if (state.isArmedSlot(getModelObject().feature, 0)) {
            state.clearArmedSlot();
            aTarget.add(content);
        }
        else {
            state.setArmedSlot(getModelObject().feature, 0);
            // Need to re-render the whole form because a slot in another
            // link editor might get unarmed
            aTarget.add(getOwner());
        }
    }

    @Override
    public Component getFocusComponent() {
        return focusComponent;
    }
}
