import {Annotation} from "./Annotation";

export class ServerMessage
{
    private _document : number;
    private _viewport :number[][];
    private _viewportText : string[];
    private _offsetType : string;

    private _annotations : Annotation [];

    private _annotationAddress : number;
    private _annotationType : string;
    private _annotationOffsetBegin : number;
    private _annotationOffsetEnd : number;
    private _annotationText : string;


    constructor()
    {
    }


    get document(): number {
        return this._document;
    }

    set document(value: number) {
        this._document = value;
    }

    get viewport(): number[][] {
        return this._viewport;
    }

    set viewport(value: number[][]) {
        this._viewport = value;
    }

    get viewportText(): string[] {
        return this._viewportText;
    }

    set viewportText(value: string[]) {
        this._viewportText = value;
    }

    get offsetType(): string {
        return this._offsetType;
    }

    set offsetType(value: string) {
        this._offsetType = value;
    }

    get annotations(): Annotation[] {
        return this._annotations;
    }

    set annotations(value: Annotation[]) {
        this._annotations = value;
    }

    get annotationAddress(): number {
        return this._annotationAddress;
    }

    set annotationAddress(value: number) {
        this._annotationAddress = value;
    }

    get annotationType(): string {
        return this._annotationType;
    }

    set annotationType(value: string) {
        this._annotationType = value;
    }

    get annotationOffsetBegin(): number {
        return this._annotationOffsetBegin;
    }

    set annotationOffsetBegin(value: number) {
        this._annotationOffsetBegin = value;
    }

    get annotationOffsetEnd(): number {
        return this._annotationOffsetEnd;
    }

    set annotationOffsetEnd(value: number) {
        this._annotationOffsetEnd = value;
    }

    get annotationText(): string {
        return this._annotationText;
    }

    set annotationText(value: string) {
        this._annotationText = value;
    }
}
