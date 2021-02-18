package de.tudarmstadt.ukp.inception.editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.wicket.jquery.ui.settings.JQueryUILibrarySettings;
import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorRenderedMetaDataKey;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.controller.AnnotationEditorController;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.*;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.inception.support.axios.AxiosResourceReference;
import org.apache.uima.cas.CAS;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

public class AnnotationEditor extends AnnotationEditorBase {
    private static final long serialVersionUID = 2983502506977571078L;

    private final User currentUser;
    private final Project currentProject;
    private SourceDocument currentDocument;
    private String document_name = "TTTTTTTTT";

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ARC_ID = "arcId";
    private static final String PARAM_ID = "id";
    private static final String PARAM_OFFSETS = "offsets";
    private static final String PARAM_TARGET_SPAN_ID = "targetSpanId";
    private static final String PARAM_ORIGIN_SPAN_ID = "originSpanId";
    private static final String PARAM_SPAN_TYPE = "type";

    private static final String ACTION_CONTEXT_MENU = "contextMenu";

    private final AnnotationEditorController controller;

    public AnnotationEditor(String aId, AnnotationEditorController aController, String jsonUser, String jsonProject) throws IOException {
        super(aId, aController);

        controller = aController;
        currentUser = JSONUtil.fromJsonString(User.class, jsonUser);
        currentProject = JSONUtil.fromJsonString(Project.class, jsonProject);

        initController();
    }

    //Whenever the document changes, reinit the controller with the correct values for user, document and project
    private void initController()
    {
        controller.initController(currentUser, currentProject.getId());
    }

    private void updateDocument(String jsonDocument) throws IOException
    {
        currentDocument = JSONUtil.fromJsonString(SourceDocument.class, jsonDocument);
        controller.updateDocumentService(currentDocument);
    }
    private void render(CAS aCas)
    {
        AnnotatorState aState = getModelObject();
        VDocument vdoc = render(aCas, aState.getWindowBeginOffset(), aState.getWindowEndOffset());
        System.out.println(vdoc);
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {

    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
    }

}
