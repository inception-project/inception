/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.uima.cas.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.uima.UimaSerializable;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.internal.util.Int2ObjHashMap;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Workaround UIMA bug while UIMA 3.4.1 has not yet been released.
 * 
 * @see "https://github.com/inception-project/inception/issues/3799"
 * @see "https://github.com/apache/uima-uimaj/issues/304"
 */
public class FixedBinaryCasSerDes
    extends BinaryCasSerDes
{
    private static final boolean SOFA_IN_NORMAL_ORDER = false;
    private static final int arrayLengthFeatOffset = 1;
    private static final int arrayContentOffset = 2;

    private static Field baseCasField = FieldUtils.getDeclaredField(BinaryCasSerDes.class,
            "baseCas", true);
    private static Field tsiField = FieldUtils.getDeclaredField(BinaryCasSerDes.class, "tsi", true);
    private static Method updateStringFeatureMethod = MethodUtils.getMatchingMethod(
            BinaryCasSerDes.class, "updateStringFeature", TOP.class, FeatureImpl.class,
            String.class, List.class);
    private static Method getSofaFromAnnotBaseMethod = MethodUtils.getMatchingMethod(
            BinaryCasSerDes.class, "getSofaFromAnnotBase", int.class, StringHeap.class,
            Int2ObjHashMap.class, CommonSerDesSequential.class);
    private static Method heapFeatMethod = MethodUtils.getMatchingMethod(BinaryCasSerDes.class,
            "heapFeat", int.class, FeatureImpl.class);
    private static Method makeSofaFromHeapMethod = MethodUtils.getMatchingMethod(
            BinaryCasSerDes.class, "makeSofaFromHeap", int.class, StringHeap.class,
            CommonSerDesSequential.class, boolean.class);
    private static Method setFeatOrDeferMethod = MethodUtils.getMatchingMethod(
            BinaryCasSerDes.class, "setFeatOrDefer", int.class, FeatureImpl.class, List.class,
            Consumer.class, Int2ObjHashMap.class);

    static {
        updateStringFeatureMethod.setAccessible(true);
        getSofaFromAnnotBaseMethod.setAccessible(true);
        heapFeatMethod.setAccessible(true);
        makeSofaFromHeapMethod.setAccessible(true);
        setFeatOrDeferMethod.setAccessible(true);
    }

    public static void inject(CAS aCas)
    {
        CASImpl casImpl = (CASImpl) aCas;
        CASImpl baseCas = casImpl.getBaseCAS();
        try {
            FieldUtils.writeDeclaredField(casImpl.svd, "bcsd", new FixedBinaryCasSerDes(baseCas),
                    true);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public FixedBinaryCasSerDes(CASImpl aBaseCAS)
    {
        super(aBaseCAS);
    }

    @Override
    void reinit(int[] heapMetadata, int[] heapArray, String[] stringTable, int[] fsIndex,
            byte[] byteHeapArray, short[] shortHeapArray, long[] longHeapArray)
    {
        CASImpl baseCas = getBaseCas();

        CommonSerDesSequential csds = new CommonSerDesSequential(baseCas); // for non Delta case,
                                                                           // not
                                                                           // held on to
        // compare with compress form 4, which does cas.getCsds() or cas.newCsds() which saves it in
        // cas.svd
        csds.setup(null, 1);
        heap = new Heap();
        byteHeap = new ByteHeap();
        shortHeap = new ShortHeap();
        longHeap = new LongHeap();
        stringHeap = new StringHeap();

        createStringTableFromArray(stringTable);

        heap.reinit(heapMetadata, heapArray);
        if (byteHeapArray != null) {
            byteHeap.reinit(byteHeapArray);
        }
        if (shortHeapArray != null) {
            shortHeap.reinit(shortHeapArray);
        }
        if (longHeapArray != null) {
            longHeap.reinit(longHeapArray);
        }

        createFSsFromHeaps(false, 1, csds); // false means not delta

        reinitIndexedFSs(fsIndex, false, i -> csds.addr2fs.get(i));
    }

    private void createFSsFromHeaps(boolean isDelta, int startPos, CommonSerDesSequential csds)
    {
        CASImpl baseCas = getBaseCas();

        final int heapsz = heap.getCellsUsed();
        final Int2ObjHashMap<TOP, TOP> addr2fs = csds.addr2fs;
        TypeSystemImpl tsi = baseCas.getTypeSystemImpl();
        setTsi(tsi);
        TOP fs;
        TypeImpl type;
        CASImpl initialView = baseCas.getInitialView(); // creates if needed

        List<Runnable> fixups4forwardFsRefs = new ArrayList<>();
        List<Runnable> fixups4UimaSerialization = new ArrayList<>();

        for (int heapIndex = startPos; heapIndex < heapsz; heapIndex += getFsSpaceReq(fs, type)) {
            int typecode = heap.heap[heapIndex];
            // if (isBeforeV3 && typecode > TypeSystemConstants.lastBuiltinV2TypeCode) {
            // typecode = typecode + TypeSystemConstants.numberOfNewBuiltInsSinceV2;
            // }
            type = tsi.getTypeForCode(heap.heap[heapIndex]);
            if (type == null) {
                throw new CASRuntimeException(CASRuntimeException.deserialized_type_not_found,
                        heap.heap[heapIndex]);
            }
            if (type.isArray()) {
                final int len = heap.heap[heapIndex + arrayLengthFeatOffset];

                fs = baseCas.createArray(type, len);
                csds.addFS(fs, heapIndex);

                if (len > 0) {
                    final int bhi = heap.heap[heapIndex + arrayContentOffset];
                    final int hhi = heapIndex + arrayContentOffset;

                    switch (type.getComponentSlotKind()) {

                    case Slot_BooleanRef: {
                        boolean[] ba = ((BooleanArray) fs)._getTheArray();
                        for (int ai = 0; ai < len; ai++) {
                            ba[ai] = byteHeap.heap[bhi + ai] == (byte) 1;
                        }
                        break;
                    }

                    case Slot_ByteRef:
                        System.arraycopy(byteHeap.heap, bhi, ((ByteArray) fs)._getTheArray(), 0,
                                len);
                        break;

                    case Slot_ShortRef:
                        System.arraycopy(shortHeap.heap, bhi, ((ShortArray) fs)._getTheArray(), 0,
                                len);
                        break;

                    case Slot_LongRef:
                        System.arraycopy(longHeap.heap, bhi, ((LongArray) fs)._getTheArray(), 0,
                                len);
                        break;

                    case Slot_DoubleRef: {
                        double[] da = ((DoubleArray) fs)._getTheArray();
                        for (int ai = 0; ai < len; ai++) {
                            da[ai] = CASImpl.long2double(longHeap.heap[bhi + ai]);
                        }
                        break;
                    }

                    case Slot_Int:
                        System.arraycopy(heap.heap, hhi, ((IntegerArray) fs)._getTheArray(), 0,
                                len);
                        break;

                    case Slot_Float: {
                        float[] fa = ((FloatArray) fs)._getTheArray();
                        for (int ai = 0; ai < len; ai++) {
                            fa[ai] = CASImpl.int2float(heap.heap[hhi + ai]);
                        }
                        break;
                    }

                    case Slot_StrRef: {
                        String[] sa = ((StringArray) fs)._getTheArray();
                        for (int ai = 0; ai < len; ai++) {
                            sa[ai] = stringHeap.getStringForCode(heap.heap[hhi + ai]);
                        }
                        break;
                    }

                    case Slot_HeapRef: {
                        TOP[] fsa = ((FSArray) fs)._getTheArray();
                        for (int ai = 0; ai < len; ai++) {
                            int a = heap.heap[hhi + ai];
                            if (a == 0) {
                                continue;
                            }
                            TOP item = addr2fs.get(a);
                            if (item != null) {
                                fsa[ai] = item;
                            }
                            else {
                                final int aiSaved = ai;
                                final int addrSaved = a;
                                fixups4forwardFsRefs.add(() -> {
                                    fsa[aiSaved] = addr2fs.get(addrSaved);
                                });
                            }
                        }
                        break;
                    }

                    default:
                        Misc.internalError();

                    } // end of switch
                }
            }
            else { // end of arrays
                   // start of normal non-array
                CASImpl view = null;
                boolean isSofa = false;
                boolean documentAnnotationPreviouslyIndexed = false;
                if (type.isAnnotationBaseType()) {
                    Sofa sofa = _getSofaFromAnnotBase(heapIndex, stringHeap, addr2fs, csds); // creates
                                                                                             // sofa
                                                                                             // if
                                                                                             // needed
                                                                                             // and
                                                                                             // exists
                                                                                             // (forward
                                                                                             // ref
                                                                                             // case)
                    view = (sofa == null) ? baseCas.getInitialView() : baseCas.getView(sofa);
                    if (type == tsi.docType) {
                        Annotation documentAnnotationPrevious = view
                                .getDocumentAnnotationNoCreate();
                        if (documentAnnotationPrevious == null) {
                            // document annotation not present
                            fs = view.createDocumentAnnotationNoRemoveNoIndex(0); // create but
                                                                                  // don't index
                            view.set_deserialized_doc_annot_not_indexed((Annotation) fs); // for use
                                                                                          // by
                                                                                          // other
                                                                                          // code
                                                                                          // that
                                                                                          // sets
                                                                                          // length,
                                                                                          // if
                                                                                          // this is
                                                                                          // not
                                                                                          // indexed
                            // documentAnnotationPreviouslyIndex == false, preset above
                        }
                        else {
                            fs = documentAnnotationPrevious;
                            // remove from Corruptable indexes, because we'll be updating it.
                            view.removeFromCorruptableIndexAnyView(fs, view.getAddbackSingle());
                            documentAnnotationPreviouslyIndexed = true;
                        }
                    }
                    else {
                        fs = view.createFS(type);
                        if (fs instanceof UimaSerializable) {
                            final UimaSerializable ufs = (UimaSerializable) fs;
                            fixups4UimaSerialization.add(() -> ufs._init_from_cas_data());
                        }
                    }
                }
                else if (type == tsi.sofaType) {
                    fs = _makeSofaFromHeap(heapIndex, stringHeap, csds, SOFA_IN_NORMAL_ORDER); // creates
                                                                                               // Sofa
                                                                                               // if
                                                                                               // not
                                                                                               // already
                                                                                               // created
                                                                                               // due
                                                                                               // to
                                                                                               // annotationbase
                                                                                               // code
                                                                                               // above
                    isSofa = true;
                }
                else {
                    fs = initialView.createFS(type);
                    if (fs instanceof UimaSerializable) {
                        final UimaSerializable ufs = (UimaSerializable) fs;
                        fixups4UimaSerialization.add(() -> ufs._init_from_cas_data());
                    }
                }
                if (!isSofa) { // if it was a sofa, other code added or pended it
                    csds.addFS(fs, heapIndex);
                }

                for (final FeatureImpl feat : type.getFeatureImpls()) {
                    SlotKind slotKind = feat.getSlotKind();
                    switch (slotKind) {
                    case Slot_Boolean:
                    case Slot_Byte:
                    case Slot_Short:
                    case Slot_Int:
                    case Slot_Float:
                        if (!isSofa || feat != tsi.sofaNum) {
                            fs._setIntLikeValueNcNj(slotKind, feat, _heapFeat(heapIndex, feat));
                        }
                        break;

                    case Slot_LongRef:
                        fs._setLongValueNcNj(feat, longHeap.heap[_heapFeat(heapIndex, feat)]);
                        break;
                    case Slot_DoubleRef:
                        fs._setDoubleValueNcNj(feat,
                                CASImpl.long2double(longHeap.heap[_heapFeat(heapIndex, feat)]));
                        break;
                    case Slot_StrRef: {
                        String s = stringHeap.getStringForCode(_heapFeat(heapIndex, feat));
                        if (_updateStringFeature(fs, feat, s, fixups4forwardFsRefs)) {
                            fs._setStringValueNcNj(feat, s);
                        }
                        break;
                    }

                    case Slot_HeapRef: {
                        final TOP finalFs = fs;
                        if (feat == tsi.annotBaseSofaFeat) {
                            break; // already set
                        }
                        _setFeatOrDefer(heapIndex, feat, fixups4forwardFsRefs, item -> {
                            if (feat == tsi.sofaArray) {
                                ((Sofa) finalFs).setLocalSofaData(item);
                            }
                            else {
                                finalFs._setFeatureValueNcNj(feat, item);
                            }
                        }, addr2fs);
                        break;
                    }

                    default:
                        Misc.internalError();
                    } // end of switch
                } // end of for-loop-over-all-features

                if (type == tsi.docType && documentAnnotationPreviouslyIndexed) {
                    view.addbackSingle(fs);
                }
            } // end of non-array, normal fs
        } // end of loop over all fs in main array

        for (Runnable r : fixups4forwardFsRefs) {
            r.run();
        }

        for (Runnable r : fixups4UimaSerialization) {
            r.run();
        }
    }

    private void _setFeatOrDefer(int aHeapIndex, FeatureImpl aFeat,
            List<Runnable> aFixups4forwardFsRefs, Consumer<TOP> aSetter,
            Int2ObjHashMap<TOP, TOP> aAddr2fs)
    {
        try {
            setFeatOrDeferMethod.invoke(this, aHeapIndex, aFeat, aFixups4forwardFsRefs, aSetter,
                    aAddr2fs);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean _updateStringFeature(TOP aFs, FeatureImpl aFeat, String aS,
            List<Runnable> aFixups4forwardFsRefs)
    {
        try {
            return (boolean) updateStringFeatureMethod.invoke(this, aFs, aFeat, aS,
                    aFixups4forwardFsRefs);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int _heapFeat(int aHeapIndex, FeatureImpl aFeat)
    {
        try {
            return (int) heapFeatMethod.invoke(this, aHeapIndex, aFeat);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TOP _makeSofaFromHeap(int aHeapIndex, StringHeap aStringHeap,
            CommonSerDesSequential aCsds, boolean aSofaInNormalOrder)
    {
        try {
            return (TOP) makeSofaFromHeapMethod.invoke(this, aHeapIndex, aStringHeap, aCsds,
                    aSofaInNormalOrder);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Sofa _getSofaFromAnnotBase(int annotBaseAddr, StringHeap stringHeap2,
            Int2ObjHashMap<TOP, TOP> addr2fs, CommonSerDesSequential csds)
    {
        try {
            return (Sofa) getSofaFromAnnotBaseMethod.invoke(this, annotBaseAddr, stringHeap2,
                    addr2fs, csds);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setTsi(TypeSystemImpl aTsi)
    {
        try {
            FieldUtils.writeField(tsiField, this, aTsi, true);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private CASImpl getBaseCas()
    {
        CASImpl baseCas;
        try {
            baseCas = (CASImpl) FieldUtils.readField(baseCasField, this, true);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return baseCas;
    }
}
