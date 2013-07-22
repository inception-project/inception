/**
 *
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.ExportModalWindowPage;

/**
 * @author Seid Muhie Yimam
 *
 */
public class ExportModalPanel
    extends Panel
{
    private static final long serialVersionUID = 671214149298791793L;

    public ExportModalPanel(String id, final IModel<BratAnnotatorModel> aModel)
    {
        super(id, aModel);

        final ModalWindow exportModal;
        add(exportModal = new ModalWindow("exportModal"));

        exportModal.setCookieName("modal-1");
        exportModal.setInitialWidth(550);
        exportModal.setInitialHeight(450);
        exportModal.setResizable(true);
        exportModal.setWidthUnit("px");
        exportModal.setHeightUnit("px");
        exportModal.setTitle("Export Annotated data to a given Format");

        exportModal.setPageCreator(new ModalWindow.PageCreator()
        {
            private static final long serialVersionUID = -2827824968207807739L;

            @Override
            public Page createPage()
            {
                return new ExportModalWindowPage(exportModal, aModel.getObject());
            }

        });
        add(new AjaxLink<Void>("showExportModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (aModel.getObject().getDocument() == null) {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
                else {
                    exportModal.show(target);
                }

            }
        });

    }

}
