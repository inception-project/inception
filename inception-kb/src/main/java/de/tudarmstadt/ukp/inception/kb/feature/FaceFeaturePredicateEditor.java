package de.tudarmstadt.ukp.inception.kb.feature;

import com.googlecode.wicket.kendo.ui.form.dropdown.DropDownList;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
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
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class FaceFeaturePredicateEditor extends FeatureEditor {
    private Label feature;
    private Component featureKey;
    private DropDownList<KBHandle>  featureValue;

    private @SpringBean
    KnowledgeBaseService kbService;

    public FaceFeaturePredicateEditor(String aId, MarkupContainer aOwner, IModel<FeatureState> aModel) {
        super(aId, aOwner, aModel);
        updateName();
        updateKey();
        updateValue();
    }


    public void updateName() {
        feature = new Label("feature", getModelObject().feature.getUiName());
        add(feature);

    }

    public void updateKey() {
        featureKey = new Label("featureKey", "<Select to fill>");
        add(featureKey);
    }

    public void updateValue() {
        featureValue = new DropDownList<>("featureValue", LambdaModel.of(() -> {
            AnnotationFeature feat = getModelObject().feature;
            List<KBHandle> handles = new LinkedList<>();
            for (KnowledgeBase kb : kbService.getKnowledgeBases(feat.getProject())) {
                handles.addAll(kbService.listProperties(kb, true));
            }
            return new ArrayList<>(handles);
        }), new ChoiceRenderer<>("uiLabel"));
        featureValue.setOutputMarkupId(true);
        featureValue.setMarkupId(ID_PREFIX + getModelObject().feature.getId());
        add(featureValue);
    }
    @Override
    public Component getFocusComponent() {
        return featureValue;
    }
}
