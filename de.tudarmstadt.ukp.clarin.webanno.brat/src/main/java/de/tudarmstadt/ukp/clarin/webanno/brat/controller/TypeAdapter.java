package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;

public interface TypeAdapter
{
    /**
     * Add annotations from the CAS, which is controlled by the window size, to the brat
     * response {@link GetDocumentResponse}
     *
     * @param aJcas
     *            The JCAS object containing annotations
     * @param aResponse
     *            A brat response containing annotations in brat protocol
     * @param aBratAnnotatorModel
     *            Data model for brat annotations
     */
    void addToBrat(JCas aJcas, GetDocumentResponse aResponse,
            BratAnnotatorModel aBratAnnotatorModel);
    
//    /**
//     * Update the CAS with new/modification of annotations from brat
//     *
//     * @param aLabelValue
//     *            the value of the annotation
//     * @param aUIData
//     *            Other information obtained from brat such as the start and end offsets
//     * @param aReverse
//     *            If arc direction are in reverse direction, from Dependent to Governor
//     */
//    void addToCas(String aLabelValue, BratAnnotatorUIData aUIData,
//            BratAnnotatorModel aBratAnnotatorModel, boolean aReverse);

//    /**
//     * Delete arc annotation from CAS
//     *
//     * @param aJCas
//     * @param aId
//     */
//    void deleteFromCas(BratAnnotatorUIData aUIData, BratAnnotatorModel aBratAnnotatorModel);
}
