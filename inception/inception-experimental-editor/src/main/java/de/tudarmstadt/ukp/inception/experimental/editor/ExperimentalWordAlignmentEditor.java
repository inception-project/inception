package de.tudarmstadt.ukp.inception.experimental.editor;

import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import javax.servlet.ServletContext;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.inception.experimental.editor.resources.ExperimentalAPIWordAlignmentEditorReference;
import de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig;

public class ExperimentalWordAlignmentEditor
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -3812646280364767142L;

    private @SpringBean
    ServletContext servletContext;

    public ExperimentalWordAlignmentEditor(String aId, IModel<AnnotatorState> aModel,
                                        final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
        aResponse.render(JavaScriptHeaderItem
            .forScript("; localStorage.setItem('url','" + constructEndpointUrl() + "')", "0"));
        aResponse.render(forReference(ExperimentalAPIWordAlignmentEditorReference.get()));
    }

    private String constructEndpointUrl()
    {
        Url endPointUrl = Url.parse(String.format("%s%s", servletContext.getContextPath(),
            WebsocketConfig.WS_ENDPOINT));
        endPointUrl.setProtocol("ws");
        return RequestCycle.get().getUrlRenderer().renderFullUrl(endPointUrl);
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {

    }
}
