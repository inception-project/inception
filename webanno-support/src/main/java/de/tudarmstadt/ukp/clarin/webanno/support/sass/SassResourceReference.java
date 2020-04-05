package de.tudarmstadt.ukp.clarin.webanno.support.sass;

import java.util.Locale;

import org.apache.wicket.request.resource.CssResourceReference;

/**
 * A resource reference for <a href="https://sass-lang.com/">SASS</a> resources. The resources are
 * filtered (stripped comments and whitespace) if there is registered compressor.
 *
 * <p>
 * Supported path schemes for SASS {@code @import} directive are:
 *
 * <ol>
 * <li>Direct e.g. {@code @import "child.scss";} the imported file must be in the same package and
 * JAR.</li>
 * <li>Absolute classpath e.g. {@code @import "classpath!/com/soluvas/web/child.scss";} the imported
 * file can be in any JAR but must be in specified package.</li>
 * <li>Relative classpath e.g. {@code @import "package!child.scss";} the imported file can be in any
 * JAR but must be accessible relative to the {@code scope} given to
 * {@code SassResourceReference}.</li>
 * <li>WebJar e.g. {@code @import "webjars!bootstrap/current/scss/variables.scss";} (current
 * version) or {@code &commat;import "webjars!bootstrap/3.2.0/scss/variables.scss";} (specific
 * version).</li>
 * </ol>
 *
 * @author miha
 * @see org.apache.wicket.settings.ResourceSettings#getCssCompressor()
 */
public class SassResourceReference
    extends CssResourceReference
{
    private static final long serialVersionUID = 1L;

    /**
     * Construct.
     *
     * @param scope
     *            mandatory parameter
     * @param name
     *            mandatory parameter
     */
    public SassResourceReference(final Class<?> scope, final String name)
    {
        this(scope, name, null, null, null);
    }

    /**
     * Construct.
     *
     * @param key
     *            mandatory parameter
     */
    public SassResourceReference(final Key key)
    {
        super(key);
    }

    /**
     * Construct.
     *
     * @param scope
     *            mandatory parameter
     * @param name
     *            mandatory parameter
     * @param locale
     *            resource locale
     * @param style
     *            resource style
     */
    public SassResourceReference(final Class<?> scope, final String name, final Locale locale,
            final String style, final String variation)
    {
        super(scope, name, locale, style, variation);
    }

    @Override
    public SassPackageResource getResource()
    {
        return new SassPackageResource(getScope(), getName(), getLocale(), getStyle(),
                getVariation());
    }
}
