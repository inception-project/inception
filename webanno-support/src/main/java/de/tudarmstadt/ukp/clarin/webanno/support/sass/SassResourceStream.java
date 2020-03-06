package de.tudarmstadt.ukp.clarin.webanno.support.sass;

import java.net.URL;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.core.util.resource.UrlResourceStream;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.resource.AbstractStringResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamWrapper;
import org.apache.wicket.util.time.Time;

/**
 * A IResourceStream that loads the generated CSS content for Sass resources
 */
public class SassResourceStream
    extends AbstractStringResourceStream
{

    /**
     * The LessSource for the root Sass resource. Any LessSource can have children resources -
     * imported resources
     */
    private final SassSource sassSource;

    /**
     * Constructor.
     *
     * @param sassStream
     *            The resource stream that loads the Sass content. Only UrlResourceStream is
     *            supported at the moment!
     * @param scopeClass
     *            The name of the class used as a scope to resolve "package!" dependencies/imports
     */
    public SassResourceStream(IResourceStream sassStream, String scopeClass)
    {
        Args.notNull(sassStream, "sassStream");

        while (sassStream instanceof ResourceStreamWrapper) {
            ResourceStreamWrapper wrapper = (ResourceStreamWrapper) sassStream;
            try {
                sassStream = wrapper.getDelegate();
            }
            catch (Exception x) {
                throw new WicketRuntimeException(x);
            }
        }

        if (!(sassStream instanceof UrlResourceStream)) {
            throw new IllegalArgumentException(String.format("%s can work only with %s",
                    SassResourceStream.class.getSimpleName(), UrlResourceStream.class.getName()));
        }

        URL sassUrl = ((UrlResourceStream) sassStream).getURL();

        SassCacheManager cacheManager = SassCacheManager.get();

        this.sassSource = cacheManager.getSassContext(sassUrl, scopeClass);
    }

    @Override
    protected String getString()
    {
        SassCacheManager cacheManager = SassCacheManager.get();
        return cacheManager.getCss(sassSource);
    }

    @Override
    public Time lastModifiedTime()
    {
        SassCacheManager cacheManager = SassCacheManager.get();
        return cacheManager.getLastModifiedTime(sassSource);
    }

    @Override
    public String getContentType()
    {
        return "text/css";
    }
}
