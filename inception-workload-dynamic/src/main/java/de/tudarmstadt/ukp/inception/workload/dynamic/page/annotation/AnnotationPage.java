package de.tudarmstadt.ukp.inception.workload.dynamic.page.annotation;

/*
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectType;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.wicketstuff.annotation.mount.MountPath;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.*;

@MountPath(value = "/annotation.html", alt = { "/annotate/${" + PAGE_PARAM_PROJECT_ID + "}",
    "/annotate/${" + PAGE_PARAM_PROJECT_ID + "}/${" + PAGE_PARAM_DOCUMENT_ID + "}",
    "/annotate-by-name/${" + PAGE_PARAM_PROJECT_ID + "}/${" + PAGE_PARAM_DOCUMENT_NAME + "}" })
@ProjectType(id = WebAnnoConst.PROJECT_TYPE_ANNOTATION, prio = 100)
public class AnnotationPage extends de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage {

    //SpringBeans
    private @SpringBean UserDao userRepository;

    public AnnotationPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        setModel(Model.of(new AnnotatorStateImpl(Mode.ANNOTATION)));
        // Ensure that a user is set
        getModelObject().setUser(userRepository.getCurrentUser());

        StringValue project = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        StringValue document = aPageParameters.get(PAGE_PARAM_DOCUMENT_ID);
        StringValue name = aPageParameters.get(PAGE_PARAM_DOCUMENT_NAME);
        StringValue focus = aPageParameters.get(PAGE_PARAM_FOCUS);
        if (focus == null) focus = StringValue.valueOf(0);

        handleParameters(project, document, name, focus, true);
        commonInit(focus);
    }

}

 */
