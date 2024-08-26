package mtas.codec.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.index.Terms;
import org.apache.lucene.store.IndexInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtas.analysis.token.MtasToken;
import mtas.analysis.token.MtasTokenString;
import mtas.codec.MtasCodecPostingsFormat;
import mtas.codec.tree.IntervalRBTree;
import mtas.codec.tree.IntervalTreeNodeData;
import mtas.codec.util.CodecSearchTree.MtasTreeHit;

/**
 * The Class CodecInfo.
 */
public class CodecInfo
{

    /** The log. */
    private static final Logger log = LoggerFactory.getLogger(CodecInfo.class);

    /** The index input list. */
    HashMap<String, IndexInput> indexInputList;

    /** The index input offset list. */
    HashMap<String, Long> indexInputOffsetList;

    /** The version. */
    int version;

    /** The field references. */
    private HashMap<String, FieldReferences> fieldReferences;

    /** The prefix references. */
    private HashMap<String, LinkedHashMap<String, Long>> prefixReferences;

    /**
     * Instantiates a new codec info.
     *
     * @param indexInputList
     *            the index input list
     * @param indexInputOffsetList
     *            the index input offset list
     * @param version
     *            the version
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public CodecInfo(HashMap<String, IndexInput> indexInputList,
            HashMap<String, Long> indexInputOffsetList, int version)
        throws IOException
    {
        this.indexInputList = indexInputList;
        this.indexInputOffsetList = indexInputOffsetList;
        this.version = version;
        init();
    }

    /**
     * Gets the codec info from terms.
     *
     * @param t
     *            the t
     * @return the codec info from terms
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    public static CodecInfo getCodecInfoFromTerms(Terms t) throws IOException
    {
        try {
            HashMap<String, IndexInput> indexInputList = null;
            HashMap<String, Long> indexInputOffsetList = null;
            Object version = null;
            Method[] methods = t.getClass().getMethods();
            Object[] emptyArgs = null;
            for (Method m : methods) {
                if (m.getName().equals("getIndexInputList")) {
                    indexInputList = (HashMap<String, IndexInput>) m.invoke(t, emptyArgs);
                }
                else if (m.getName().equals("getIndexInputOffsetList")) {
                    indexInputOffsetList = (HashMap<String, Long>) m.invoke(t, emptyArgs);
                }
                else if (m.getName().equals("getVersion")) {
                    version = m.invoke(t, emptyArgs);
                }
            }
            if (indexInputList == null || indexInputOffsetList == null || version == null) {
                throw new IOException("Reader doesn't provide MtasFieldsProducer");
            }
            else {
                return new CodecInfo(indexInputList, indexInputOffsetList, (int) version);
            }
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Can't get codecInfo", e);
        }
    }

    /**
     * Inits the.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void init() throws IOException
    {
        // move to begin
        IndexInput inField = indexInputList.get("field");
        inField.seek(indexInputOffsetList.get("field"));
        // store field references in memory
        fieldReferences = new HashMap<String, FieldReferences>();
        boolean doInit = true;
        while (doInit) {
            try {
                String field = inField.readString();
                long refIndexDoc = inField.readVLong();
                long refIndexDocId = inField.readVLong();
                int numberOfDocs = inField.readVInt();
                inField.readVLong(); // refTerm
                inField.readVInt(); // numberOfTerms
                long refPrefix = inField.readVLong();
                int numberOfPrefixes = inField.readVInt();
                fieldReferences.put(field, new FieldReferences(refIndexDoc, refIndexDocId,
                        numberOfDocs, refPrefix, numberOfPrefixes));
            }
            catch (IOException e) {
                log.debug("Error", e);
                doInit = false;
            }
        }
        // prefixReferences
        prefixReferences = new HashMap<String, LinkedHashMap<String, Long>>();
    }

    /**
     * Gets the object by id.
     *
     * @param field
     *            the field
     * @param docId
     *            the doc id
     * @param mtasId
     *            the mtas id
     * @return the object by id
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public MtasToken getObjectById(String field, int docId, int mtasId) throws IOException
    {
        try {
            Long ref;
            Long objectRefApproxCorrection;
            IndexDoc doc = getDoc(field, docId);
            IndexInput inObjectId = indexInputList.get("indexObjectId");
            IndexInput inObject = indexInputList.get("object");
            IndexInput inTerm = indexInputList.get("term");
            if (doc.storageFlags == MtasCodecPostingsFormat.MTAS_STORAGE_BYTE) {
                inObjectId.seek(doc.fpIndexObjectId + (mtasId * 1L));
                objectRefApproxCorrection = Long.valueOf(inObjectId.readByte());
            }
            else if (doc.storageFlags == MtasCodecPostingsFormat.MTAS_STORAGE_SHORT) {
                inObjectId.seek(doc.fpIndexObjectId + (mtasId * 2L));
                objectRefApproxCorrection = Long.valueOf(inObjectId.readShort());
            }
            else if (doc.storageFlags == MtasCodecPostingsFormat.MTAS_STORAGE_INTEGER) {
                inObjectId.seek(doc.fpIndexObjectId + (mtasId * 4L));
                objectRefApproxCorrection = Long.valueOf(inObjectId.readInt());
            }
            else {
                inObjectId.seek(doc.fpIndexObjectId + (mtasId * 8L));
                objectRefApproxCorrection = Long.valueOf(inObjectId.readLong());
            }
            ref = objectRefApproxCorrection + doc.objectRefApproxOffset
                    + (mtasId * (long) doc.objectRefApproxQuotient);
            return MtasCodecPostingsFormat.getToken(inObject, inTerm, ref);
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Gets the objects by parent id.
     *
     * @param field
     *            the field
     * @param docId
     *            the doc id
     * @param position
     *            the position
     * @return the objects by parent id
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public List<MtasTokenString> getObjectsByParentId(String field, int docId, int position)
        throws IOException
    {
        IndexDoc doc = getDoc(field, docId);
        IndexInput inIndexObjectParent = indexInputList.get("indexObjectParent");
        ArrayList<MtasTreeHit<?>> hits = CodecSearchTree.searchMtasTree(position,
                inIndexObjectParent, doc.fpIndexObjectParent, doc.smallestObjectFilepointer);
        return getObjects(hits);
    }

    /**
     * Gets the objects by position.
     *
     * @param field
     *            the field
     * @param docId
     *            the doc id
     * @param position
     *            the position
     * @return the objects by position
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public ArrayList<MtasTokenString> getObjectsByPosition(String field, int docId, int position)
        throws IOException
    {
        IndexDoc doc = getDoc(field, docId);
        IndexInput inIndexObjectPosition = indexInputList.get("indexObjectPosition");
        ArrayList<MtasTreeHit<?>> hits = CodecSearchTree.searchMtasTree(position,
                inIndexObjectPosition, doc.fpIndexObjectPosition, doc.smallestObjectFilepointer);
        return getObjects(hits);
    }

    /**
     * Gets the objects by positions.
     *
     * @param field
     *            the field
     * @param docId
     *            the doc id
     * @param startPosition
     *            the start position
     * @param endPosition
     *            the end position
     * @return the objects by positions
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public ArrayList<MtasTokenString> getObjectsByPositions(String field, int docId,
            int startPosition, int endPosition)
        throws IOException
    {
        IndexDoc doc = getDoc(field, docId);
        IndexInput inIndexObjectPosition = indexInputList.get("indexObjectPosition");
        ArrayList<MtasTreeHit<?>> hits = CodecSearchTree.searchMtasTree(startPosition, endPosition,
                inIndexObjectPosition, doc.fpIndexObjectPosition, doc.smallestObjectFilepointer);
        return getObjects(hits);
    }

    /**
     * Gets the prefix filtered objects by positions.
     *
     * @param field
     *            the field
     * @param docId
     *            the doc id
     * @param prefixes
     *            the prefixes
     * @param startPosition
     *            the start position
     * @param endPosition
     *            the end position
     * @return the prefix filtered objects by positions
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public List<MtasTokenString> getPrefixFilteredObjectsByPositions(String field, int docId,
            List<String> prefixes, int startPosition, int endPosition)
        throws IOException
    {
        IndexDoc doc = getDoc(field, docId);
        IndexInput inIndexObjectPosition = indexInputList.get("indexObjectPosition");
        if (doc != null && startPosition <= endPosition) {
            ArrayList<MtasTreeHit<?>> hits = CodecSearchTree.searchMtasTree(startPosition,
                    endPosition, inIndexObjectPosition, doc.fpIndexObjectPosition,
                    doc.smallestObjectFilepointer);
            return getPrefixFilteredObjects(hits, prefixes);
        }
        else {
            return new ArrayList<>();
        }
    }

    /**
     * Gets the prefix filtered objects.
     *
     * @param hits
     *            the hits
     * @param prefixes
     *            the prefixes
     * @return the prefix filtered objects
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private List<MtasTokenString> getPrefixFilteredObjects(List<MtasTreeHit<?>> hits,
            List<String> prefixes)
        throws IOException
    {
        ArrayList<MtasTokenString> tokens = new ArrayList<>();
        IndexInput inObject = indexInputList.get("object");
        IndexInput inTerm = indexInputList.get("term");
        for (MtasTreeHit<?> hit : hits) {
            MtasTokenString token = MtasCodecPostingsFormat.getToken(inObject, inTerm, hit.ref);
            if (token != null) {
                if (prefixes != null && !prefixes.isEmpty()) {
                    if (prefixes.contains(token.getPrefix())) {
                        tokens.add(token);
                    }
                }
                else {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }

    /**
     * Gets the positioned terms by prefixes and position.
     *
     * @param field
     *            the field
     * @param docId
     *            the doc id
     * @param prefixes
     *            the prefixes
     * @param position
     *            the position
     * @return the positioned terms by prefixes and position
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public List<MtasTreeHit<String>> getPositionedTermsByPrefixesAndPosition(String field,
            int docId, List<String> prefixes, int position)
        throws IOException
    {
        return getPositionedTermsByPrefixesAndPositionRange(field, docId, prefixes, position,
                position);
    }

    /**
     * Gets the positioned terms by prefixes and position range.
     *
     * @param field
     *            the field
     * @param docId
     *            the doc id
     * @param prefixes
     *            the prefixes
     * @param startPosition
     *            the start position
     * @param endPosition
     *            the end position
     * @return the positioned terms by prefixes and position range
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public List<MtasTreeHit<String>> getPositionedTermsByPrefixesAndPositionRange(String field,
            int docId, List<String> prefixes, int startPosition, int endPosition)
        throws IOException
    {
        IndexDoc doc = getDoc(field, docId);
        IndexInput inIndexObjectPosition = indexInputList.get("indexObjectPosition");
        if (doc != null && startPosition <= endPosition) {
            ArrayList<MtasTreeHit<?>> hitItems = CodecSearchTree.searchMtasTree(startPosition,
                    endPosition, inIndexObjectPosition, doc.fpIndexObjectPosition,
                    doc.smallestObjectFilepointer);
            List<MtasTreeHit<String>> hits = new ArrayList<>();
            Map<String, Integer> prefixIds = getPrefixesIds(field, prefixes);
            if (prefixIds != null && prefixIds.size() > 0) {
                ArrayList<MtasTreeHit<?>> filteredHitItems = new ArrayList<MtasTreeHit<?>>();

                for (MtasTreeHit<?> hitItem : hitItems) {
                    if (prefixIds.containsValue(hitItem.additionalId)) {
                        filteredHitItems.add(hitItem);
                    }
                }
                if (filteredHitItems.size() > 0) {
                    ArrayList<MtasTokenString> objects = getObjects(filteredHitItems);
                    for (MtasTokenString token : objects) {
                        MtasTreeHit<String> hit = new MtasTreeHit<String>(token.getPositionStart(),
                                token.getPositionEnd(), token.getTokenRef(), 0, 0,
                                token.getValue());
                        hits.add(hit);
                    }
                }
            }
            return hits;
        }
        else {
            return new ArrayList<MtasTreeHit<String>>();
        }
    }

    /**
     * Collect terms by prefixes for list of hit positions.
     *
     * @param field
     *            the field
     * @param docId
     *            the doc id
     * @param listPrefixes
     *            the prefixes
     * @param positionsHits
     *            the positions hits
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void collectTermsByPrefixesForListOfHitPositions(String field, int docId,
            List<String> listPrefixes, ArrayList<IntervalTreeNodeData<String>> positionsHits)
        throws IOException
    {
        IndexDoc doc = getDoc(field, docId);
        IndexInput inIndexObjectPosition = indexInputList.get("indexObjectPosition");
        IndexInput inTerm = indexInputList.get("term");
        // create tree interval hits
        IntervalRBTree<String> positionTree = new IntervalRBTree<String>(positionsHits);

        // find prefixIds
        Map<String, Integer> prefixIds = getPrefixesIds(field, listPrefixes);
        // search matching tokens
        if (prefixIds != null) {
            CodecSearchTree.searchMtasTreeWithIntervalTree(prefixIds.values(), positionTree,
                    inIndexObjectPosition, doc.fpIndexObjectPosition,
                    doc.smallestObjectFilepointer);

            // reverse list
            Map<Integer, String> idPrefixes = new HashMap<>();
            for (Entry<String, Integer> entry : prefixIds.entrySet()) {
                idPrefixes.put(entry.getValue(), entry.getKey());
            }
            // term administration
            Map<Long, String> refTerms = new HashMap<>();

            for (IntervalTreeNodeData<String> positionHit : positionsHits) {
                for (MtasTreeHit<String> hit : positionHit.list) {
                    if (hit.idData == null) {
                        hit.idData = idPrefixes.get(hit.additionalId);
                        if (!refTerms.containsKey(hit.additionalRef)) {
                            refTerms.put(hit.additionalRef,
                                    MtasCodecPostingsFormat.getTerm(inTerm, hit.additionalRef));
                        }
                        hit.refData = refTerms.get(hit.additionalRef);
                    }
                }
            }
        }
    }

    /**
     * Gets the objects.
     *
     * @param hits
     *            the hits
     * @return the objects
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public ArrayList<MtasTokenString> getObjects(List<MtasTreeHit<?>> hits) throws IOException
    {
        ArrayList<MtasTokenString> tokens = new ArrayList<>();
        IndexInput inObject = indexInputList.get("object");
        IndexInput inTerm = indexInputList.get("term");
        for (MtasTreeHit<?> hit : hits) {
            MtasTokenString token = MtasCodecPostingsFormat.getToken(inObject, inTerm, hit.ref);
            if (token != null) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    /**
     * Gets the terms.
     *
     * @param refs
     *            the refs
     * @return the terms
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public ArrayList<MtasTreeHit<String>> getTerms(ArrayList<MtasTreeHit<?>> refs)
        throws IOException
    {
        try {
            ArrayList<MtasTreeHit<String>> terms = new ArrayList<MtasTreeHit<String>>();
            IndexInput inTerm = indexInputList.get("term");
            for (MtasTreeHit<?> hit : refs) {
                inTerm.seek(hit.ref);
                String term = inTerm.readString();
                MtasTreeHit<String> newHit = new MtasTreeHit<String>(hit.startPosition,
                        hit.endPosition, hit.ref, hit.additionalId, hit.additionalRef, term);
                terms.add(newHit);
            }
            return terms;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Gets the prefixes ids.
     *
     * @param field
     *            the field
     * @param prefixes
     *            the prefixes
     * @return the prefixes ids
     */
    Map<String, Integer> getPrefixesIds(String field, List<String> prefixes)
    {
        LinkedHashMap<String, Long> refs = getPrefixRefs(field);
        if (refs != null) {
            List<String> list = new ArrayList<>(refs.keySet());
            Map<String, Integer> result = new HashMap<>();
            for (String prefix : prefixes) {
                int id = list.indexOf(prefix);
                if (id >= 0) {
                    result.put(prefix, id + 1);
                }
            }
            return result;
        }
        else {
            return null;
        }
    }

    /**
     * Gets the prefixes.
     *
     * @param field
     *            the field
     * @return the prefixes
     */
    public Set<String> getPrefixes(String field)
    {
        LinkedHashMap<String, Long> prefixRefs = this.getPrefixRefs(field);
        return prefixRefs.keySet();
    }

    /**
     * Gets the prefixes.
     *
     * @param field
     *            the field
     * @return the prefixes
     */
    private LinkedHashMap<String, Long> getPrefixRefs(String field)
    {
        if (fieldReferences.containsKey(field)) {
            FieldReferences fr = fieldReferences.get(field);
            if (!prefixReferences.containsKey(field)) {
                LinkedHashMap<String, Long> refs = new LinkedHashMap<String, Long>();
                try {
                    IndexInput inPrefix = indexInputList.get("prefix");
                    inPrefix.seek(fr.refPrefix);
                    for (int i = 0; i < fr.numberOfPrefixes; i++) {
                        Long ref = inPrefix.getFilePointer();
                        String prefix = inPrefix.readString();
                        refs.put(prefix, ref);
                    }
                }
                catch (Exception e) {
                    log.error("Error", e);
                    refs.clear();
                }
                prefixReferences.put(field, refs);
                return refs;
            }
            else {
                return prefixReferences.get(field);
            }
        }
        else {
            return null;
        }
    }

    /**
     * Gets the doc.
     *
     * @param field
     *            the field
     * @param docId
     *            the doc id
     * @return the doc
     */
    public IndexDoc getDoc(String field, int docId)
    {
        if (fieldReferences.containsKey(field)) {
            FieldReferences fr = fieldReferences.get(field);
            try {
                IndexInput inIndexDocId = indexInputList.get("indexDocId");
                ArrayList<MtasTreeHit<?>> list = CodecSearchTree.searchMtasTree(docId, inIndexDocId,
                        fr.refIndexDocId, fr.refIndexDoc);
                if (list.size() == 1) {
                    return new IndexDoc(list.get(0).ref);
                }
            }
            catch (IOException e) {
                log.debug("Error", e);
                return null;
            }
        }
        return null;
    }

    /**
     * Gets the next doc.
     *
     * @param field
     *            the field
     * @param previousDocId
     *            the previous doc id
     * @return the next doc
     */
    public IndexDoc getNextDoc(String field, int previousDocId)
    {
        if (fieldReferences.containsKey(field)) {
            FieldReferences fr = fieldReferences.get(field);
            try {
                if (previousDocId < 0) {
                    return new IndexDoc(fr.refIndexDoc);
                }
                else {
                    int nextDocId = previousDocId + 1;
                    IndexInput inIndexDocId = indexInputList.get("indexDocId");
                    ArrayList<MtasTreeHit<?>> list = CodecSearchTree.advanceMtasTree(nextDocId,
                            inIndexDocId, fr.refIndexDocId, fr.refIndexDoc);
                    if (list.size() == 1) {
                        IndexInput inDoc = indexInputList.get("doc");
                        inDoc.seek(list.get(0).ref);
                        return new IndexDoc(inDoc.getFilePointer());
                    }
                }
            }
            catch (IOException e) {
                log.debug("Error", e);
                return null;
            }
        }
        return null;
    }

    /**
     * Gets the number of docs.
     *
     * @param field
     *            the field
     * @return the number of docs
     */
    public int getNumberOfDocs(String field)
    {
        if (fieldReferences.containsKey(field)) {
            FieldReferences fr = fieldReferences.get(field);
            return fr.numberOfDocs;
        }
        else {
            return 0;
        }
    }

    /**
     * Gets the number of positions.
     *
     * @param field
     *            the field
     * @param docId
     *            the doc id
     * @return the number of positions
     */
    public Integer getNumberOfPositions(String field, int docId)
    {
        if (fieldReferences.containsKey(field)) {
            IndexDoc doc = getDoc(field, docId);
            if (doc != null) {
                return 1 + doc.maxPosition - doc.minPosition;
            }
        }
        return null;
    }

    /**
     * Gets the all number of positions.
     *
     * @param field
     *            the field
     * @param docBase
     *            the doc base
     * @return the all number of positions
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public HashMap<Integer, Integer> getAllNumberOfPositions(String field, int docBase)
        throws IOException
    {
        HashMap<Integer, Integer> numbers = new HashMap<Integer, Integer>();
        if (fieldReferences.containsKey(field)) {
            FieldReferences fr = fieldReferences.get(field);
            IndexInput inIndexDoc = indexInputList.get("doc");
            inIndexDoc.seek(fr.refIndexDoc);
            IndexDoc doc;
            for (int i = 0; i < fr.numberOfDocs; i++) {
                doc = new IndexDoc(null);
                numbers.put((doc.docId + docBase), (1 + doc.maxPosition - doc.minPosition));
            }
        }
        return numbers;
    }

    /**
     * Gets the number of tokens.
     *
     * @param field
     *            the field
     * @param docId
     *            the doc id
     * @return the number of tokens
     */
    public Integer getNumberOfTokens(String field, int docId)
    {
        if (fieldReferences.containsKey(field)) {
            IndexDoc doc = getDoc(field, docId);
            if (doc != null) {
                return doc.size;
            }
        }
        return null;
    }

    /**
     * Gets the all number of tokens.
     *
     * @param field
     *            the field
     * @param docBase
     *            the doc base
     * @return the all number of tokens
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public HashMap<Integer, Integer> getAllNumberOfTokens(String field, int docBase)
        throws IOException
    {
        HashMap<Integer, Integer> numbers = new HashMap<Integer, Integer>();
        if (fieldReferences.containsKey(field)) {
            FieldReferences fr = fieldReferences.get(field);
            IndexInput inIndexDoc = indexInputList.get("doc");
            inIndexDoc.seek(fr.refIndexDoc);
            IndexDoc doc;
            for (int i = 0; i < fr.numberOfDocs; i++) {
                doc = new IndexDoc(null);
                numbers.put((doc.docId + docBase), doc.size);
            }
        }
        return numbers;
    }

    /**
     * The Class IndexDoc.
     */
    public class IndexDoc
    {

        /** The doc id. */
        public int docId;

        /** The fp index object id. */
        public long fpIndexObjectId;

        /** The fp index object position. */
        public long fpIndexObjectPosition;

        /** The fp index object parent. */
        public long fpIndexObjectParent;

        /** The smallest object filepointer. */
        public long smallestObjectFilepointer;

        /** The object ref approx offset. */
        public long objectRefApproxOffset;

        /** The object ref approx quotient. */
        public int objectRefApproxQuotient;

        /** The storage flags. */
        public byte storageFlags;

        /** The size. */
        public int size;

        /** The min position. */
        public int minPosition;

        /** The max position. */
        public int maxPosition;

        /**
         * Instantiates a new index doc.
         *
         * @param ref
         *            the ref
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public IndexDoc(Long ref) throws IOException
        {
            try {
                IndexInput inIndexDoc = indexInputList.get("doc");
                if (ref != null) {
                    inIndexDoc.seek(ref);
                }
                docId = inIndexDoc.readVInt(); // docId
                fpIndexObjectId = inIndexDoc.readVLong(); // ref indexObjectId
                fpIndexObjectPosition = inIndexDoc.readVLong(); // ref
                                                                // indexObjectPosition
                fpIndexObjectParent = inIndexDoc.readVLong(); // ref indexObjectParent
                smallestObjectFilepointer = inIndexDoc.readVLong(); // offset
                objectRefApproxQuotient = inIndexDoc.readVInt(); // slope
                objectRefApproxOffset = inIndexDoc.readZLong(); // offset
                storageFlags = inIndexDoc.readByte(); // flag
                size = inIndexDoc.readVInt(); // number of objects
                minPosition = inIndexDoc.readVInt(); // minimum position
                maxPosition = inIndexDoc.readVInt(); // maximum position
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    /**
     * The Class FieldReferences.
     */
    private static class FieldReferences
    {

        /** The ref index doc. */
        public long refIndexDoc;

        /** The ref index doc id. */
        public long refIndexDocId;

        /** The ref prefix. */
        public long refPrefix;

        /** The number of docs. */
        public int numberOfDocs;

        /** The number of prefixes. */
        public int numberOfPrefixes;

        /**
         * Instantiates a new field references.
         *
         * @param refIndexDoc
         *            the ref index doc
         * @param refIndexDocId
         *            the ref index doc id
         * @param numberOfDocs
         *            the number of docs
         * @param refPrefix
         *            the ref prefix
         * @param numberOfPrefixes
         *            the number of prefixes
         */
        public FieldReferences(long refIndexDoc, long refIndexDocId, int numberOfDocs,
                long refPrefix, int numberOfPrefixes)
        {
            this.refIndexDoc = refIndexDoc;
            this.refIndexDocId = refIndexDocId;
            this.numberOfDocs = numberOfDocs;
            this.refPrefix = refPrefix;
            this.numberOfPrefixes = numberOfPrefixes;
        }
    }

}
