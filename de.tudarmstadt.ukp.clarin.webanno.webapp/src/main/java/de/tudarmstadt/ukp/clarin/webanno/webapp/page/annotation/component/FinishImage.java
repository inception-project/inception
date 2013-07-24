/**
 *
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * @author Seid Muhie Yimam
 *
 */
public class FinishImage
    extends WebMarkupContainer
{

    private static final long serialVersionUID = -4931039843586219625L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    WebMarkupContainer finish;

    public void setModel(IModel<BratAnnotatorModel> aModel)
    {
        setDefaultModel(aModel);
    }

    public void setModelObject(BratAnnotatorModel aModel)
    {
        setDefaultModelObject(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<BratAnnotatorModel> getModel()
    {
        return (IModel<BratAnnotatorModel>) getDefaultModel();
    }

    public BratAnnotatorModel getModelObject()
    {
        return (BratAnnotatorModel) getDefaultModelObject();
    }


    @SuppressWarnings("deprecation")
    public FinishImage(String id, final IModel<BratAnnotatorModel> aModel)
    {
        super(id, aModel);

        add(new AttributeModifier("src", true, new LoadableDetachableModel<String>()
        {
            private static final long serialVersionUID = 1562727305401900776L;

            @Override
            protected String load()
            {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = repository.getUser(username);

                if (aModel.getObject().getProject() != null
                        && aModel.getObject().getDocument() != null) {
                    if (repository.existsAnnotationDocument(aModel.getObject().getDocument(), user)
                            && repository
                                    .getAnnotationDocument(aModel.getObject().getDocument(), user)
                                    .getState().equals(AnnotationDocumentState.FINISHED)) {
                        return "images/cancel.png";
                    }
                    else {
                        return "images/accept.png";
                    }
                }
                else {
                    return "images/accept.png";
                }

            }
        }));
    }

    }
