package de.tudarmstadt.ukp.inception.recommendation.sidebar;

import org.apache.wicket.ajax.AjaxRequestTarget;

public class DropDownEvent
{
    public String selectedValue;
    public AjaxRequestTarget target;

    public String getSelectedValue()
    {
        return selectedValue;
    }

    public void setSelectedValue(String selectedValue)
    {
        this.selectedValue = selectedValue;
    }

    public AjaxRequestTarget getTarget()
    {
        return target;
    }

    public void setTarget(AjaxRequestTarget target)
    {
        this.target = target;
    }
}
