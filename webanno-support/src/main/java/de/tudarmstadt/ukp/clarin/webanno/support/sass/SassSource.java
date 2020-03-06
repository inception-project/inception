package de.tudarmstadt.ukp.clarin.webanno.support.sass;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents information required to load Sass source from:
 * <ul>
 * <li>WebJars</li>
 * <li>classpath</li>
 * <li>same package, different jar</li>
 * </ul>
 */
class SassSource
{

    /**
     * The Sass resource filepath.
     */
    private final String filepath;

    /**
     * The scope class used with SassResourceReference. Used to resolve dependencies with scheme
     * "package!"
     */
    private final String scopeClass;

    /**
     * The collections of Sass sources imported to this source.
     */
    private final Collection<SassSource> importedSources;

    /**
     * Constructor
     *
     * @param filepath
     *            The Sass resource filepath
     * @param scopeClass
     *            The scope class used to load this Sass resource. Also used to resolve "package!"
     *            dependencies
     */
    SassSource(String filepath, String scopeClass)
    {
        this.filepath = filepath;
        this.scopeClass = scopeClass;
        this.importedSources = new ArrayList<>();
    }

    /**
     *
     * @return the url of Sass resource
     */
    public URL getURL()
    {
        try {
            return new URL(filepath);
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException("Cannot create a URL to a resource", e);
        }
    }

    /**
     *
     * @return The scope class used to load this Sass resource.
     */
    public String getScopeClass()
    {
        return scopeClass;
    }

    public Collection<SassSource> getImportedSources()
    {
        return importedSources;
    }

    public void addImportedSources(Collection<SassSource> sources)
    {
        importedSources.addAll(sources);
    }
}
