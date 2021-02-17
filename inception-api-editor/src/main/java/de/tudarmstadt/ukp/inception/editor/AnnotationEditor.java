package de.tudarmstadt.ukp.inception.editor;

import com.fasterxml.jackson.databind.JsonNode;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.controller.AnnotationEditorController;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.support.axios.AxiosResourceReference;
import de.tudarmstadt.ukp.inception.support.vue.VueBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.io.IOException;
import java.util.Map;

import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

public class AnnotationEditor extends AnnotationEditorBase {
    private static final long serialVersionUID = 2983502506977571078L;

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ARC_ID = "arcId";
    private static final String PARAM_ID = "id";
    private static final String PARAM_OFFSETS = "offsets";
    private static final String PARAM_TARGET_SPAN_ID = "targetSpanId";
    private static final String PARAM_ORIGIN_SPAN_ID = "originSpanId";
    private static final String PARAM_SPAN_TYPE = "type";

    private static final String ACTION_CONTEXT_MENU = "contextMenu";


    private final AnnotationEditorController controller;

    public AnnotationEditor(String aId, AnnotationEditorController aController)
    {
        super(aId, aController);

        this.controller = aController;
        System.out.println("Created new editor");

        setOutputMarkupPlaceholderTag(true);

        add(new VueBehavior(
            new PackageResourceReference(getClass(), getClass().getSimpleName() + ".vue")));
    }

    @Override
    protected void render(AjaxRequestTarget aTarget) {

    }


    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(true);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(AxiosResourceReference.get()));

    }

    private String toJson(Object result)
    {
        String json = "[]";
        try {
            if (result instanceof JsonNode) {
                json = JSONUtil.toInterpretableJsonString((JsonNode) result);
            }
            else {
                json = JSONUtil.toInterpretableJsonString(result);
            }
        }
        catch (IOException e) {
            handleError("Unable to produce JSON response", e);
        }
        return json;
    }

    private void handleError(String aMessage, Exception e)
    {
        RequestCycle requestCycle = RequestCycle.get();
        requestCycle.find(AjaxRequestTarget.class)
            .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));

        if (e instanceof AnnotationException) {
            // These are common exceptions happening as part of the user interaction. We do
            // not really need to log their stack trace to the log.
            error(aMessage + ": " + e.getMessage());
            // If debug is enabled, we'll also write the error to the log just in case.
            /*
            if (LOG.isDebugEnabled()) {
                LOG.error("{}: {}", aMessage, e.getMessage(), e);
            }
            return;

             */
        }

        //LOG.error("{}", aMessage, e);
        error(aMessage);
    }
}
