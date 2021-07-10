import {Span} from "../../model/Span";

export class NewDocumentResponse
{
    documentId : number;
    viewportText : string[];
    spanAnnotations : Span[];
}