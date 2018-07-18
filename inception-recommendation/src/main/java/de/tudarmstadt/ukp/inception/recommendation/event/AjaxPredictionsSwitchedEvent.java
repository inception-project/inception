package de.tudarmstadt.ukp.inception.recommendation.event;

import org.apache.wicket.ajax.AjaxRequestTarget;

public class AjaxPredictionsSwitchedEvent {
    protected AjaxRequestTarget target;

    public AjaxPredictionsSwitchedEvent(AjaxRequestTarget aTarget) {
        this.target = aTarget;
    }
}
