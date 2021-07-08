import {AnnotationExperienceAPI} from "../../../../../inception-api-annotation-experimental/src/main/ts/client/AnnotationExperienceAPI";
import {AnnotationExperienceAPIVisualization} from "./visualization/AnnotationExperienceAPIVisualization";
import {AnnotationExperienceAPIActionHandler} from "./actionhandling/AnnotationExperienceAPIActionHandler";
import {AnnotationExperienceAPIImpl} from "../../../../../inception-api-annotation-experimental/src/main/ts/client/AnnotationExperienceAPIImpl";

export class AnnotationExperienceAPIEditor
{
    annotationExperienceAPI: AnnotationExperienceAPI;
    annotationExperienceAPIVisualization: AnnotationExperienceAPIVisualization;
    annotationExperienceAPIActionHandler : AnnotationExperienceAPIActionHandler;

    constructor()
    {
        this.annotationExperienceAPI = new AnnotationExperienceAPIImpl();
        this.annotationExperienceAPIVisualization = new AnnotationExperienceAPIVisualization(this.annotationExperienceAPI);
        this.annotationExperienceAPIActionHandler = new AnnotationExperienceAPIActionHandler(this.annotationExperienceAPI);
        this.annotationExperienceAPIActionHandler.registerDefaultActionHandler();
        this.annotationExperienceAPI.sendDocumentMessageToServer("admin", "41714", [[30,40],[40,50],[0,12],[17,18],[19,19]],"WORD")

    }
}

let annotationExperienceAPIEditor = new AnnotationExperienceAPIEditor();
