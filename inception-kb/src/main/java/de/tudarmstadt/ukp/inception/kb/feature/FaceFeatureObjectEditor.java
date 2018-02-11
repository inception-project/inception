package de.tudarmstadt.ukp.inception.kb.feature;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;

/**
 *
 */
public class FaceFeatureObjectEditor extends FeatureEditor {

    public FaceFeatureObjectEditor(String aId, MarkupContainer aOwner, IModel<FeatureState> aModel) {
        super(aId, aOwner, aModel);
    }

    @Override
    public Component getFocusComponent() {
        return null;
    }
}
