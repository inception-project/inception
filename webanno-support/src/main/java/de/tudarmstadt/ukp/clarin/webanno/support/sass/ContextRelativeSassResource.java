package de.tudarmstadt.ukp.clarin.webanno.support.sass;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.ServletContext;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.core.util.resource.UrlResourceStream;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.io.Streams;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;

/**
 * A context relative resource that uses {@link SassResourceStream} to compile it to CSS
 */
public class ContextRelativeSassResource
    extends ContextRelativeResource
{

    /**
     * The path to the LESS resource in the servlet context
     */
    private final String path;

    /**
     * Constructor.
     *
     * @param path
     *            The path to the LESS resource in the servlet context
     */
    public ContextRelativeSassResource(String path)
    {
        super(path);

        // Make sure there is a leading '/'.
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        this.path = path;
    }

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes)
    {
        final ResourceResponse resourceResponse = new ResourceResponse();

        try {
            final ServletContext context = WebApplication.get().getServletContext();
            final URL resourceURL = context.getResource(path);
            if (resourceURL == null) {
                throw new FileNotFoundException(
                        "Unable to find resource '" + path + "' in the servlet context");
            }
            UrlResourceStream urlResourceStream = new UrlResourceStream(resourceURL);
            final SassResourceStream webExternalResourceStream = new SassResourceStream(
                    urlResourceStream, ContextRelativeSassResource.class.getName());
            resourceResponse.setContentType(webExternalResourceStream.getContentType());
            resourceResponse.setLastModified(webExternalResourceStream.lastModifiedTime());
            resourceResponse.setFileName(path);
            resourceResponse.setWriteCallback(new WriteCallback()
            {
                @Override
                public void writeData(final Attributes attributes) throws IOException
                {
                    try {
                        InputStream inputStream = webExternalResourceStream.getInputStream();
                        try {
                            Streams.copy(inputStream, attributes.getResponse().getOutputStream());
                        }
                        finally {
                            IOUtils.closeQuietly(inputStream);
                        }
                    }
                    catch (ResourceStreamNotFoundException rsnfx) {
                        throw new WicketRuntimeException(rsnfx);
                    }
                }
            });

            return resourceResponse;
        }
        catch (IOException iox) {
            throw new WicketRuntimeException(iox);
        }
    }
}
