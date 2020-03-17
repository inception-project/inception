package de.tudarmstadt.ukp.clarin.webanno.support.sass;

import org.apache.wicket.request.resource.ContextRelativeResource;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.apache.wicket.util.resource.ResourceUtils;

/**
 * A resource reference to a SASS resource located at the servlet context
 */
public class ContextRelativeSassResourceReference
    extends ContextRelativeResourceReference
{

    public static final String CONTEXT_RELATIVE_SASS_REFERENCE_VARIATION = "wicketcrlrrv";

    public ContextRelativeSassResourceReference(String name)
    {
        this(name, ResourceUtils.MIN_POSTFIX_DEFAULT, true);
    }

    public ContextRelativeSassResourceReference(String name, boolean minifyIt)
    {
        this(name, ResourceUtils.MIN_POSTFIX_DEFAULT, minifyIt);
    }

    public ContextRelativeSassResourceReference(String name, String minPostfix)
    {
        this(name, minPostfix, true);
    }

    public ContextRelativeSassResourceReference(String name, String minPostfix, boolean minifyIt)
    {
        super(name, minPostfix, minifyIt);
    }

    @Override
    protected ContextRelativeResource buildContextRelativeResource(String name, String minPostfix)
    {
        String minifiedName = name;

        if (canBeMinified()) {
            minifiedName = ResourceUtils.getMinifiedName(name, minPostfix);
        }
        return new ContextRelativeSassResource(minifiedName);
    }

    @Override
    public String getVariation()
    {
        return CONTEXT_RELATIVE_SASS_REFERENCE_VARIATION;
    }
}
