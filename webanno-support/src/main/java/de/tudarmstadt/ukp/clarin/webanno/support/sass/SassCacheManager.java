package de.tudarmstadt.ukp.clarin.webanno.support.sass;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.Application;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.util.io.Connections;
import org.apache.wicket.util.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;

/**
 * A class that manages the generated CSS content for Sass resources.
 */
public class SassCacheManager
{

    private static final Logger LOG = LoggerFactory.getLogger(SassCacheManager.class);

    private static final MetaDataKey<SassCacheManager> KEY = new MetaDataKey<SassCacheManager>()
    {
        private static final long serialVersionUID = 1L;
    };

    /**
     * A cache that keeps the root SassSource instance per URL.
     */
    private final ConcurrentMap<URL, SassSource> urlSourceCache = new ConcurrentHashMap<>();

    /**
     * A cache that keeps the generated CSS content per root SassSource
     */
    private final ConcurrentMap<SassSource, ConcurrentMap<Time, String>> contentCache = 
            new ConcurrentHashMap<>();

    /**
     * A factory that creates {@link io.bit3.jsass.Options}s.
     */
    private final SassCompilerOptionsFactory optionsFactory;

    /**
     * Creates a sass cache manager with the {@link SassCompilerOptionsFactory} provided. Choose
     * this constructor if you want to use application specific configuration for example custom
     * sass functions.
     *
     * @param optionsFactory
     *            The factory to use when creating a new {@link io.bit3.jsass.Options}.
     */
    public SassCacheManager(SassCompilerOptionsFactory optionsFactory)
    {
        this.optionsFactory = optionsFactory != null ? optionsFactory
                : new SimpleSassCompilerOptionsFactory();
    }

    /**
     * Creates a sass cache manager with a {@link SimpleSassCompilerOptionsFactory}.
     */
    public SassCacheManager()
    {
        this(null);
    }

    /**
     * Returns the SassSource per URL. If there is no entry in the cache then it will be
     * automatically registered
     *
     * @param sassUrl
     *            the URL to the Sass resource file
     * @param scopeClass
     *            The name of the class used as a scope to resolve "package!" dependencies/imports
     * @return The SassSource for the Sass resource file
     */
    public SassSource getSassContext(URL sassUrl, String scopeClass)
    {
        SassSource sassSource = new SassSource(sassUrl.toExternalForm(), scopeClass);
        SassSource oldValue = urlSourceCache.putIfAbsent(sassUrl, sassSource);
        if (oldValue != null) {
            sassSource = oldValue;
        }

        return sassSource;
    }

    /**
     * Returns the generated CSS content per Sass source. If there is no cached content or the root
     * SassSource or any of its imported resources is updated then the CSS content is (re-)generated
     *
     * @param sassSource
     *            The root Sass context for which to load its CSS representation
     * @return The generated CSS content
     */
    public String getCss(SassSource sassSource)
    {
        ConcurrentMap<Time, String> timeToContentMap = contentCache.get(sassSource);
        if (timeToContentMap == null) {
            timeToContentMap = new ConcurrentHashMap<>();
            ConcurrentMap<Time, String> old = contentCache.putIfAbsent(sassSource,
                    timeToContentMap);
            if (old != null) {
                timeToContentMap = old;
            }
        }

        Time lastModifiedTime = getLastModifiedTime(sassSource);
        String cssContent = timeToContentMap.get(lastModifiedTime);

        if (cssContent == null) {
            // We only want to compile the Sass once. If we end up here waiting for another thread
            // to finish will it be ok to wait. It will also probably go faster than compile it once
            // more.
            // Recompile the cached sassSource will append imports once more and we will end up with
            // multiple import references, if there are any in the Sass file.
            synchronized (sassSource) {
                lastModifiedTime = getLastModifiedTime(sassSource);
                cssContent = timeToContentMap.get(lastModifiedTime);

                if (cssContent == null) {
                    // clear any obsolete content
                    timeToContentMap.clear();

                    Compiler compiler = new Compiler();
                    Options options = optionsFactory.newOptions();

                    // jsass doesn't track imports for us, so we need to track them in importer
                    // and add them to root source after successful compilation
                    TrackingImporter trackingImporter = new TrackingImporter(
                            sassSource.getScopeClass(),
                            new UrlImporter(sassSource.getScopeClass()));
                    options.getImporters().add(trackingImporter);
                    options.setOmitSourceMapUrl(false);

                    try {
                        Output result;
                        if (sassSource.getURL().toURI().toString().startsWith("file:")) {
                            // In this execution path, all types of imports including local imports
                            // are supported
                            result = compiler.compileFile(sassSource.getURL().toURI(), null,
                                    options);
                        }
                        else {
                            // This execution path is used when the SCSS resource does not reside
                            // one the file system, e.g. when it is loaded from a JAR.
                            // In this execution path, local imports are not supported, but they
                            // should usually be substitutable by package imports.
                            String scssContent;
                            try (InputStream is = sassSource.getURL().openStream()) {
                                scssContent = IOUtils.toString(is, StandardCharsets.UTF_8);
                            }
                            catch (IOException e) {
                                throw new WicketRuntimeException("Cannot read SASS resource "
                                        + sassSource.getURL().toExternalForm() + ". ", e);
                            }
                            result = compiler.compileString(scssContent, options);
                        }
                        sassSource.addImportedSources(trackingImporter.getImportedSources());
                        cssContent = result.getCss();

                        // Make sure that all last modified files are taken into account before
                        // adding the compiled result to the cache
                        lastModifiedTime = getLastModifiedTime(sassSource);

                        timeToContentMap.put(lastModifiedTime, cssContent);
                    }
                    catch (URISyntaxException ex) {
                        throw new WicketRuntimeException("Cannot create URI for resource.", ex);
                    }
                    catch (CompilationException x) {
                        throw new WicketRuntimeException(
                                "An error occurred while compiling SASS resource "
                                        + sassSource.getURL().toExternalForm() + ". "
                                        + x.getErrorJson(),
                                x);
                    }
                }
            }
        }

        return cssContent;
    }

    /**
     * @param sassSource
     *            The root SassSource which last modification time should be calculated
     * @return The time when either the root LessSource or any of the imported resources has been
     *         last modified
     */
    public Time getLastModifiedTime(SassSource sassSource)
    {
        Time modified = Time.START_OF_UNIX_TIME;
        return findLastModified(sassSource, modified);
    }

    /**
     * Calculates the soonest time when a LessSource or any of its imported resources has been
     * modified
     *
     * @param source
     *            The LessSource which time to check
     * @param time
     *            The last modification time of the parent resource
     * @return The latest modified time of the root LessSource and its imported resources
     */
    private Time findLastModified(SassSource source, Time time)
    {
        Time max = time;
        try {
            Time lastModified = Connections.getLastModified(source.getURL());
            max = Time.maxNullSafe(time, lastModified);

            Collection<SassSource> importedSources = source.getImportedSources();
            if (importedSources != null) {

                SassSource[] importedSourcesArray = importedSources.toArray(new SassSource[0]);
                int size = importedSourcesArray.length;

                for (int i = 0; i < size; i++) {
                    max = findLastModified(importedSourcesArray[i], max);
                }
            }
        }
        catch (IOException iox) {
            LOG.warn("Cannot read the last modification time of a resource "
                    + source.getURL().toExternalForm(), iox);
        }
        return max;
    }

    /**
     * Registers this instance as the one which should be used in this application.
     *
     * @param app
     *            The application used as a scope
     * @see #get()
     */
    public void install(Application app)
    {
        app.setMetaData(KEY, this);
    }

    /**
     * Clears the CSS-cache to enable recomiling at runtime. This will clear the complete cache, not
     * for a single .scss-file.
     *
     * @see #contentCache
     * @see #urlSourceCache
     */
    public void clearCache()
    {
        // Clear both caches to make sure that we have a clean URLSource during the recompiling
        urlSourceCache.clear();
        contentCache.clear();
    }

    /**
     * @return the registered instance of this manager during the start up of the application
     */
    public static SassCacheManager get()
    {
        if (Application.exists()) {
            return get(Application.get());
        }

        throw new IllegalStateException("there is no active application assigned to this thread.");
    }

    /**
     * @param application
     *            the application that keeps the cache manage
     * @return The registered instance of this manager during the start up of the application
     */
    private static SassCacheManager get(Application application)
    {
        return application.getMetaData(KEY);
    }
}
