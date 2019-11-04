package de.tudarmstadt.ukp.inception.recommendation.event;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public class ErrorForJSEvent 
{
    private static final long serialVersionUID = 4618078923202025558L;

    private final String errorMessage;
    public AjaxRequestTarget target;

    public ErrorForJSEvent(Object aSource, String aerrorMessage)
    {
        errorMessage = aerrorMessage;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    @Override public String toString()
    { 
        return errorMessage;
    }

	public AjaxRequestTarget getTarget() {
		return target;
	}

	public void setTarget(AjaxRequestTarget target) {
		this.target = target;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}
