package de.tudarmstadt.ukp.inception.kb.feature;

import com.googlecode.wicket.kendo.ui.form.dropdown.DropDownList;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class FaceFeaturePredicateEditor extends FeatureEditor {

    private Component focusComponent;

    private @SpringBean KnowledgeBaseService kbService;

    public FaceFeaturePredicateEditor (String aId, MarkupContainer aOwner, AnnotationActionHandler aHandler,
                                       IModel<AnnotatorState> aStateModel, IModel<FeatureState> aFeatureStateModel)
    {
        super(aId, aOwner, new CompoundPropertyModel<>(aFeatureStateModel));
        add(new Label("feature", getModelObject().feature.getUiName()));
        add(focusComponent = createFieldComboBox());
    }

    private DropDownList<KBHandle> createFieldComboBox()
    {
        DropDownList<KBHandle> field = new DropDownList<>("value", LambdaModel.of(() -> {
            AnnotationFeature feat = getModelObject().feature;
            List<KBHandle> handles = new LinkedList<>();
            for (KnowledgeBase kb : kbService.getKnowledgeBases(feat.getProject())) {
                handles.addAll(kbService.listProperties(kb, true));
            }
            return new ArrayList<>(handles);
        }), new ChoiceRenderer<>("uiLabel"));
        field.setOutputMarkupId(true);
        field.setMarkupId(ID_PREFIX + getModelObject().feature.getId());
        return field;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();
    }

    @Override
    public Component getFocusComponent()
    {
        return focusComponent;
    }
}

