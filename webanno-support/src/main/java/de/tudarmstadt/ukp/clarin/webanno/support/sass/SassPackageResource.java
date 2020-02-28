package de.tudarmstadt.ukp.clarin.webanno.support.sass;

import java.util.Locale;

import org.apache.wicket.request.resource.CssPackageResource;
import org.apache.wicket.util.resource.IResourceStream;

/**
 * A package resource that uses a custom IResourceStream to load Sass content but return CSS content
 * generated out of it.
 */
public class SassPackageResource
    extends CssPackageResource
{

    /**
     * Constructor.
     *
     * @param scope
     *            This argument will be used to get the class loader for loading the package
     *            resource, and to determine what package it is in
     * @param name
     *            The relative path to the resource
     * @param locale
     *            The locale of the resource
     * @param style
     *            The style of the resource
     * @param variation
     *            The variation of the resource
     */
    public SassPackageResource(Class<?> scope, String name, Locale locale, String style,
            String variation)
    {
        super(scope, name, locale, style, variation);
    }

    @Override
    public IResourceStream getResourceStream()
    {
        IResourceStream resourceStream = super.getResourceStream();
        return new SassResourceStream(resourceStream, getScope().getName());
    }

}
