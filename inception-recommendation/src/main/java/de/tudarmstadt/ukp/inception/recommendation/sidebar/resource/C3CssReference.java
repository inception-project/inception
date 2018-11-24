package de.tudarmstadt.ukp.inception.recommendation.sidebar.resource;

import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class C3CssReference  extends JavaScriptResourceReference
{
    private static final long serialVersionUID = 1L;

    private static final C3CssReference INSTANCE = new C3CssReference();

    /**
     * Gets the instance of the resource reference
     *
     * @return the single instance of the resource reference
     */
    public static C3CssReference get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private C3CssReference()
    {
        super(C3CssReference.class, "c3.min.css");
    }

}
