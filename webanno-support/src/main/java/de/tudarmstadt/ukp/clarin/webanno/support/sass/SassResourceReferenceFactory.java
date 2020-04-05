package de.tudarmstadt.ukp.clarin.webanno.support.sass;

import org.apache.wicket.request.resource.IResourceReferenceFactory;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;

/**
 * A factory that creates a new instance of {@link SassResourceReference} when there is no
 * registered one in Wicket's {@link org.apache.wicket.request.resource.ResourceReferenceRegistry}
 * for a resource with extension <em>.scss</em> or <em>.sass</em>.
 */
public class SassResourceReferenceFactory
    implements IResourceReferenceFactory
{

    /**
     * A factory to delegate the creation of the ResourceReference if the key's name doesn't have
     * extension <em>.scss</em> or <em>.sass</em>
     */
    private final IResourceReferenceFactory delegate;

    /**
     * Constructor.
     *
     * @param delegate
     *            A factory to delegate the creation of the ResourceReference if the key's name
     *            doesn't have extension <em>.scss</em> or <em>.sass</em>
     */
    public SassResourceReferenceFactory(IResourceReferenceFactory delegate)
    {
        this.delegate = Args.notNull(delegate, "delegate");
    }

    @Override
    public ResourceReference create(ResourceReference.Key key)
    {
        String name = key.getName();
        String variation = key.getVariation();
        if (ContextRelativeSassResourceReference.CONTEXT_RELATIVE_SASS_REFERENCE_VARIATION
                .equals(variation)) {
            return new ContextRelativeSassResourceReference(name); // TODO what about the min
                                                                   // extension ?!
        }
        if (name != null && (name.endsWith(".scss") || name.endsWith(".sass"))) {
            return new SassResourceReference(key);
        }
        else {
            return delegate.create(key);
        }
    }
}
