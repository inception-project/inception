package de.tudarmstadt.ukp.inception.ui.core.footer.resources;

import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class TutorialJavascriptReference  extends JavaScriptResourceReference
{
    private static final long serialVersionUID = 1L;

    private static final TutorialJavascriptReference INSTANCE = new TutorialJavascriptReference();

    /**
     * Gets the instance of the resource reference
     *
     * @return the single instance of the resource reference
     */
    public static TutorialJavascriptReference get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private TutorialJavascriptReference()
    {
        super(TutorialJavascriptReference.class, "tutorial.js");
    }
}
