import {Stomp} from "@stomp/stompjs";
import {ServerMessage} from "./util/ServerMessage";

export interface AnnotationExperienceAPI {

    unsubscribe(aChannel: string);

    disconnect();

    editAnnotation(aId, aAnnotationType);


    sendDocumentMessageToServer(aUsername, aDocument, aOffset, aOffsetType);

    sendViewportMessageToServer(aUsername, aViewport, aOffsetType);

    sendSelectAnnotationMessageToServer(aUsername, aId);

    sendCreateAnnotationMessageToServer(begin, end, aAnnotationType);

    sendUpdateAnnotationMessageToServer(aId, aAnnotationType);

    sendDeleteAnnotationMessageToServer(aId);


    receiveNewDocumentMessageByServer(aMessage: ServerMessage);

    receiveNewViewportMessageByServer(aMessage: ServerMessage);

    receiveSelectedAnnotationMessageByServer(aMessage: ServerMessage);

    receiveAnnotationMessageByServer(aMessage: ServerMessage);
}