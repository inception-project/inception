package de.tudarmstadt.ukp.inception.experimental.editor.websocket;

import de.tudarmstadt.ukp.inception.experimental.editor.websocket.model.WebsocketMessage;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import java.util.Set;

import static org.apache.commons.collections4.SetUtils.unmodifiableSet;

@Controller
public class WebsocketControllerImpl implements WebsocketController {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Set<String> genericEvents = unmodifiableSet();

    private static final String SELECT_ANNOTATION_BY_CLIENT_EVENT = "/selectEvent";
    private static final String SELECT_ANNOTATION_BY_CLIENT_EVENT_TOPIC = "/selectTopic" + SELECT_ANNOTATION_BY_CLIENT_EVENT;


    public WebsocketControllerImpl() {
    }

    @EventListener
    @Override
    public void onApplicationEvent(ApplicationEvent aEvent) {
        System.out.println("---- EVENT ---- : " + aEvent);
    }

    @Override
    public String getTopicChannel() {
        return SELECT_ANNOTATION_BY_CLIENT_EVENT;
    }


    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        return exception.getMessage();
    }

    @MessageMapping("/select_annotation_by_client")
    @SendTo("/selected_annotation")
    public Annotation greeting(WebsocketMessage aMmessage) throws Exception {
        System.out.println("Reveiced SELECT_ANNOTATION BY CLIENT");
        return new Annotation();
    }
}
