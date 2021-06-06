package de.tudarmstadt.ukp.inception.experimental.editor;

import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.inception.experimental.api.resources.ExperimentalAPIResourceReference;

public class ExperimentalAdvancedEditor
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -5928851124630974531L;

    public ExperimentalAdvancedEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
        aResponse.render(forReference(ExperimentalAPIResourceReference.get()));
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {

    }
}
