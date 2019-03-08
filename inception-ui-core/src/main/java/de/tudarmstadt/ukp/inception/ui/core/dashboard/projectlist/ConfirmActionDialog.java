package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Panel;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.AjaxCallback;

public class ConfirmActionDialog
    extends ModalWindow
{

    private static final long serialVersionUID = -3327097907575875735L;

    private AjaxCallback confirmAction;
    private AjaxCallback cancelAction;

    public ConfirmActionDialog(String id, String aTitle)
    {
        super(id);
        setTitle(aTitle);
        setContent(new ContentPanel(getContentId()));
        setCssClassName("w_blue w_flex");
        // do not show warning about leaving the page
        showUnloadConfirmation(false);
    }

    public AjaxCallback getConfirmAction()
    {
        return confirmAction;
    }

    public void setConfirmAction(AjaxCallback confirmAction)
    {
        this.confirmAction = confirmAction;
    }

    public AjaxCallback getCancelAction()
    {
        return cancelAction;
    }

    public void setCancelAction(AjaxCallback cancelAction)
    {
        this.cancelAction = cancelAction;
    }

    private class ContentPanel
        extends Panel
    {
        private static final long serialVersionUID = -8656143706550925048L;

        public ContentPanel(String aId)
        {
            super(aId);

            add(new AjaxLink<Void>("confirm")
            {

                private static final long serialVersionUID = 1746960252534055377L;

                @Override
                public void onClick(AjaxRequestTarget aTarget)
                {
                    if (confirmAction != null) {
                        try {
                            confirmAction.accept(aTarget);
                        }
                        catch (Exception e) {
                            LoggerFactory.getLogger(getPage().getClass())
                                    .error("Error: " + e.getMessage(), e);
                        }
                    }
                    close(aTarget);
                }

            });
            add(new AjaxLink<Void>("cancel")
            {

                private static final long serialVersionUID = 7263243223630333200L;

                @Override
                public void onClick(AjaxRequestTarget aTarget)
                {
                    if (cancelAction != null) {
                        try {
                            cancelAction.accept(aTarget);
                        }
                        catch (Exception e) {
                            LoggerFactory.getLogger(getPage().getClass())
                                    .error("Error: " + e.getMessage(), e);
                        }
                    }
                    close(aTarget);
                }

            });
        }
    }

}
