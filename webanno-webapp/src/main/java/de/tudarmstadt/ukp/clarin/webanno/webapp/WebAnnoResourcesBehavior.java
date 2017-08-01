package de.tudarmstadt.ukp.clarin.webanno.webapp;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.WebAnnoCssReference;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.WebAnnoJavascriptReference;

public class WebAnnoResourcesBehavior
    extends Behavior
{
    private static final long serialVersionUID = 8847646938685436192L;
    
    private static final WebAnnoResourcesBehavior INSTANCE = new WebAnnoResourcesBehavior();

    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        // Loading WebAnno CSS here so it can override JQuery/Kendo CSS
        aResponse.render(CssHeaderItem.forReference(WebAnnoCssReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(WebAnnoJavascriptReference.get()));
    }
    
    public static WebAnnoResourcesBehavior get()
    {
        return INSTANCE;
    }
}
