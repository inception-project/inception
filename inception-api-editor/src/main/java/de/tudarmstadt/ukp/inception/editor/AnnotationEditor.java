package de.tudarmstadt.ukp.inception.editor;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.controller.AnnotationEditorController;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ContextMenu;
import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;

import java.io.IOException;

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

    private final ContextMenu contextMenu;
    private final WebMarkupContainer vis;
    private CAS cas;


    public AnnotationEditor(String aId, AnnotationEditorController aController, String aJsonUser, String aJsonProject) throws IOException {
        super(aId, aController);

        controller = aController;
        currentUser = JSONUtil.fromJsonString(User.class, aJsonUser);
        currentProject = JSONUtil.fromJsonString(Project.class, aJsonProject);

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);
        add(vis);

        contextMenu = new ContextMenu("contextMenu");
        add(contextMenu);

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

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        try {
            cas = getCasProvider().get();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

    }

}
