/**
 *
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.YesNoModalPanel;

/**
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public class FinishLink
    extends Panel
{
    private static final long serialVersionUID = 3584950105138069924L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    ModalWindow yesNoModal;

    public FinishLink(String id, final IModel<BratAnnotatorModel> aModel, final FinishImage finishImag)
    {
        super(id, aModel);

        final ModalWindow FinishModal;
        add(FinishModal = new ModalWindow("yesNoModal"));
        FinishModal.setOutputMarkupId(true);

        FinishModal.setInitialWidth(400);
        FinishModal.setInitialHeight(50);
        FinishModal.setResizable(true);
        FinishModal.setWidthUnit("px");
        FinishModal.setHeightUnit("px");
        FinishModal.setTitle("Are you sure you want to finish annotating?");

        AjaxLink<Void> showYesNoModal;

        add(showYesNoModal = new AjaxLink<Void>("showYesNoModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = repository.getUser(username);
                if (repository.getAnnotationDocument(aModel.getObject().getDocument(), user)
                        .getState().equals(AnnotationDocumentState.FINISHED)) {
                    target.appendJavaScript("alert('Document already closed!')");
                }
                else {
                    FinishModal.setContent(new YesNoModalPanel(FinishModal.getContentId(),
                            aModel.getObject(), FinishModal, Mode.ANNOTATION));
                    FinishModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                    {
                        private static final long serialVersionUID = -1746088901018629567L;

                        @Override
                        public void onClose(AjaxRequestTarget target)
                        {
                            target.add(finishImag.setOutputMarkupId(true));
                        }
                    });
                    FinishModal.show(target);
                }

            }
        });
        showYesNoModal.add(finishImag);
    }

}
