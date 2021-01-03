package de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.vue;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

public class VueComponent
    extends Panel
{
    private static final long serialVersionUID = 2590487883771158315L;

    public VueComponent(String aId, String aVueComponentFile)
    {
        this(aId, aVueComponentFile, null);
    }

    public VueComponent(String aId, String aVueComponentFile, IModel<?> aModel)
    {
        super(aId, aModel);

        add(new VueBehavior(new PackageResourceReference(this.getClass(), aVueComponentFile)));
    }
}
