package de.tudarmstadt.ukp.clarin.webanno.support.sass;

import org.apache.wicket.Application;
import org.apache.wicket.markup.html.IPackageResourceGuard;
import org.apache.wicket.markup.html.SecurePackageResourceGuard;
import org.apache.wicket.request.resource.IResourceReferenceFactory;
import org.apache.wicket.request.resource.ResourceReferenceRegistry;

/**
 * Bootstrap sass compiler settings accessor class
 */
public final class BootstrapSass
{

    /**
     * Construct.
     */
    private BootstrapSass()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Installs given settings for given application
     *
     * @param app
     *            The current application
     * @param configFactory
     *            The {@link SassCompilerOptionsFactory} to create new
     *            {@link io.bit3.jsass.Options}.
     */
    public static void install(final Application app,
            final SassCompilerOptionsFactory configFactory)
    {

        SassCacheManager cacheManager = new SassCacheManager(configFactory);
        cacheManager.install(app);

        IPackageResourceGuard resourceGuard = app.getResourceSettings().getPackageResourceGuard();
        if (resourceGuard instanceof SecurePackageResourceGuard) {
            SecurePackageResourceGuard securePackageResourceGuard = 
                    (SecurePackageResourceGuard) resourceGuard;
            securePackageResourceGuard.addPattern("+*.scss");
            securePackageResourceGuard.addPattern("+*.sass");
        }

        ResourceReferenceRegistry resourceReferenceRegistry = app.getResourceReferenceRegistry();
        IResourceReferenceFactory delegate = resourceReferenceRegistry
                .getResourceReferenceFactory();
        resourceReferenceRegistry
                .setResourceReferenceFactory(new SassResourceReferenceFactory(delegate));
    }

    /**
     * Installs given settings for given application
     *
     * @param app
     *            The current application
     */
    public static void install(final Application app)
    {
        install(app, null);
    }

}
