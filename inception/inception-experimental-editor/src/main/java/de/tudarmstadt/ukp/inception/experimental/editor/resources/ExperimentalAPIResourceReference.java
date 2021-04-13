package de.tudarmstadt.ukp.inception.experimental.editor.resources;

import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class ExperimentalAPIResourceReference
    extends JavaScriptResourceReference
{
    private static final long serialVersionUID = 1L;

    private static final ExperimentalAPIResourceReference INSTANCE = new ExperimentalAPIResourceReference();

    /**
     * Gets the instance of the resource reference
     *
     * @return the single instance of the resource reference
     */
    public static ExperimentalAPIResourceReference get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private ExperimentalAPIResourceReference()
    {
        super(ExperimentalAPIResourceReference.class, "../ts/Editor.js");
    }
}