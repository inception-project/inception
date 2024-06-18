package mtas.codec;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.store.IndexInput;

/**
 * The Class MtasFieldsProducer.
 */
public class MtasFieldsProducer extends FieldsProducer {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(MtasFieldsProducer.class);

  /** The delegate fields producer. */
  private FieldsProducer delegateFieldsProducer;

  /** The index input list. */
  private HashMap<String, IndexInput> indexInputList;

  /** The index input offset list. */
  private HashMap<String, Long> indexInputOffsetList;

  /** The version. */
  private int version;

  /**
   * Instantiates a new mtas fields producer.
   *
   * @param state the state
   * @param name the name
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public MtasFieldsProducer(SegmentReadState state, String name)
      throws IOException {
    String postingsFormatName = null;
    indexInputList = new HashMap<>();
    indexInputOffsetList = new HashMap<>();
    version = MtasCodecPostingsFormat.VERSION_CURRENT;    
    postingsFormatName = addIndexInputToList("object", openMtasFile(state, name,
        MtasCodecPostingsFormat.MTAS_OBJECT_EXTENSION), postingsFormatName);
    addIndexInputToList("term",
        openMtasFile(state, name, MtasCodecPostingsFormat.MTAS_TERM_EXTENSION),
        postingsFormatName);
    addIndexInputToList("prefix", openMtasFile(state, name,
        MtasCodecPostingsFormat.MTAS_PREFIX_EXTENSION), postingsFormatName);
    addIndexInputToList("field",
        openMtasFile(state, name, MtasCodecPostingsFormat.MTAS_FIELD_EXTENSION),
        postingsFormatName);
    addIndexInputToList("indexDocId",
        openMtasFile(state, name,
            MtasCodecPostingsFormat.MTAS_INDEX_DOC_ID_EXTENSION),
        postingsFormatName);
    addIndexInputToList("indexObjectId",
        openMtasFile(state, name,
            MtasCodecPostingsFormat.MTAS_INDEX_OBJECT_ID_EXTENSION),
        postingsFormatName);
    try {
      addIndexInputToList(
          "doc", openMtasFile(state, name,
              MtasCodecPostingsFormat.MTAS_DOC_EXTENSION, version, version),
          postingsFormatName);
      addIndexInputToList("indexObjectPosition",
          openMtasFile(state, name,
              MtasCodecPostingsFormat.MTAS_INDEX_OBJECT_POSITION_EXTENSION,
              version, version),
          postingsFormatName);
      addIndexInputToList("indexObjectParent",
          openMtasFile(state, name,
              MtasCodecPostingsFormat.MTAS_INDEX_OBJECT_PARENT_EXTENSION,
              version, version),
          postingsFormatName);
    } catch (IndexFormatTooOldException e) {
      log.debug("Error", e);
      throw new IOException(
          "This MTAS doesn't support your index version, please upgrade");
    }
    // Load the delegate postingsFormatName from this file
    this.delegateFieldsProducer = PostingsFormat.forName(postingsFormatName)
        .fieldsProducer(state);      
  }

  /**
   * Adds the index input to list.
   *
   * @param name the name
   * @param in the in
   * @param postingsFormatName the postings format name
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private String addIndexInputToList(String name, IndexInput in,
      String postingsFormatName) throws IOException {
    if (indexInputList.get(name) != null) {
      indexInputList.get(name).close();
    }
    if (in != null) {
      String localPostingsFormatName = postingsFormatName;
      if (localPostingsFormatName == null) {
        localPostingsFormatName = in.readString();
      } else if (!in.readString().equals(localPostingsFormatName)) {
        throw new IOException("delegate codec " + name + " doesn't equal "
            + localPostingsFormatName);
      }
      indexInputList.put(name, in);
      indexInputOffsetList.put(name, in.getFilePointer());
      return localPostingsFormatName;
    } else {
      log.debug("no " + name + " registered");
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.index.Fields#iterator()
   */
  @Override
  public Iterator<String> iterator() {
    return delegateFieldsProducer.iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.codecs.FieldsProducer#close()
   */
  @Override
  public void close() throws IOException {
    delegateFieldsProducer.close();
    for (Entry<String, IndexInput> entry : indexInputList.entrySet()) {
      entry.getValue().close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.index.Fields#terms(java.lang.String)
   */
  @Override
  public Terms terms(String field) throws IOException {
    return new MtasTerms(delegateFieldsProducer.terms(field), indexInputList,
        indexInputOffsetList, version);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.index.Fields#size()
   */
  @Override
  public int size() {
    return delegateFieldsProducer.size();
  }

//  /*
//   * (non-Javadoc)
//   * 
//   * @see org.apache.lucene.util.Accountable#ramBytesUsed()
//   */
//  @Override
//  public long ramBytesUsed() {
//    // return BASE_RAM_BYTES_USED + delegateFieldsProducer.ramBytesUsed();
//    return 3 * delegateFieldsProducer.ramBytesUsed();
//  }
//
//  /*
//   * (non-Javadoc)
//   * 
//   * @see org.apache.lucene.util.Accountable#getChildResources()
//   */
//  @Override
//  public Collection<Accountable> getChildResources() {
//    List<Accountable> resources = new ArrayList<>();
//    if (delegateFieldsProducer != null) {
//      resources.add(
//          Accountables.namedAccountable("delegate", delegateFieldsProducer));
//    }
//    return Collections.unmodifiableList(resources);
//  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.codecs.FieldsProducer#checkIntegrity()
   */
  @Override
  public void checkIntegrity() throws IOException {
    delegateFieldsProducer.checkIntegrity();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getClass().getSimpleName() + "(delegate=" + delegateFieldsProducer
        + ")";
  }

  /**
   * Open mtas file.
   *
   * @param state the state
   * @param name the name
   * @param extension the extension
   * @param minimum the minimum
   * @param maximum the maximum
   * @return the index input
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private IndexInput openMtasFile(SegmentReadState state, String name,
      String extension, Integer minimum, Integer maximum) throws IOException {
    String fileName = IndexFileNames.segmentFileName(state.segmentInfo.name,
        state.segmentSuffix, extension);
    IndexInput object;
    try {
      object = state.directory.openInput(fileName, state.context);
    } catch (FileNotFoundException | NoSuchFileException e) {
      log.debug("Error", e);
      // throw new NoSuchFileException(e.getMessage());
      return null;
    }
    int minVersion = (minimum == null) ? MtasCodecPostingsFormat.VERSION_START
        : minimum.intValue();
    int maxVersion = (maximum == null) ? MtasCodecPostingsFormat.VERSION_CURRENT
        : maximum.intValue();
    try {
      CodecUtil.checkIndexHeader(object, name, minVersion, maxVersion,
          state.segmentInfo.getId(), state.segmentSuffix);
    } catch (IndexFormatTooOldException e) {
      object.close();
      log.debug("Error", e);
      throw new IndexFormatTooOldException(e.getMessage(), e.getVersion(),
          e.getMinVersion(), e.getMaxVersion());
    } catch (EOFException e) {
      object.close();
      log.debug("Error", e);
      // throw new EOFException(e.getMessage());
      return null;
    }
    return object;
  }

  /**
   * Open mtas file.
   *
   * @param state the state
   * @param name the name
   * @param extension the extension
   * @return the index input
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private IndexInput openMtasFile(SegmentReadState state, String name,
      String extension) throws IOException {
    return openMtasFile(state, name, extension, null, null);
  }

}
