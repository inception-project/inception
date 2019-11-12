package de.tudarmstadt.ukp.inception.recommendation.event;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

/**
 * this event is used for the tutorial feature. The save recommender event needs to be captured to proceed to the next step of the tutorial
 *
 */
public class RecommenderSavedEvent 
{
    private static final long serialVersionUID = 4618078923202025558L;

    public AjaxRequestTarget target;

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
