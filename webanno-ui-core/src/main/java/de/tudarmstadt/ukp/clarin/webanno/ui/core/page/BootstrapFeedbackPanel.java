package de.tudarmstadt.ukp.clarin.webanno.ui.core.page;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

/**
 * {@code FeedbackPanel} which applies Bootstrap alert styles to feedback messages.
 */
public class BootstrapFeedbackPanel extends FeedbackPanel {

    private static final long serialVersionUID = 5171764027460264375L;

    public BootstrapFeedbackPanel(String id) {
        super(id);
    }

    public BootstrapFeedbackPanel(String id, IFeedbackMessageFilter filter) {
        super(id, filter);
    }

    @Override
    protected String getCSSClass(FeedbackMessage message) {
        String cssClass = "alert";
        switch (message.getLevel()) {
        case FeedbackMessage.ERROR:
        case FeedbackMessage.FATAL:
            cssClass += " alert-danger";
            break;
        case FeedbackMessage.SUCCESS:
            cssClass += " alert-success";
            break;
        case FeedbackMessage.WARNING:
            cssClass += " alert-warning";
            break;
        case FeedbackMessage.INFO:
            cssClass += " alert-info";
            break;
        default:
            break;
        }        
        return cssClass;
    }

}
