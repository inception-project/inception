package de.tudarmstadt.ukp.inception.app.ui.externalsearch;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

@MountPath("/documentDetails.html")
public class DocumentDetailsPage extends ApplicationPageBase
{
    private final WebMarkupContainer mainContainer = new WebMarkupContainer("mainContainer");

    public DocumentDetailsPage(String aId, String aText)
    {
        mainContainer.add(new Label("title", aId));
        Label textElement = new Label("text", aText);
        textElement.setOutputMarkupId(true);
        textElement.setEscapeModelStrings(false);
        mainContainer.add(textElement);
        add(mainContainer);
    }
}
