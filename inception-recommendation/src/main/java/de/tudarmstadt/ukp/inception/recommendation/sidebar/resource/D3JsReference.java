package de.tudarmstadt.ukp.inception.recommendation.sidebar.resource;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class D3JsReference  extends JavaScriptResourceReference
{
    private static final long serialVersionUID = 1L;

    private static final D3JsReference INSTANCE = new D3JsReference();

    /**
     * Gets the instance of the resource reference
     *
     * @return the single instance of the resource reference
     */
    public static D3JsReference get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private D3JsReference()
    {
        super(D3JsReference.class, "d3.min.js");
    }

}
