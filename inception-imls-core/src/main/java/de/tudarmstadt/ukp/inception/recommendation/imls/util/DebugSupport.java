/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;

/** 
 * A helper class to create an excel file containing debug information.
 * It was used during the development of the framework. 
 * 
 *
 *
 */
public class DebugSupport
{
    private static final String EXCEL_DELIMITER = "\t";

    private DebugSupport()
    {
    }

    public static void printComparisonAsExcel(FileWriter fw, List<AnnotationObject> expected,
            List<AnnotationObject> generated)
        throws IOException
    {
        if (fw != null) {
            StringBuilder sb = new StringBuilder();
            Iterator<AnnotationObject> itExpected = expected.iterator();
            Iterator<AnnotationObject> itGenerated = generated.iterator();
            AnnotationObject aoExpected = itExpected.next();
            AnnotationObject aoGenerated = itGenerated.next();

            // Headline
            sb.append("Expected - #Character").append(EXCEL_DELIMITER).append("Expected - #Token")
                    .append(EXCEL_DELIMITER).append("Expected - Annotation").append(EXCEL_DELIMITER)
                    .append("Expected - CoveredText").append(EXCEL_DELIMITER)
                    .append("Predicted - CoveredText").append(EXCEL_DELIMITER)
                    .append("Predicted - Annotation").append(EXCEL_DELIMITER)
                    .append("Predicted - #Token").append(EXCEL_DELIMITER)
                    .append("Predicted - #Character").append("\n");

            while (aoExpected != null && aoGenerated != null) {
                Offset offsetExpected = aoExpected.getOffset();
                Offset offsetGenerated = aoGenerated.getOffset();
                if (offsetExpected.getBeginCharacter() < offsetGenerated.getBeginCharacter()) {
                    printAnnotationObjectComparison(sb, aoExpected, null, EXCEL_DELIMITER);
                    if (itExpected.hasNext()) {
                        aoExpected = itExpected.next();
                    }
                    else {
                        aoExpected = null;
                    }

                }
                else if (offsetExpected.getBeginCharacter() > offsetGenerated.getBeginCharacter()) {
                    printAnnotationObjectComparison(sb, null, aoGenerated, EXCEL_DELIMITER);
                    if (itGenerated.hasNext()) {
                        aoGenerated = itGenerated.next();
                    }
                    else {
                        aoGenerated = null;
                    }

                }
                else {
                    printAnnotationObjectComparison(sb, aoExpected, aoGenerated, EXCEL_DELIMITER);
                    if (itExpected.hasNext()) {
                        aoExpected = itExpected.next();
                    }
                    else {
                        aoExpected = null;
                    }
                    if (itGenerated.hasNext()) {
                        aoGenerated = itGenerated.next();
                    }
                    else {
                        aoGenerated = null;
                    }
                }
                flushDataRow(fw, sb);
            }

            while (aoExpected != null) {
                printAnnotationObjectComparison(sb, aoExpected, null, EXCEL_DELIMITER);
                if (itExpected.hasNext()) {
                    aoExpected = itExpected.next();
                }
                else {
                    aoExpected = null;
                }
                flushDataRow(fw, sb);
            }

            while (aoGenerated != null) {
                printAnnotationObjectComparison(sb, null, aoGenerated, EXCEL_DELIMITER);
                if (itGenerated.hasNext()) {
                    aoGenerated = itGenerated.next();
                }
                else {
                    aoGenerated = null;
                }
                flushDataRow(fw, sb);
            }

            fw.close();
        }
    }

    private static void flushDataRow(FileWriter fw, StringBuilder sb)
        throws IOException
    {
        sb.append("\n");
        fw.write(sb.toString());
        fw.flush();
        sb.setLength(0);
    }

    private static void printAnnotationObjectComparison(StringBuilder sb, AnnotationObject ao1,
            AnnotationObject ao2, String delimiter)
    {
        printAnnotationObject(sb, ao1, delimiter);
        sb.append(delimiter);
        printAnnotationObjectInverted(sb, ao2, delimiter);
    }

    private static void printAnnotationObject(StringBuilder sb, AnnotationObject ao,
            String delimiter)
    {
        if (ao == null) {
            return;
        }

        Offset offset = ao.getOffset();
        sb.append(offset.getBeginCharacter()).append("..").append(offset.getEndCharacter());
        sb.append(delimiter);
        sb.append(offset.getBeginToken()).append("..").append(offset.getEndToken());
        sb.append(delimiter);
        sb.append(ao.getLabel());
        sb.append(delimiter);
        sb.append(ao.getCoveredText());
    }

    private static void printAnnotationObjectInverted(StringBuilder sb, AnnotationObject ao,
            String delimiter)
    {
        if (ao == null) {
            return;
        }

        Offset offset = ao.getOffset();
        sb.append(ao.getCoveredText());
        sb.append(delimiter);
        sb.append(ao.getLabel());
        sb.append(delimiter);
        sb.append(offset.getBeginToken()).append("..").append(offset.getEndToken());
        sb.append(delimiter);
        sb.append(offset.getBeginCharacter()).append("..").append(offset.getEndCharacter());
    }
}
