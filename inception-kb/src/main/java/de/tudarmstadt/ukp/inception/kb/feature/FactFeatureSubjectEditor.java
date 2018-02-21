package de.tudarmstadt.ukp.inception.kb.feature;

import com.googlecode.wicket.kendo.ui.form.TextField;
import com.googlecode.wicket.kendo.ui.form.dropdown.DropDownList;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.LinkFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class FactFeatureSubjectEditor extends FeatureEditor {

    private static final long serialVersionUID = 4230722501745589589L;
    private @SpringBean AnnotationSchemaService annotationService;
    private WebMarkupContainer content;

    @SuppressWarnings("rawtypes")
    private AbstractTextComponent field;
    private boolean hideUnconstraintFeature;

    private AnnotationActionHandler actionHandler;
    private IModel<AnnotatorState> stateModel;

    // Wicket component is bound to this property
    @SuppressWarnings("unused")
    private String newRole = "subj";

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

        List<LinkWithRoleModel> links = (List<LinkWithRoleModel>) FactFeatureSubjectEditor.this
            .getModelObject().value;
        AnnotatorState state = FactFeatureSubjectEditor.this.stateModel.getObject();

        LinkWithRoleModel m = new LinkWithRoleModel();
        m.role = newRole;
        links.add(m);
        state.setArmedSlot(FactFeatureSubjectEditor.this.getModelObject().feature, links.size() - 1);


        content.add(new RefreshingView<LinkWithRoleModel>("slots",
            PropertyModel.of(getModel(), "value"))
        {
            private static final long serialVersionUID = 5475284956525780698L;

            @Override
            protected Iterator<IModel<LinkWithRoleModel>> getItemModels()
            {
                return new ModelIteratorAdapter<LinkWithRoleModel>(
                    (List<LinkWithRoleModel>) FactFeatureSubjectEditor.this.getModelObject().value)
                {
                    @Override
                    protected IModel<LinkWithRoleModel> model(LinkWithRoleModel aObject)
                    {
                        return Model.of(aObject);
                    }
                };
            }

            @Override
            protected void populateItem(final Item<LinkWithRoleModel> aItem)
            {
                AnnotatorState state = stateModel.getObject();

                aItem.setModel(new CompoundPropertyModel<>(aItem.getModelObject()));
                Label role = new Label("role");
                aItem.add(role);

                final Label label;
                if (aItem.getModelObject().targetAddr == -1
                    && state.isArmedSlot(getModelObject().feature, aItem.getIndex())) {
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
                        actionToggleArmedState(aTarget, aItem);
                    }
                });
                label.add(new AttributeAppender("style", new Model<String>()
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject()
                    {
                        if (state.isArmedSlot(getModelObject().feature, aItem.getIndex())) {
                            return "; background: orange";
                        }
                        else {
                            return "";
                        }
                    }
                }));
                aItem.add(label);
            }
        });


    }

    private void actionToggleArmedState(AjaxRequestTarget aTarget, Item<LinkWithRoleModel> aItem)
    {
        AnnotatorState state = FactFeatureSubjectEditor.this.stateModel.getObject();

        if (state.isArmedSlot(getModelObject().feature, aItem.getIndex())) {
            state.clearArmedSlot();
            aTarget.add(content);
        }
        else {
            state.setArmedSlot(getModelObject().feature, aItem.getIndex());
            // Need to re-render the whole form because a slot in another
            // link editor might get unarmed
            aTarget.add(getOwner());
        }
    }

    @Override
    public Component getFocusComponent() {
        return field;
    }
}
