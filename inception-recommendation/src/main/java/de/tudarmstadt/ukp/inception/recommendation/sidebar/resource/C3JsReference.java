package de.tudarmstadt.ukp.inception.recommendation.sidebar.resource;

import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class C3JsReference  extends JavaScriptResourceReference
{
    private static final long serialVersionUID = 1L;

    private static final C3JsReference INSTANCE = new C3JsReference();

    /**
     * Gets the instance of the resource reference
     *
     * @return the single instance of the resource reference
     */
    public static C3JsReference get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private C3JsReference()
    {
        super(C3JsReference.class, "c3.min.js");
    }

}
