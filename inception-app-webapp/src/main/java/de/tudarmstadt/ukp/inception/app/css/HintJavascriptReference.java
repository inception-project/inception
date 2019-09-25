package de.tudarmstadt.ukp.inception.app.css;

import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class HintJavascriptReference  extends JavaScriptResourceReference
{
    private static final long serialVersionUID = 1L;

    private static final HintJavascriptReference INSTANCE = new HintJavascriptReference();

    /**
     * Gets the instance of the resource reference
     *
     * @return the single instance of the resource reference
     */
    public static HintJavascriptReference get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private HintJavascriptReference()
    {
        super(HintJavascriptReference.class, "hint.js");
    }
}
