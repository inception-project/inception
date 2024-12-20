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
package de.tudarmstadt.ukp.inception.support.uima;

import static java.io.ObjectInputFilter.Config.createFilter;
import static java.lang.String.join;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_BEGIN;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_END;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_LANGUAGE;
import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovering;
import static org.apache.uima.fit.util.CasUtil.selectSingle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Contain Methods for updating CAS Objects directed from brat UI, different utility methods to
 * process the CAS such getting the sentence address, determine page numbers,...
 */
public class WebAnnoCasUtil
{
    private static final String PROP_ENFORCE_CAS_THREAD_LOCK = "webanno.debug.enforce_cas_thread_lock";

    private static final boolean ENFORCE_CAS_THREAD_LOCK = System
            .getProperty(PROP_ENFORCE_CAS_THREAD_LOCK, "true").equals("true");

    private final static ObjectInputFilter SERIALIZED_CAS_INPUT_FILTER = createFilter(join(";", //
            CASCompleteSerializer.class.getName(), //
            CASSerializer.class.getName(), //
            CASMgrSerializer.class.getName(), //
            String.class.getName(), //
            "!*"));

    public static CAS createCas(TypeSystemDescription aTSD) throws ResourceInitializationException
    {
        CAS cas = CasCreationUtils.createCas(aTSD, null, null);

        if (ENFORCE_CAS_THREAD_LOCK) {
            cas = (CAS) Proxy.newProxyInstance(cas.getClass().getClassLoader(),
                    new Class[] { CAS.class }, new ThreadLockingInvocationHandler(cas));
        }

        return cas;
    }

    public static CAS createCas() throws ResourceInitializationException
    {
        return createCas(null);
    }

    /**
     * Creates a copy of the given CAS.
     * 
     * @param aOriginal
     *            the original CAS
     * @return the copy
     * @throws UIMAException
     *             if there was a problem preparing the copy.
     */
    public static CAS createCasCopy(CAS aOriginal) throws UIMAException
    {
        var copy = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
        var serializer = serializeCASComplete((CASImpl) getRealCas(aOriginal));
        deserializeCASComplete(serializer, (CASImpl) getRealCas(copy));
        return copy;
    }

    public static CAS getRealCas(CAS aCas)
    {
        if (!ENFORCE_CAS_THREAD_LOCK) {
            return aCas;
        }

        if (!Proxy.isProxyClass(aCas.getClass())) {
            return aCas;
        }

        ThreadLockingInvocationHandler handler = (ThreadLockingInvocationHandler) Proxy
                .getInvocationHandler(aCas);
        return (CAS) handler.getTarget();
    }

    public static void transferCasOwnershipToCurrentThread(CAS aCas)
    {
        if (!ENFORCE_CAS_THREAD_LOCK) {
            return;
        }

        if (!Proxy.isProxyClass(aCas.getClass())) {
            return;
        }

        ThreadLockingInvocationHandler handler = (ThreadLockingInvocationHandler) Proxy
                .getInvocationHandler(aCas);
        handler.transferOwnershipToCurrentThread();
    }

    /**
     * Annotation a and annotation b are the same if they have the same address.
     *
     * @param a
     *            a FS.
     * @param b
     *            a FS.
     * @return if both FSes are the same.
     */
    public static boolean isSame(FeatureStructure a, FeatureStructure b)
    {
        if (a == null || b == null) {
            return false;
        }

        if (a.getCAS() != b.getCAS()) {
            return false;
        }

        return ICasUtil.getAddr(a) == ICasUtil.getAddr(b);
    }

    /**
     * Check if the two given begin offsets are within the same sentence. If the second offset is at
     * the end of the sentence, it is no longer considered to be part of the sentence. Mind that
     * annotations in UIMA are half-open intervals <code>[begin,end)</code>. If there is no sentence
     * covering the offsets, the method returns <code>false</code>.
     *
     * @param aCas
     *            the CAS.
     * @param aBegin1
     *            the reference offset.
     * @param aBegin2
     *            the comparison offset.
     * @return if the two offsets are within the same sentence.
     */
    public static boolean isBeginInSameSentence(CAS aCas, int aBegin1, int aBegin2)
    {
        return selectCovering(aCas, getType(aCas, Sentence.class), aBegin1, aBegin1).stream()
                .filter(s -> s.getBegin() <= aBegin1 && aBegin1 < s.getEnd())
                .filter(s -> s.getBegin() <= aBegin2 && aBegin2 < s.getEnd()).findFirst()
                .isPresent();
    }

    /**
     * Check if the begin/end offsets are within the same sentence. If the end offset is at the end
     * of the sentence, it is considered to be part of the sentence. Mind that annotations in UIMA
     * are half-open intervals <code>[begin,end)</code>. If there is no sentence covering the
     * offsets, the method returns <code>false</code>.
     *
     * @param aCas
     *            the CAS.
     * @param aBegin
     *            the reference offset.
     * @param aEnd
     *            the comparison offset.
     * @return if the two offsets are within the same sentence.
     */
    public static boolean isBeginEndInSameSentence(CAS aCas, int aBegin, int aEnd)
    {
        var sentenceIndex = aCas.getAnnotationIndex(getType(aCas, Sentence.class));

        if (sentenceIndex.isEmpty()) {
            throw new IllegalArgumentException("Unable to check if start and end offsets are in "
                    + "the same sentence because the CAS contains no sentences!");
        }

        return StreamSupport.stream(sentenceIndex.spliterator(), false) //
                .filter(s -> s.getBegin() <= aBegin && aBegin < s.getEnd()) //
                .filter(s -> s.getBegin() <= aEnd && aEnd <= s.getEnd()) //
                .findFirst().isPresent();
    }

    public static AnnotationFS createToken(CAS aCas, int aBegin, int aEnd)
    {
        return aCas.createAnnotation(getType(aCas, Token.class), aBegin, aEnd);
    }

    public static AnnotationFS createSentence(CAS aCas, int aBegin, int aEnd)
    {
        return aCas.createAnnotation(getType(aCas, Sentence.class), aBegin, aEnd);
    }

    public static boolean exists(CAS aCas, Type aType)
    {
        return !aCas.select(aType).isEmpty();
    }

    /**
     * Get overlapping annotations where selection overlaps with annotations.<br>
     * Example: if annotation is (5, 13) and selection covered was from (7, 12); the annotation (5,
     * 13) is returned as overlapped selection <br>
     * If multiple annotations are [(3, 8), (9, 15), (16, 21)] and selection covered was from (10,
     * 18), overlapped annotation [(9, 15), (16, 21)] should be returned
     *
     * @param aCas
     *            a CAS containing the annotation.
     * @param aType
     *            a UIMA type.
     * @param aBegin
     *            begin offset.
     * @param aEnd
     *            end offset.
     * @return a return value.
     */
    public static List<AnnotationFS> selectOverlapping(CAS aCas, Type aType, int aBegin, int aEnd)
    {
        var annotations = new ArrayList<AnnotationFS>();
        for (var t : select(aCas, aType)) {
            if (t.getBegin() >= aEnd) {
                break;
            }
            // not yet there
            if (t.getEnd() <= aBegin) {
                continue;
            }
            annotations.add(t);
        }

        return annotations;
    }

    @SuppressWarnings("unchecked")
    public static <T extends AnnotationFS> T getNext(T aRef)
    {
        var cas = aRef.getCAS();
        var idx = cas.getAnnotationIndex(aRef.getType());
        var it = idx.iterator(aRef);

        if (!it.isValid()) {
            return null;
        }

        // If the first annotation we hit is already the reference annotation, we can simply
        // move on to the next one and are done.
        if (it.get() == aRef) {
            it.moveToNext();
            return it.isValid() ? (T) it.get() : null;
        }

        // Seek left until we hit the last FS that is no longer equal to the current
        var moved = false;
        while (it.isValid() && idx.compare(it.get(), aRef) == 0) {
            it.moveToPrevious();
            moved = true;
        }

        if (moved) {
            it.moveToNext();
        }

        while (it.isValid() && idx.compare(it.get(), aRef) == 0) {
            if (it.get() == aRef) {
                it.moveToNext();
                return it.isValid() ? (T) it.get() : null;
            }
            it.moveToNext();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T extends AnnotationFS> T getPrev(T aRef)
    {
        CAS cas = aRef.getCAS();
        AnnotationIndex<AnnotationFS> idx = cas.getAnnotationIndex(aRef.getType());
        FSIterator<AnnotationFS> it = idx.iterator(aRef);

        if (!it.isValid()) {
            return null;
        }

        // If the first annotation we hit is already the reference annotation, we can simply
        // move on to the previous one and are done.
        if (it.get() == aRef) {
            it.moveToPrevious();
            return it.isValid() ? (T) it.get() : null;
        }

        // Seek left until we hit the last FS that is no longer equal to the current
        boolean moved = false;
        while (it.isValid() && idx.compare(it.get(), aRef) == 0) {
            it.moveToPrevious();
            moved = true;
        }

        if (moved) {
            it.moveToNext();
        }

        while (it.isValid() && idx.compare(it.get(), aRef) == 0) {
            if (it.get() == aRef) {
                it.moveToPrevious();
                return it.isValid() ? (T) it.get() : null;
            }
            it.moveToNext();
        }

        return null;
    }

    /**
     * Get the sentence number at this specific position
     *
     * @param aCas
     *            the CAS.
     * @param aBeginOffset
     *            the begin offset.
     * @return the sentence number.
     */
    public static int getSentenceNumber(CAS aCas, int aBeginOffset)
    {
        int sentenceNumber = 0;

        Type sentenceType = getType(aCas, Sentence.class);
        Collection<AnnotationFS> sentences = select(aCas, sentenceType);
        if (sentences.isEmpty()) {
            throw new IndexOutOfBoundsException("No sentences");
        }

        for (AnnotationFS sentence : select(aCas, sentenceType)) {
            if (sentence.getBegin() <= aBeginOffset && aBeginOffset <= sentence.getEnd()) {
                sentenceNumber++;
                break;
            }
            sentenceNumber++;
        }
        return sentenceNumber;
    }

    public static Collection<AnnotationFS> selectSentences(CAS aCas)
    {
        return CasUtil.select(aCas, getType(aCas, Sentence.class));
    }

    public static Collection<AnnotationFS> selectTokens(CAS aCas)
    {
        return CasUtil.select(aCas, getType(aCas, Token.class));
    }

    public static boolean isNativeUimaType(String aType)
    {
        Validate.notNull(aType, "Type must not be null");

        switch (aType) {
        case CAS.TYPE_NAME_ANNOTATION:
        case CAS.TYPE_NAME_ANNOTATION_BASE:
        case CAS.TYPE_NAME_ARRAY_BASE:
        case CAS.TYPE_NAME_BOOLEAN:
        case CAS.TYPE_NAME_BOOLEAN_ARRAY:
        case CAS.TYPE_NAME_BYTE:
        case CAS.TYPE_NAME_BYTE_ARRAY:
        case CAS.TYPE_NAME_DOCUMENT_ANNOTATION:
        case CAS.TYPE_NAME_DOUBLE:
        case CAS.TYPE_NAME_DOUBLE_ARRAY:
        case CAS.TYPE_NAME_EMPTY_FLOAT_LIST:
        case CAS.TYPE_NAME_EMPTY_FS_LIST:
        case CAS.TYPE_NAME_EMPTY_INTEGER_LIST:
        case CAS.TYPE_NAME_EMPTY_STRING_LIST:
        case CAS.TYPE_NAME_FLOAT:
        case CAS.TYPE_NAME_FLOAT_ARRAY:
        case CAS.TYPE_NAME_FLOAT_LIST:
        case CAS.TYPE_NAME_FS_ARRAY:
        case CAS.TYPE_NAME_FS_LIST:
        case CAS.TYPE_NAME_INTEGER:
        case CAS.TYPE_NAME_INTEGER_ARRAY:
        case CAS.TYPE_NAME_INTEGER_LIST:
        case CAS.TYPE_NAME_LIST_BASE:
        case CAS.TYPE_NAME_LONG:
        case CAS.TYPE_NAME_LONG_ARRAY:
        case CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST:
        case CAS.TYPE_NAME_NON_EMPTY_FS_LIST:
        case CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST:
        case CAS.TYPE_NAME_NON_EMPTY_STRING_LIST:
        case CAS.TYPE_NAME_SHORT:
        case CAS.TYPE_NAME_SHORT_ARRAY:
        case CAS.TYPE_NAME_SOFA:
        case CAS.TYPE_NAME_STRING:
        case CAS.TYPE_NAME_STRING_ARRAY:
        case CAS.TYPE_NAME_STRING_LIST:
        case CAS.TYPE_NAME_TOP:
            return true;
        }

        return false;
    }

    public static boolean isPrimitiveType(Type aType)
    {
        switch (aType.getName()) {
        case CAS.TYPE_NAME_STRING: // fallthrough
        case CAS.TYPE_NAME_BOOLEAN: // fallthrough
        case CAS.TYPE_NAME_FLOAT: // fallthrough
        case CAS.TYPE_NAME_INTEGER:
            return true;
        default:
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFeature(FeatureStructure aFS, String aFeatureName)
    {
        Feature feature = aFS.getType().getFeatureByBaseName(aFeatureName);

        if (feature == null) {
            throw new IllegalArgumentException("Type [" + aFS.getType().getName()
                    + "] has no feature called [" + aFeatureName + "]");
        }

        switch (feature.getRange().getName()) {
        case CAS.TYPE_NAME_STRING:
            return (T) aFS.getStringValue(feature);
        case CAS.TYPE_NAME_BOOLEAN:
            return (T) (Boolean) aFS.getBooleanValue(feature);
        case CAS.TYPE_NAME_FLOAT:
            return (T) (Float) aFS.getFloatValue(feature);
        case CAS.TYPE_NAME_INTEGER:
            return (T) (Integer) aFS.getIntValue(feature);
        default:
            throw new IllegalArgumentException("Cannot get value of feature [" + feature.getName()
                    + "] with type [" + feature.getRange().getName() + "]");
        }
    }

    public static FeatureStructure createDocumentMetadata(CAS aCas)
    {
        Type type = getType(aCas, DocumentMetaData.class);
        FeatureStructure dmd;
        if (aCas.getDocumentText() != null) {
            dmd = aCas.createAnnotation(type, 0, aCas.getDocumentText().length());
        }
        else {
            dmd = aCas.createAnnotation(type, 0, 0);
        }

        // If there is already a DocumentAnnotation copy it's information and delete it
        FeatureStructure da = aCas.getDocumentAnnotation();
        if (da != null) {
            FSUtil.setFeature(dmd, FEATURE_BASE_NAME_LANGUAGE,
                    FSUtil.getFeature(da, FEATURE_BASE_NAME_LANGUAGE, String.class));
            FSUtil.setFeature(dmd, FEATURE_BASE_NAME_BEGIN,
                    FSUtil.getFeature(da, FEATURE_BASE_NAME_BEGIN, Integer.class));
            FSUtil.setFeature(dmd, FEATURE_BASE_NAME_END,
                    FSUtil.getFeature(da, FEATURE_BASE_NAME_END, Integer.class));
            aCas.removeFsFromIndexes(da);
        }
        else if (aCas.getDocumentText() != null) {
            FSUtil.setFeature(dmd, FEATURE_BASE_NAME_BEGIN, 0);
            FSUtil.setFeature(dmd, FEATURE_BASE_NAME_END, aCas.getDocumentText().length());
        }
        aCas.addFsToIndexes(dmd);
        return dmd;
    }

    public static FeatureStructure getDocumentMetadata(CAS aCas)
    {
        Type type = getType(aCas, DocumentMetaData.class);
        FeatureStructure dmd;
        try {
            dmd = selectSingle(aCas, type);
        }
        catch (IllegalArgumentException e) {
            dmd = createDocumentMetadata(aCas);
        }

        return dmd;
    }

    public static void copyDocumentMetadata(CAS aSourceView, CAS aTargetView)
    {
        // First get the DMD then create. In case the get fails, we do not create.
        FeatureStructure dmd = getDocumentMetadata(aSourceView);
        FeatureStructure docMetaData = createDocumentMetadata(aTargetView);
        FSUtil.setFeature(docMetaData, "collectionId",
                FSUtil.getFeature(dmd, "collectionId", String.class));
        FSUtil.setFeature(docMetaData, "documentBaseUri",
                FSUtil.getFeature(dmd, "documentBaseUri", String.class));
        FSUtil.setFeature(docMetaData, "documentId",
                FSUtil.getFeature(dmd, "documentId", String.class));
        FSUtil.setFeature(docMetaData, "documentTitle",
                FSUtil.getFeature(dmd, "documentTitle", String.class));
        FSUtil.setFeature(docMetaData, "documentUri",
                FSUtil.getFeature(dmd, "documentUri", String.class));
        FSUtil.setFeature(docMetaData, "isLastSegment",
                FSUtil.getFeature(dmd, "isLastSegment", Boolean.class));
        FSUtil.setFeature(docMetaData, CAS.FEATURE_BASE_NAME_LANGUAGE,
                FSUtil.getFeature(dmd, CAS.FEATURE_BASE_NAME_LANGUAGE, String.class));
    }

    public static void setDocumentId(CAS aCas, String aID)
    {
        FeatureStructure dmd = getDocumentMetadata(aCas);
        FSUtil.setFeature(dmd, "documentId", aID);
    }

    public static String getDocumentId(CAS aCas)
    {
        try {
            Type type = getType(aCas, DocumentMetaData.class);
            FeatureStructure dmd = selectSingle(aCas, type);
            return FSUtil.getFeature(dmd, "documentId", String.class);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String getDocumentUri(CAS aCas)
    {
        try {
            Type type = getType(aCas, DocumentMetaData.class);
            FeatureStructure dmd = selectSingle(aCas, type);
            return FSUtil.getFeature(dmd, "documentUri", String.class);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String getDocumentTitle(CAS aCas)
    {
        try {
            Type type = getType(aCas, DocumentMetaData.class);
            FeatureStructure dmd = selectSingle(aCas, type);
            return FSUtil.getFeature(dmd, "documentTitle", String.class);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static byte[] casToByteArray(CASCompleteSerializer aSer) throws IOException
    {
        try (var bos = new ByteArrayOutputStream()) {
            try (var oos = new ObjectOutputStream(bos)) {
                oos.writeObject(aSer);
            }
            return bos.toByteArray();
        }
    }

    public static byte[] casToByteArray(CAS aCas) throws IOException
    {
        // Index annotation document
        var realCas = (CASImpl) getRealCas(aCas);
        // UIMA-6162 Workaround: synchronize CAS during de/serialization
        synchronized (realCas.getBaseCAS()) {
            return casToByteArray(serializeCASComplete(realCas));
        }
    }

    public static CAS byteArrayToCas(byte[] aByteArray) throws IOException
    {
        CAS cas;
        try {
            cas = createCas();
        }
        catch (ResourceInitializationException e) {
            throw new IOException(e);
        }

        var realCas = (CASImpl) getRealCas(cas);
        synchronized (realCas.getBaseCAS()) {
            try (var ois = new ObjectInputStream(new ByteArrayInputStream(aByteArray))) {
                ois.setObjectInputFilter(SERIALIZED_CAS_INPUT_FILTER);
                var casCompleteSerializer = (CASCompleteSerializer) ois.readObject();
                realCas.reinit(casCompleteSerializer);
            }
            catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        }

        return cas;
    }
}
