package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event;

import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;

public class RenderSlotsEvent {

    private final IPartialPageRequestHandler requestHandler;

    public RenderSlotsEvent(IPartialPageRequestHandler aRequestHandler) {
        requestHandler = aRequestHandler;
    }

    public IPartialPageRequestHandler getRequestHandler() {
        return requestHandler;
    }
}
