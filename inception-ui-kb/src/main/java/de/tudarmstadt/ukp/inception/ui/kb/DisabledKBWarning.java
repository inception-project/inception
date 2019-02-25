package de.tudarmstadt.ukp.inception.ui.kb;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.widget.tooltip.TooltipBehavior;

public class DisabledKBWarning
    extends Panel
{
    public DisabledKBWarning(String aId, IModel<String> aModel)
    {
        super(aId);

        WebMarkupContainer warning = new WebMarkupContainer("warning");

        TooltipBehavior tip = new TooltipBehavior();
        tip.setOption("content", Options.asString(aModel.getObject()));
        tip.setOption("width", Options.asString("300px"));
        warning.add(tip);
        add(warning);

    }
}
