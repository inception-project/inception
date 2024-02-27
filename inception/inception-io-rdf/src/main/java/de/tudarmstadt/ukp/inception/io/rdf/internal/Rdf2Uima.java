/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.io.rdf.internal;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections4.iterators.IteratorIterable;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class Rdf2Uima
{
    public static void convert(Statement aContext, JCas aJCas) throws CASException
    {
        var m = aContext.getModel();

        // Set up names
        var tView = m.createResource(RdfCas.TYPE_VIEW);
        var tFeatureStructure = m.createResource(RdfCas.TYPE_FEATURE_STRUCTURE);
        var pIndexedIn = m.createProperty(RdfCas.PROP_INDEXED_IN);

        var fsIndex = new HashMap<Resource, FeatureStructure>();

        // Convert the views/SofAs
        var viewIndex = new HashMap<Resource, JCas>();
        var viewIter = m.listSubjectsWithProperty(RDF.type, tView);
        for (var view : new IteratorIterable<Resource>(viewIter)) {
            var viewJCas = convertView(view, aJCas);
            viewIndex.put(view, viewJCas);
            fsIndex.put(view, viewJCas.getSofa());
        }

        // Convert the FSes but without setting their feature values yet - we cannot fill
        // the feature values just set because some of them may point to FSes not yet created
        var fses = m.listSubjectsWithProperty(RDF.type, tFeatureStructure).toList();
        for (var fs : fses) {
            var uimaFS = initFS(fs.as(OntResource.class), aJCas);
            fsIndex.put(fs, uimaFS);
        }

        // Now fill the FSes with their feature values
        for (var fs : fses) {
            convertFS(fs.as(OntResource.class), aJCas, fsIndex);
        }

        // Finally add the FSes to the indexes of the respective views
        for (var fs : fses) {
            var indexedInIter = fs.listProperties(pIndexedIn);
            for (var indexedIn : new IteratorIterable<Statement>(indexedInIter)) {
                var viewJCas = viewIndex.get(indexedIn.getResource());
                viewJCas.addFsToIndexes(fsIndex.get(fs));
            }
        }
    }

    public static JCas convertView(Resource aView, JCas aJCas) throws CASException
    {
        var m = aView.getModel();

        // Set up names
        var pSofaID = m.createProperty(RdfCas.PROP_SOFA_ID);
        var pSofaString = m.createProperty(RdfCas.PROP_SOFA_STRING);
        var pSofaMimeType = m.createProperty(RdfCas.PROP_SOFA_MIME_TYPE);

        // Get the values
        var viewName = aView.getProperty(pSofaID).getString();
        var sofaString = aView.getProperty(pSofaString).getString();
        var sofaMimeType = aView.getProperty(pSofaMimeType).getString();

        // Instantiate the view/SofA
        var view = JCasUtil.getView(aJCas, viewName, true);
        view.setSofaDataString(sofaString, sofaMimeType);

        return view;
    }

    public static FeatureStructure initFS(OntResource aFS, JCas aJCas)
    {
        var cas = aJCas.getCas();

        // Figure out the UIMA type - there can be only one type per FS
        var types = aFS.listRDFTypes(true).toSet();
        types.removeIf(res -> res.getURI().startsWith(RdfCas.NS_RDFCAS));
        assert types.size() == 1;
        var type = CasUtil.getType(cas,
                types.iterator().next().getURI().substring(RdfCas.NS_UIMA.length()));

        FeatureStructure fs;
        if (type.getName().equals(DocumentMetaData.class.getName())) {
            // Special handling to avoid ending up with two document annotations in the CAS
            fs = DocumentMetaData.get(aJCas);
        }
        else {
            fs = cas.createFS(type);
        }

        return fs;
    }

    public static FeatureStructure convertFS(OntResource aFS, JCas aJCas,
            Map<Resource, FeatureStructure> aFsIndex)
    {
        var fs = aFsIndex.get(aFS);

        var stmtIter = aFS.listProperties();
        for (var stmt : new IteratorIterable<Statement>(stmtIter)) {
            // Skip all non-features
            if (!stmt.getPredicate().getURI().startsWith("uima:")) {
                continue;
            }

            var featureName = StringUtils.substringAfterLast(stmt.getPredicate().getURI(), "-");
            var uimaFeat = fs.getType().getFeatureByBaseName(featureName);

            // Cannot update start/end of document annotation because that FS is already indexed, so
            // we skip those
            if (fs == aJCas.getDocumentAnnotationFs()
                    && (CAS.FEATURE_BASE_NAME_BEGIN.equals(featureName)
                            || CAS.FEATURE_BASE_NAME_END.equals(featureName))) {
                continue;
            }

            if (uimaFeat.getRange().isPrimitive()) {
                switch (uimaFeat.getRange().getName()) {
                case CAS.TYPE_NAME_BOOLEAN:
                    fs.setBooleanValue(uimaFeat, stmt.getObject().asLiteral().getBoolean());
                    break;
                case CAS.TYPE_NAME_BYTE:
                    fs.setByteValue(uimaFeat, stmt.getObject().asLiteral().getByte());
                    break;
                case CAS.TYPE_NAME_DOUBLE:
                    fs.setDoubleValue(uimaFeat, stmt.getObject().asLiteral().getDouble());
                    break;
                case CAS.TYPE_NAME_FLOAT:
                    fs.setFloatValue(uimaFeat, stmt.getObject().asLiteral().getFloat());
                    break;
                case CAS.TYPE_NAME_INTEGER:
                    fs.setIntValue(uimaFeat, stmt.getObject().asLiteral().getInt());
                    break;
                case CAS.TYPE_NAME_LONG:
                    fs.setLongValue(uimaFeat, stmt.getObject().asLiteral().getLong());
                    break;
                case CAS.TYPE_NAME_SHORT:
                    fs.setShortValue(uimaFeat, stmt.getObject().asLiteral().getShort());
                    break;
                case CAS.TYPE_NAME_STRING: {
                    if (stmt.getObject().isLiteral()) {
                        fs.setStringValue(uimaFeat, stmt.getObject().asLiteral().getString());
                    }
                    else {
                       fs.setStringValue(uimaFeat, stmt.getObject().asResource().getURI());
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException(
                            "Feature [" + uimaFeat.getName() + "] has unsupported primitive type ["
                                    + uimaFeat.getRange().getName() + "]");
                }
            }
            else {
                FeatureStructure targetUimaFS = aFsIndex.get(stmt.getObject().asResource());
                if (targetUimaFS == null) {
                    throw new IllegalStateException("No UIMA FS found for ["
                            + stmt.getObject().asResource().getURI() + "]");
                }
                fs.setFeatureValue(uimaFeat, targetUimaFS);
            }
        }

        return fs;
    }
}
