package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorFactoryImplBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;

@Component("bratEditor")
public class BratAnnotationEditorFactory
    extends AnnotationEditorFactoryImplBase
{
    @Override
    public String getDisplayName()
    {
        return "brat";
    }
    
    @Override
    public AnnotationEditorBase create(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, JCasProvider aJCasProvider)
    {
        return new BratAnnotationEditor(aId, aModel, aActionHandler, aJCasProvider);
    }
    
    @Override
    public int getOrder()
    {
        return 0;
    }
}
