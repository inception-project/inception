package mtas.codec.util.collector;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.Map.Entry;

import mtas.codec.util.DataCollector;


/**
 * The Class MtasDataCollector.
 *
 * @param <T1> the generic type
 * @param <T2> the generic type
 */
public abstract class MtasDataCollector<T1 extends Number & Comparable<T1>, T2 extends Number & Comparable<T2>>
    implements Serializable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /** The Constant SEGMENT_SORT_ASC. */
  public static final String SEGMENT_SORT_ASC = "segment_asc";

  /** The Constant SEGMENT_SORT_DESC. */
  public static final String SEGMENT_SORT_DESC = "segment_desc";

  /** The Constant SEGMENT_BOUNDARY_ASC. */
  public static final String SEGMENT_BOUNDARY_ASC = "segment_boundary_asc";

  /** The Constant SEGMENT_BOUNDARY_DESC. */
  public static final String SEGMENT_BOUNDARY_DESC = "segment_boundary_desc";

  /** The Constant SEGMENT_KEY. */
  public static final String SEGMENT_KEY = "key";

  /** The Constant SEGMENT_NEW. */
  public static final String SEGMENT_NEW = "new";

  /** The Constant SEGMENT_KEY_OR_NEW. */
  public static final String SEGMENT_KEY_OR_NEW = "key_or_new";

  /** The Constant SEGMENT_POSSIBLE_KEY. */
  public static final String SEGMENT_POSSIBLE_KEY = "possible_key";

  /** The size. */
  protected int size;

  /** The position. */
  protected int position;

  /** The collector type. */
  // properties collector
  protected String collectorType;

  /** The stats type. */
  protected String statsType;

  /** The data type. */
  protected String dataType;

  /** The stats items. */
  private SortedSet<String> statsItems;

  /** The sort type. */
  protected String sortType;

  /** The sort direction. */
  protected String sortDirection;

  /** The start. */
  protected Integer start;

  /** The number. */
  protected Integer number;

  /** The error number. */
  // error
  protected int[] errorNumber;

  /** The error list. */
  protected HashMap<String, Integer>[] errorList;

  /** The key list. */
  protected String[] keyList;

  /** The source number list. */
  protected int[] sourceNumberList;

  /** The with total. */
  private boolean withTotal;

  /** The segment registration. */
  public transient String segmentRegistration;

  /** The segment key value list. */
  protected transient LinkedHashMap<String, Map<String, T1>> segmentKeyValueList;

  /** The segment recompute key list. */
  public transient Map<String, Set<String>> segmentRecomputeKeyList;

  /** The segment keys. */
  public transient Set<String> segmentKeys;

  /** The segment values boundary. */
  protected transient Map<String, T1> segmentValuesBoundary;

  /** The segment value boundary. */
  protected transient T1 segmentValueBoundary;

  /** The segment value top list last. */
  protected transient Map<String, T1> segmentValueTopListLast;

  /** The segment value top list. */
  protected transient ArrayList<T1> segmentValueTopList;

  /** The segment name. */
  protected transient String segmentName;

  /** The segment number. */
  protected transient int segmentNumber;

  /** The has sub. */
  private boolean hasSub;

  /** The sub collector types. */
  private String[] subCollectorTypes;

  /** The sub data types. */
  private String[] subDataTypes;

  /** The sub stats types. */
  private String[] subStatsTypes;

  /** The sub stats items. */
  private SortedSet<String>[] subStatsItems;

  /** The sub sort types. */
  private String[] subSortTypes;

  /** The sub sort directions. */
  private String[] subSortDirections;

  /** The sub start. */
  private Integer[] subStart;

  /** The sub number. */
  private Integer[] subNumber;

  /** The sub collector list next level. */
  protected MtasDataCollector<?, ?>[] subCollectorListNextLevel = null;

  /** The sub collector next level. */
  protected MtasDataCollector<?, ?> subCollectorNextLevel = null;
  
  /** The merged into. */
  public MtasDataCollector<?, ?> mergedInto = null;
  
  /** The new size. */
  protected transient int newSize;

  /** The new position. */
  protected transient int newPosition;

  /** The new current position. */
  protected transient int newCurrentPosition;

  /** The new current existing. */
  protected transient boolean newCurrentExisting;

  /** The new key list. */
  protected transient String[] newKeyList = null;

  /** The new source number list. */
  protected transient int[] newSourceNumberList = null;

  /** The new error number. */
  protected transient int[] newErrorNumber;

  /** The new error list. */
  protected transient HashMap<String, Integer>[] newErrorList;

  /** The new known key found in segment. */
  public transient Set<String> newKnownKeyFoundInSegment;

  /** The new sub collector types. */
  private transient String[] newSubCollectorTypes;

  /** The new sub data types. */
  private transient String[] newSubDataTypes;

  /** The new sub stats types. */
  private transient String[] newSubStatsTypes;

  /** The new sub stats items. */
  private transient SortedSet<String>[] newSubStatsItems;

  /** The new sub sort types. */
  private transient String[] newSubSortTypes;

  /** The new sub sort directions. */
  private transient String[] newSubSortDirections;

  /** The new sub start. */
  private transient Integer[] newSubStart;

  /** The new sub number. */
  private transient Integer[] newSubNumber;

  /** The new sub collector list next level. */
  // subcollectors next level for adding
  protected transient MtasDataCollector<?, ?>[] newSubCollectorListNextLevel = null;

  /** The new sub collector next level. */
  protected transient MtasDataCollector<?, ?> newSubCollectorNextLevel = null;

  /** The closed. */
  protected transient boolean closed = false;

  /** The result. */
  private transient MtasDataCollectorResult<T1, T2> result = null;

  /**
   * Instantiates a new mtas data collector.
   *
   * @param collectorType the collector type
   * @param dataType the data type
   * @param statsType the stats type
   * @param statsItems the stats items
   * @param sortType the sort type
   * @param sortDirection the sort direction
   * @param start the start
   * @param number the number
   * @param segmentRegistration the segment registration
   * @param boundary the boundary
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("unchecked")
  protected MtasDataCollector(String collectorType, String dataType,
      String statsType, SortedSet<String> statsItems, String sortType,
      String sortDirection, Integer start, Integer number,
      String segmentRegistration, String boundary) throws IOException {
    // set properties
    this.closed = false;
    this.collectorType = collectorType; // data or list
    this.dataType = dataType; // long or double
    this.statsType = statsType; // basic, advanced or full
    this.statsItems = statsItems; // sum, n, all, ...
    this.sortType = sortType;
    this.sortDirection = sortDirection;
    this.start = start;
    this.number = number;
    this.segmentRegistration = segmentRegistration;
    this.withTotal = false;
    if (segmentRegistration != null) {
      segmentKeys = new HashSet<>();
      segmentKeyValueList = new LinkedHashMap<>();
      segmentValuesBoundary = new LinkedHashMap<>();
      segmentValueTopListLast = new LinkedHashMap<>();
      if (segmentRegistration.equals(SEGMENT_BOUNDARY_ASC)
          || segmentRegistration.equals(SEGMENT_BOUNDARY_DESC)) {
        if (boundary != null) {
          segmentValueBoundary = stringToBoundary(boundary);
        } else {
          throw new IOException("did expect boundary with segmentRegistration "
              + segmentRegistration);
        }
      } else if (boundary != null) {
        throw new IOException("didn't expect boundary with segmentRegistration "
            + segmentRegistration);
      }
    }
    // initialize administration
    keyList = new String[0];
    sourceNumberList = new int[0];
    errorNumber = new int[0];
    errorList = (HashMap<String, Integer>[]) new HashMap<?, ?>[0];
    size = 0;
    position = 0;
    // subCollectors properties
    hasSub = false;
    subCollectorTypes = null;
    subDataTypes = null;
    subStatsTypes = null;
    subStatsItems = null;
    subSortTypes = null;
    subSortDirections = null;
    subStart = null;
    subNumber = null;
    subCollectorListNextLevel = null;
    subCollectorNextLevel = null;
  }

  /**
   * Instantiates a new mtas data collector.
   *
   * @param collectorType the collector type
   * @param dataType the data type
   * @param statsType the stats type
   * @param statsItems the stats items
   * @param sortType the sort type
   * @param sortDirection the sort direction
   * @param start the start
   * @param number the number
   * @param subCollectorTypes the sub collector types
   * @param subDataTypes the sub data types
   * @param subStatsTypes the sub stats types
   * @param subStatsItems the sub stats items
   * @param subSortTypes the sub sort types
   * @param subSortDirections the sub sort directions
   * @param subStart the sub start
   * @param subNumber the sub number
   * @param segmentRegistration the segment registration
   * @param boundary the boundary
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected MtasDataCollector(String collectorType, String dataType,
      String statsType, SortedSet<String> statsItems, String sortType,
      String sortDirection, Integer start, Integer number,
      String[] subCollectorTypes, String[] subDataTypes, String[] subStatsTypes,
      SortedSet<String>[] subStatsItems, String[] subSortTypes,
      String[] subSortDirections, Integer[] subStart, Integer[] subNumber,
      String segmentRegistration, String boundary) throws IOException {
    // initialize
    this(collectorType, dataType, statsType, statsItems, sortType,
        sortDirection, start, number, segmentRegistration, boundary);
    // initialize subCollectors
    if (subCollectorTypes != null) {
      hasSub = true;
      this.subCollectorTypes = subCollectorTypes;
      this.subDataTypes = subDataTypes;
      this.subStatsTypes = subStatsTypes;
      this.subStatsItems = subStatsItems;
      this.subSortTypes = subSortTypes;
      this.subSortDirections = subSortDirections;
      this.subStart = subStart;
      this.subNumber = subNumber;
      if (subCollectorTypes.length > 1) {
        newSubCollectorTypes = Arrays.copyOfRange(subCollectorTypes, 1,
            subCollectorTypes.length);
        newSubDataTypes = Arrays.copyOfRange(subDataTypes, 1,
            subStatsTypes.length);
        newSubStatsTypes = Arrays.copyOfRange(subStatsTypes, 1,
            subStatsTypes.length);
        newSubStatsItems = Arrays.copyOfRange(subStatsItems, 1,
            subStatsItems.length);
        newSubSortTypes = Arrays.copyOfRange(subSortTypes, 1,
            subSortTypes.length);
        newSubSortDirections = Arrays.copyOfRange(subSortDirections, 1,
            subSortDirections.length);
        newSubStart = Arrays.copyOfRange(subStart, 1, subStart.length);
        newSubNumber = Arrays.copyOfRange(subNumber, 1, subNumber.length);
      }
      newSubCollectorListNextLevel = new MtasDataCollector[0];
    }
  }
  
  public static MtasDataCollector<?, ?> resolve (MtasDataCollector<?, ?> dc) { 
    while(dc!=null && dc.mergedInto!=null) {
      dc = dc.mergedInto;
    }
    return dc;
  }
  
  /**
   * Read object.
   *
   * @param in the in
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws ClassNotFoundException the class not found exception
   */
  private void readObject(java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (subCollectorTypes !=null && subCollectorTypes.length > 1) {
      newSubCollectorTypes = Arrays.copyOfRange(subCollectorTypes, 1,
          subCollectorTypes.length);
      newSubDataTypes = Arrays.copyOfRange(subDataTypes, 1,
          subStatsTypes.length);
      newSubStatsTypes = Arrays.copyOfRange(subStatsTypes, 1,
          subStatsTypes.length);
      newSubStatsItems = Arrays.copyOfRange(subStatsItems, 1,
          subStatsItems.length);
      newSubSortTypes = Arrays.copyOfRange(subSortTypes, 1,
          subSortTypes.length);
      newSubSortDirections = Arrays.copyOfRange(subSortDirections, 1,
          subSortDirections.length);
      newSubStart = Arrays.copyOfRange(subStart, 1, subStart.length);
      newSubNumber = Arrays.copyOfRange(subNumber, 1, subNumber.length);
    }
  }

  /**
   * Merge.
   *
   * @param newDataCollector the new data collector
   * @param map the map
   * @param increaseSourceNumber the increase source number
   * @throws IOException Signals that an I/O exception has occurred.
   */
  abstract public void merge(MtasDataCollector<?, ?> newDataCollector,
      boolean increaseSourceNumber) throws IOException;

  /**
   * Inits the new list.
   *
   * @param maxNumberOfTerms the max number of terms
   * @param segmentName the segment name
   * @param segmentNumber the segment number
   * @param boundary the boundary
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void initNewList(int maxNumberOfTerms, String segmentName,
      int segmentNumber, String boundary) throws IOException {
    if (closed) {
      result = null;
      closed = false;
    }
    initNewListBasic(maxNumberOfTerms);
    if (segmentRegistration != null) {
      this.segmentName = segmentName;
      this.segmentNumber = segmentNumber;
      if (!segmentKeyValueList.containsKey(segmentName)) {
        segmentKeyValueList.put(segmentName, new HashMap<String, T1>());
        if (segmentRegistration.equals(SEGMENT_BOUNDARY_ASC)
            || segmentRegistration.equals(SEGMENT_BOUNDARY_DESC)) {
          if (boundary != null) {
            segmentValuesBoundary.put(segmentName,
                stringToBoundary(boundary, segmentNumber));
          } else {
            throw new IOException("expected boundary");
          }
        } else {
          segmentValuesBoundary.put(segmentName, null);
        }
        segmentValueTopListLast.put(segmentName, null);
      }
      this.segmentValueTopList = new ArrayList<>();
    }
  }

  /**
   * Inits the new list.
   *
   * @param maxNumberOfTerms the max number of terms
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void initNewList(int maxNumberOfTerms) throws IOException {
    if (closed) {
      result = null;
      closed = false;
    }
    if (segmentRegistration != null) {
      throw new IOException("missing segment name");
    } else {
      initNewListBasic(maxNumberOfTerms);
    }
  }

  /**
   * Inits the new list basic.
   *
   * @param maxNumberOfTerms the max number of terms
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("unchecked")
  private void initNewListBasic(int maxNumberOfTerms) throws IOException {
    if (!closed) {
      position = 0;
      newPosition = 0;
      newCurrentPosition = 0;
      newSize = maxNumberOfTerms + size;
      newKeyList = new String[newSize];
      newSourceNumberList = new int[newSize];
      newErrorNumber = new int[newSize];
      newErrorList = (HashMap<String, Integer>[]) new HashMap<?, ?>[newSize];
      newKnownKeyFoundInSegment = new HashSet<>();
      if (hasSub) {
        newSubCollectorListNextLevel = new MtasDataCollector[newSize];
      }
    } else {
      throw new IOException("already closed");
    }
  }

  /**
   * Increase new list size.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("unchecked")
  protected void increaseNewListSize() throws IOException {
    if (!closed) {
      String[] tmpNewKeyList = newKeyList;
      int[] tmpNewSourceNumberList = newSourceNumberList;
      int[] tmpNewErrorNumber = newErrorNumber;
      HashMap<String, Integer>[] tmpNewErrorList = newErrorList;
      int tmpNewSize = newSize;
      newSize = 2 * newSize;
      newKeyList = new String[newSize];
      newSourceNumberList = new int[newSize];
      newErrorNumber = new int[newSize];
      newErrorList = (HashMap<String, Integer>[]) new HashMap<?, ?>[newSize];
      System.arraycopy(tmpNewKeyList, 0, newKeyList, 0, tmpNewSize);
      System.arraycopy(tmpNewSourceNumberList, 0, newSourceNumberList, 0,
          tmpNewSize);
      System.arraycopy(tmpNewErrorNumber, 0, newErrorNumber, 0, tmpNewSize);
      System.arraycopy(tmpNewErrorList, 0, newErrorList, 0, tmpNewSize);
      if (hasSub) {
        MtasDataCollector<?, ?>[] tmpNewSubCollectorListNextLevel = newSubCollectorListNextLevel;
        newSubCollectorListNextLevel = new MtasDataCollector[newSize];
        System.arraycopy(tmpNewSubCollectorListNextLevel, 0,
            newSubCollectorListNextLevel, 0, tmpNewSize);
      }
    } else {
      throw new IOException("already closed");
    }
  }

  /**
   * Adds the.
   *
   * @param increaseSourceNumber the increase source number
   * @return the mtas data collector
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected final MtasDataCollector getSubCollector(boolean increaseSourceNumber)
      throws IOException {
    if (!closed) {
      if (!collectorType.equals(DataCollector.COLLECTOR_TYPE_DATA)) {
        throw new IOException(
            "collector should be " + DataCollector.COLLECTOR_TYPE_DATA);
      } else {
        if (newPosition > 0) {
          newCurrentExisting = true;
        } else if (position < getSize()) {
          // copy
          newKeyList[0] = keyList[0];
          newSourceNumberList[0] = sourceNumberList[0];
          if (increaseSourceNumber) {
            newSourceNumberList[0]++;
          }
          newErrorNumber[0] = errorNumber[0];
          newErrorList[0] = errorList[0];
          if (hasSub) {
            newSubCollectorNextLevel = subCollectorNextLevel;
          }
          copyToNew(0, 0);
          newPosition = 1;
          position = 1;
          newCurrentExisting = true;
        } else {
          // add key
          newKeyList[0] = DataCollector.COLLECTOR_TYPE_DATA;
          newSourceNumberList[0] = 1;
          newErrorNumber[0] = 0;
          newErrorList[0] = new HashMap<>();
          newPosition = 1;
          newCurrentPosition = newPosition - 1;
          newCurrentExisting = false;
          // ready, only handle sub
          if (hasSub) {
            newSubCollectorNextLevel = DataCollector.getCollector(
                subCollectorTypes[0], subDataTypes[0], subStatsTypes[0],
                subStatsItems[0], subSortTypes[0], subSortDirections[0],
                subStart[0], subNumber[0], newSubCollectorTypes,
                newSubDataTypes, newSubStatsTypes, newSubStatsItems,
                newSubSortTypes, newSubSortDirections, newSubStart,
                newSubNumber, segmentRegistration, null);
          } else {
            newSubCollectorNextLevel = null;
          }
        }
        return newSubCollectorNextLevel;
      }
    } else {
      throw new IOException("already closed");
    }
  }

  /**
   * Adds the.
   *
   * @param key the key
   * @param increaseSourceNumber the increase source number
   * @return the mtas data collector
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected final MtasDataCollector getSubCollector(String key,
      boolean increaseSourceNumber) throws IOException {
    if (!closed) {
      if (collectorType.equals(DataCollector.COLLECTOR_TYPE_DATA)) {
        throw new IOException(
            "collector should be " + DataCollector.COLLECTOR_TYPE_LIST);
      } else if (key == null) {
        throw new IOException("key shouldn't be null");
      } else {
        // check previous added
        if ((newPosition > 0)
            && newKeyList[(newPosition - 1)].compareTo(key) >= 0) {
          int i = newPosition;
          do {
            i--;
            if (newKeyList[i].equals(key)) {
              newCurrentPosition = i;
              newCurrentExisting = true;
              if (subDataTypes != null) {
                return newSubCollectorListNextLevel[newCurrentPosition];
              } else {
                return null;
              }
            }
          } while ((i > 0) && (newKeyList[i].compareTo(key) > 0));
        }
        // move position in old list
        if (position < getSize()) {
          // just add smaller or equal items
          while (keyList[position].compareTo(key) <= 0) {
            if (newPosition == newSize) {
              increaseNewListSize();
            }
            // copy
            newKeyList[newPosition] = keyList[position];
            newSourceNumberList[newPosition] = sourceNumberList[position];
            newErrorNumber[newPosition] = errorNumber[position];
            newErrorList[newPosition] = errorList[position];
            if (hasSub) {
              newSubCollectorListNextLevel[newPosition] = subCollectorListNextLevel[position];
            }
            copyToNew(position, newPosition);
            newPosition++;
            position++;
            // check if added key from list is right key
            if (newKeyList[(newPosition - 1)].equals(key)) {
              if (increaseSourceNumber) {
                newSourceNumberList[(newPosition - 1)]++;
              }
              newCurrentPosition = newPosition - 1;
              newCurrentExisting = true;
              // register known key found again in segment
              newKnownKeyFoundInSegment.add(key);
              // ready
              if (hasSub) {
                return newSubCollectorListNextLevel[newCurrentPosition];
              } else {
                return null;
              }
              // stop if position exceeds size
            } else if (position == getSize()) {
              break;
            }
          }
        }
        // check size
        if (newPosition == newSize) {
          increaseNewListSize();
        }
        // add key
        newKeyList[newPosition] = key;
        newSourceNumberList[newPosition] = 1;
        newErrorNumber[newPosition] = 0;
        newErrorList[newPosition] = new HashMap<>();
        newPosition++;
        newCurrentPosition = newPosition - 1;
        newCurrentExisting = false;
        // ready, only handle sub
        if (hasSub) {
          newSubCollectorListNextLevel[newCurrentPosition] = DataCollector
              .getCollector(subCollectorTypes[0], subDataTypes[0],
                  subStatsTypes[0], subStatsItems[0], subSortTypes[0],
                  subSortDirections[0], subStart[0], subNumber[0],
                  newSubCollectorTypes, newSubDataTypes, newSubStatsTypes,
                  newSubStatsItems, newSubSortTypes, newSubSortDirections,
                  newSubStart, newSubNumber, segmentRegistration, null);
          return newSubCollectorListNextLevel[newCurrentPosition];
        } else {
          return null;
        }
      }
    } else {
      throw new IOException("already closed");
    }
  }
  
  /**
   * Adds the new from.
   *
   * @param key the key
   * @param otherSubCollector the other sub collector
   * @return the mtas data collector
   */
  protected final MtasDataCollector<?, ?> addNewFrom(String key, MtasDataCollector<?, ?> otherSubCollector) {
      hasSub = true;
      this.subCollectorTypes = new String[]{otherSubCollector.collectorType};
      this.subDataTypes = new String[]{otherSubCollector.dataType};
      this.subStatsTypes = new String[]{otherSubCollector.statsType};
      this.subStatsItems = (SortedSet<String>[]) new SortedSet<?>[] {otherSubCollector.statsItems};
      this.subSortTypes = new String[]{otherSubCollector.sortType};
      this.subSortDirections = new String[]{otherSubCollector.sortDirection};
      this.subStart = new Integer[]{otherSubCollector.start};
      this.subNumber = new Integer[]{otherSubCollector.number};
      this.newSubCollectorListNextLevel = new MtasDataCollector<?, ?>[1];
      
      newKeyList[newPosition] = key;
      newSourceNumberList[newPosition] = 1;
      newErrorNumber[newPosition] = 0;
      newErrorList[newPosition] = new HashMap<>();
      newCurrentPosition = newPosition - 1;
      newCurrentExisting = false;
      // ready, only handle sub
      try {
        newSubCollectorListNextLevel[newCurrentPosition] = DataCollector
            .getCollector(subCollectorTypes[0], subDataTypes[0],
                subStatsTypes[0], subStatsItems[0], subSortTypes[0],
                subSortDirections[0], subStart[0], subNumber[0],
                newSubCollectorTypes, newSubDataTypes, newSubStatsTypes,
                newSubStatsItems, newSubSortTypes, newSubSortDirections,
                newSubStart, newSubNumber, segmentRegistration, null);
        return newSubCollectorListNextLevel[newCurrentPosition];
      } catch (IOException e) {
        return null;
      }
  }

  /**
   * Copy to new.
   *
   * @param position the position
   * @param newPosition the new position
   */
  protected abstract void copyToNew(int position, int newPosition);

  /**
   * Copy from new.
   */
  protected abstract void copyFromNew();

  /**
   * Compare with boundary.
   *
   * @param value the value
   * @param boundary the boundary
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected abstract boolean compareWithBoundary(T1 value, T1 boundary)
      throws IOException;

  /**
   * Last for computing segment.
   *
   * @param value the value
   * @param boundary the boundary
   * @return the t1
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected abstract T1 lastForComputingSegment(T1 value, T1 boundary)
      throws IOException;

  /**
   * Last for computing segment.
   *
   * @return the t1
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected abstract T1 lastForComputingSegment() throws IOException;

  /**
   * Boundary for segment.
   *
   * @param segmentName the segment name
   * @return the t1
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected abstract T1 boundaryForSegment(String segmentName)
      throws IOException;

  /**
   * Boundary for segment computing.
   *
   * @param segmentName the segment name
   * @return the t1
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected abstract T1 boundaryForSegmentComputing(String segmentName)
      throws IOException;

  /**
   * String to boundary.
   *
   * @param boundary the boundary
   * @param segmentNumber the segment number
   * @return the t1
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected abstract T1 stringToBoundary(String boundary, Integer segmentNumber)
      throws IOException;

  /**
   * String to boundary.
   *
   * @param boundary the boundary
   * @return the t1
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected T1 stringToBoundary(String boundary) throws IOException {
    return stringToBoundary(boundary, null);
  }

  /**
   * Close segment key value registration.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void closeSegmentKeyValueRegistration() throws IOException {
    if (!closed) {
      if (segmentRegistration != null) {
        Map<String, T1> keyValueList = segmentKeyValueList.get(segmentName);
        T1 tmpSegmentValueBoundary = segmentValuesBoundary.get(segmentName);
        for (Entry<String, T1> entry : keyValueList.entrySet()) {
          if (tmpSegmentValueBoundary == null || compareWithBoundary(
              entry.getValue(), tmpSegmentValueBoundary)) {
            segmentKeys.add(entry.getKey());
          }
        }
      }
    } else {
      throw new IOException("already closed");
    }
  }

  /**
   * Recompute segment keys.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void recomputeSegmentKeys() throws IOException {
    if (!closed && segmentRegistration != null) {
      if (segmentRegistration.equals(SEGMENT_SORT_ASC)
          || segmentRegistration.equals(SEGMENT_SORT_DESC)
          || segmentRegistration.equals(SEGMENT_BOUNDARY_ASC)
          || segmentRegistration.equals(SEGMENT_BOUNDARY_DESC)) {

        if (segmentRegistration.equals(SEGMENT_SORT_ASC)
            || segmentRegistration.equals(SEGMENT_SORT_DESC)) {
          segmentKeys.clear();
          // recompute boundaries
          for (Entry<String, Map<String, T1>> entry : segmentKeyValueList
              .entrySet()) {
            T1 tmpSegmentValueBoundary = boundaryForSegment(entry.getKey());
            segmentValuesBoundary.put(entry.getKey(), tmpSegmentValueBoundary);
          }
          // compute adjusted boundaries and compute keys
          for (Entry<String, Map<String, T1>> entry : segmentKeyValueList
              .entrySet()) {
            this.segmentName = entry.getKey();
            Map<String, T1> keyValueList = entry.getValue();
            T1 tmpSegmentValueBoundaryForComputing = boundaryForSegmentComputing(
                entry.getKey());
            for (Entry<String, T1> subEntry : keyValueList.entrySet()) {
              if (tmpSegmentValueBoundaryForComputing == null
                  || compareWithBoundary(subEntry.getValue(),
                      tmpSegmentValueBoundaryForComputing)) {
                if (!segmentKeys.contains(subEntry.getKey())) {
                  segmentKeys.add(subEntry.getKey());
                }
              }
            }
          }
        }

        Map<String, T1> keyValueList;
        Set<String> recomputeKeyList;
        segmentRecomputeKeyList = new LinkedHashMap<>();
        for (String key : segmentKeys) {
          for (Entry<String, Map<String, T1>> entry : segmentKeyValueList
              .entrySet()) {
            keyValueList = entry.getValue();
            if (!keyValueList.containsKey(key)) {
              if (!segmentRecomputeKeyList.containsKey(entry.getKey())) {
                recomputeKeyList = new HashSet<>();
                segmentRecomputeKeyList.put(entry.getKey(), recomputeKeyList);
              } else {
                recomputeKeyList = segmentRecomputeKeyList.get(entry.getKey());
              }
              recomputeKeyList.add(key);
            }
          }
        }
        this.segmentName = null;
      } else {
        throw new IOException(
            "not for segmentRegistration " + segmentRegistration);
      }
    } else {
      throw new IOException("already closed or no segmentRegistration ("
          + segmentRegistration + ")");
    }
  }

  /**
   * Reduce to keys.
   *
   * @param keys the keys
   */
  public abstract void reduceToKeys(Set<String> keys);

  /**
   * Reduce to segment keys.
   */
  public void reduceToSegmentKeys() {
    if (segmentRegistration != null) {
      reduceToKeys(segmentKeys);
    }
  }

  /**
   * Check existence necessary keys.
   *
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public boolean checkExistenceNecessaryKeys() throws IOException {
    if (!closed) {
      if (segmentRegistration != null) {
        return segmentRecomputeKeyList.size() == 0;
      } else {
        return true;
      }
    } else {
      throw new IOException("already closed");
    }
  }

  /**
   * Validate segment boundary.
   *
   * @param o the o
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  abstract public boolean validateSegmentBoundary(Object o) throws IOException;

  /**
   * Validate with segment boundary.
   *
   * @param value the value
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected boolean validateWithSegmentBoundary(T1 value) throws IOException {
    if (!closed && segmentRegistration != null) {
      T1 tmpSegmentValueBoundary = segmentValuesBoundary.get(segmentName);
      if (tmpSegmentValueBoundary == null
          || compareWithBoundary(value, tmpSegmentValueBoundary)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Validate segment value.
   *
   * @param value the value
   * @param maximumNumber the maximum number
   * @param segmentNumber the segment number
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public String validateSegmentValue(T1 value, int maximumNumber,
      int segmentNumber) throws IOException {
    if (!closed) {
      if (segmentRegistration != null) {
        if (maximumNumber > 0) {
          T1 tmpSegmentValueBoundary = segmentValuesBoundary.get(segmentName);
          if (segmentValueTopList.size() < maximumNumber
              || compareWithBoundary(value, tmpSegmentValueBoundary)) {
            return SEGMENT_KEY_OR_NEW;
          } else if (segmentKeys.size() > newKnownKeyFoundInSegment.size()) {
            return SEGMENT_POSSIBLE_KEY;
          } else {
            return null;
          }
        } else {
          return null;
        }
      } else {
        return null;
      }
    } else {
      throw new IOException("already closed");
    }
  }

  /**
   * Validate segment value.
   *
   * @param key the key
   * @param value the value
   * @param maximumNumber the maximum number
   * @param segmentNumber the segment number
   * @param test the test
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public String validateSegmentValue(String key, T1 value, int maximumNumber,
      int segmentNumber, boolean test) throws IOException {
    if (!closed) {
      if (segmentRegistration != null) {
        if (maximumNumber > 0) {
          T1 tmpSegmentValueMaxListMin = segmentValueTopListLast
              .get(segmentName);
          T1 tmpSegmentValueBoundary = segmentValuesBoundary.get(segmentName);
          if (segmentValueTopList.size() < maximumNumber) {
            if (!test) {
              segmentKeyValueList.get(segmentName).put(key, value);
              segmentValueTopList.add(value);
              segmentValueTopListLast.put(segmentName,
                  (tmpSegmentValueMaxListMin == null) ? value
                      : lastForComputingSegment(tmpSegmentValueMaxListMin,
                          value));
              if (segmentValueTopList.size() == maximumNumber) {
                tmpSegmentValueMaxListMin = segmentValueTopListLast
                    .get(segmentName);
                segmentValueTopListLast.put(segmentName,
                    tmpSegmentValueMaxListMin);
                segmentValuesBoundary.put(segmentName,
                    boundaryForSegmentComputing(segmentName));
              }
            }
            return segmentKeys.contains(key) ? SEGMENT_KEY : SEGMENT_NEW;
          } else if (compareWithBoundary(value, tmpSegmentValueBoundary)) {
            // System.out.println(key+" "+value+" "+tmpSegmentValueBoundary);
            if (!test) {
              segmentKeyValueList.get(segmentName).put(key, value);
              if (compareWithBoundary(value, tmpSegmentValueMaxListMin)) {
                segmentValueTopList.add(value);
                segmentValueTopList.remove(tmpSegmentValueMaxListMin);
                tmpSegmentValueMaxListMin = lastForComputingSegment();
                segmentValueTopListLast.put(segmentName,
                    tmpSegmentValueMaxListMin);
                segmentValuesBoundary.put(segmentName,
                    boundaryForSegmentComputing(segmentName));
              }
            }
            return segmentKeys.contains(key) ? SEGMENT_KEY : SEGMENT_NEW;
          } else if (segmentKeys.contains(key)) {
            if (!test) {
              segmentKeyValueList.get(segmentName).put(key, value);
            }
            return SEGMENT_KEY;
          } else {
            return null;
          }
        } else {
          return null;
        }
      } else {
        return null;
      }
    } else {
      throw new IOException("already closed");
    }
  }

  /**
   * Sets the error.
   *
   * @param newPosition the new position
   * @param errorNumberItem the error number item
   * @param errorListItem the error list item
   * @param currentExisting the current existing
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected final void setError(int newPosition, int errorNumberItem,
      HashMap<String, Integer> errorListItem, boolean currentExisting)
      throws IOException {
    if (!closed) {
      if (currentExisting) {
        newErrorNumber[newPosition] += errorNumberItem;
        HashMap<String, Integer> item = newErrorList[newPosition];
        for (Entry<String, Integer> entry : errorListItem.entrySet()) {
          if (item.containsKey(entry.getKey())) {
            item.put(entry.getKey(),
                item.get(entry.getKey()) + entry.getValue());
          } else {
            item.put(entry.getKey(), entry.getValue());
          }
        }
      } else {
        newErrorNumber[newPosition] = errorNumberItem;
        newErrorList[newPosition] = errorListItem;
      }
    } else {
      throw new IOException("already closed");
    }
  }

  /**
   * Sorted and unique.
   *
   * @param keyList the key list
   * @param size the size
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private boolean sortedAndUnique(String[] keyList, int size)
      throws IOException {
    if (!closed) {
      for (int i = 1; i < size; i++) {
        if (keyList[(i - 1)].compareTo(keyList[i]) >= 0) {
          return false;
        }
      }
      return true;
    } else {
      throw new IOException("already closed");
    }
  }

  /**
   * Compute sort and unique mapping.
   *
   * @param keyList the key list
   * @param size the size
   * @return the int[][]
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private int[][] computeSortAndUniqueMapping(String[] keyList, int size)
      throws IOException {
    if (!closed) {
      if (size > 0) {
        SortedMap<String, int[]> sortedMap = new TreeMap<>();
        for (int i = 0; i < size; i++) {
          if (sortedMap.containsKey(keyList[i])) {
            int[] previousList = sortedMap.get(keyList[i]);
            int[] newList = new int[previousList.length + 1];
            System.arraycopy(previousList, 0, newList, 0, previousList.length);
            newList[previousList.length] = i;
            sortedMap.put(keyList[i], newList);
          } else {
            sortedMap.put(keyList[i], new int[] { i });
          }
        }
        Collection<int[]> values = sortedMap.values();
        int[][] result = new int[sortedMap.size()][];
        return values.toArray(result);
      } else {
        return null;
      }
    } else {
      throw new IOException("already closed");
    }
  }

  /**
   * Remap data.
   *
   * @param mapping the mapping
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected void remapData(int[][] mapping) throws IOException {
    if (!closed) {
      // remap and merge keys
      String[] newKeyList = new String[mapping.length];
      // process mapping for functions?
      int[] newSourceNumberList = new int[mapping.length];
      int[] newErrorNumber = new int[mapping.length];
      @SuppressWarnings("unchecked")
      HashMap<String, Integer>[] newErrorList = (HashMap<String, Integer>[]) new HashMap<?, ?>[mapping.length];
      for (int i = 0; i < mapping.length; i++) {
        newKeyList[i] = keyList[mapping[i][0]];
        newSourceNumberList[i] = sourceNumberList[mapping[i][0]];
        for (int j = 0; j < mapping[i].length; j++) {
          if (j == 0) {
            newErrorNumber[i] = errorNumber[mapping[i][j]];
            newErrorList[i] = errorList[mapping[i][j]];
          } else {
            newErrorNumber[i] += errorNumber[mapping[i][j]];
            for (Entry<String, Integer> entry : errorList[mapping[i][j]]
                .entrySet()) {
              if (newErrorList[i].containsKey(entry.getKey())) {
                newErrorList[i].put(entry.getKey(),
                    newErrorList[i].get(entry.getKey()) + entry.getValue());
              } else {
                newErrorList[i].put(entry.getKey(), entry.getValue());
              }
            }
          }
        }
      }
      if (hasSub) {
        newSubCollectorListNextLevel = new MtasDataCollector<?, ?>[mapping.length];
        for (int i = 0; i < mapping.length; i++) {
          for (int j = 0; j < mapping[i].length; j++) {
            if (j == 0 || newSubCollectorListNextLevel[i] == null) {
              newSubCollectorListNextLevel[i] = subCollectorListNextLevel[mapping[i][j]];
            } else {
              newSubCollectorListNextLevel[i]
                  .merge(subCollectorListNextLevel[mapping[i][j]], false);
            }
          }
        }
        subCollectorListNextLevel = newSubCollectorListNextLevel;
      }
      keyList = newKeyList;
      sourceNumberList = newSourceNumberList;
      errorNumber = newErrorNumber;
      errorList = newErrorList;
      size = keyList.length;
      position = 0;
    } else {
      throw new IOException("already closed");
    }
  }

  /**
   * Close new list.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void closeNewList() throws IOException {
    if (!closed) {
      if (segmentRegistration != null) {
        this.segmentName = null;
      }
      if (newSize > 0) {
        // add remaining old
        while (position < getSize()) {
          if (newPosition == newSize) {
            increaseNewListSize();
          }
          newKeyList[newPosition] = keyList[position];
          newSourceNumberList[newPosition] = sourceNumberList[position];
          newErrorNumber[newPosition] = errorNumber[position];
          newErrorList[newPosition] = errorList[position];
          if (hasSub) {
            newSubCollectorListNextLevel[newPosition] = subCollectorListNextLevel[position];
          }
          copyToNew(position, newPosition);
          position++;
          newPosition++;
        }
        // copy
        keyList = newKeyList;
        sourceNumberList = newSourceNumberList;
        errorNumber = newErrorNumber;
        errorList = newErrorList;
        subCollectorListNextLevel = newSubCollectorListNextLevel;
        copyFromNew();
        size = newPosition;
        // sort and merge
        if (!sortedAndUnique(keyList, getSize())) {
          remapData(computeSortAndUniqueMapping(keyList, getSize()));
        }
      }
      position = 0;
      newSize = 0;
      newPosition = 0;
      newCurrentPosition = 0;
    }
  }

  /**
   * Gets the item.
   *
   * @param i the i
   * @return the item
   */
  abstract protected MtasDataItem<T1, T2> getItem(int i);

  /**
   * Checks for sub.
   *
   * @return true, if successful
   */
  protected boolean hasSub() {
    return hasSub;
  }

  /**
   * Error.
   *
   * @param error the error
   * @param number the number of occurrences   
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public abstract void error(String error, int number) throws IOException;

  /**
   * Error.
   *
   * @param key the key
   * @param error the error
   * @param number the number of occurrences
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public abstract void error(String key, String error, int number) throws IOException;

  /**
   * Adds the.
   *
   * @param valueSum the value sum
   * @param valueN the value N
   * @return the mtas data collector
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public abstract MtasDataCollector add(long valueSum, long valueN)
      throws IOException;

  /**
   * Adds the.
   *
   * @param values the values
   * @param number the number
   * @return the mtas data collector
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public abstract MtasDataCollector add(long[] values, int number)
      throws IOException;

  /**
   * Adds the.
   *
   * @param valueSum the value sum
   * @param valueN the value N
   * @return the mtas data collector
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public abstract MtasDataCollector add(double valueSum, long valueN)
      throws IOException;

  /**
   * Adds the.
   *
   * @param values the values
   * @param number the number
   * @return the mtas data collector
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public abstract MtasDataCollector add(double[] values, int number)
      throws IOException;

  /**
   * Adds the.
   *
   * @param key the key
   * @param valueSum the value sum
   * @param valueN the value N
   * @return the mtas data collector
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public abstract MtasDataCollector add(String key, long valueSum, long valueN)
      throws IOException;

  /**
   * Adds the.
   *
   * @param key the key
   * @param values the values
   * @param number the number
   * @return the mtas data collector
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public abstract MtasDataCollector add(String key, long[] values, int number)
      throws IOException;

  /**
   * Adds the.
   *
   * @param key the key
   * @param valueSum the value sum
   * @param valueN the value N
   * @return the mtas data collector
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public abstract MtasDataCollector add(String key, double valueSum,
      long valueN) throws IOException;

  /**
   * Adds the.
   *
   * @param key the key
   * @param values the values
   * @param number the number
   * @return the mtas data collector
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public abstract MtasDataCollector add(String key, double[] values, int number)
      throws IOException;

  /**
   * To string.
   *
   * @return the string
   */
  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder text = new StringBuilder();
    text.append(this.getClass().getSimpleName() + "-" + this.hashCode() + "\n");
    text.append("\t=== " + collectorType + " - " + statsType + " " + statsItems
        + " " + hasSub + " ===\n");
    text.append("\tclosed: " + closed + "\n");
    text.append("\tkeylist: " + Arrays.asList(keyList) + "\n");
    text.append("\tsegmentKeys: "
        + (segmentKeys != null ? segmentKeys.contains("1") : "null") + "\n");
    return text.toString().trim();
  }

  /**
   * Gets the result.
   *
   * @return the result
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public MtasDataCollectorResult<T1, T2> getResult() throws IOException {
    if (!closed) {
      close();
    }
    return result;
  }

  /**
   * Gets the key list.
   *
   * @return the key list
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Set<String> getKeyList() throws IOException {
    if (!closed) {
      close();
    }
    return new HashSet<>(Arrays.asList(keyList));
  }

  /**
   * Gets the stats items.
   *
   * @return the stats items
   */
  public SortedSet<String> getStatsItems() {
    return statsItems;
  }

  /**
   * Close.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void close() throws IOException {
    if (!closed) {
      closeNewList();
      if (collectorType.equals(DataCollector.COLLECTOR_TYPE_LIST)) {
        // compute initial basic list
        TreeMap<String, MtasDataItem<T1, T2>> basicList = new TreeMap<>();
        for (int i = 0; i < getSize(); i++) {
          MtasDataItem<T1, T2> newItem = getItem(i);
          if (basicList.containsKey(keyList[i])) {
            newItem.add(basicList.get(keyList[i]));
          }
          basicList.put(keyList[i], newItem);
        }
        // create result based on basic list
        result = new MtasDataCollectorResult<>(collectorType, sortType,
            sortDirection, basicList, start, number);
        // reduce
        if (segmentRegistration != null) {
          if (segmentRegistration.equals(SEGMENT_SORT_ASC)
              || segmentRegistration.equals(SEGMENT_SORT_DESC)) {
            reduceToKeys(result.getComparatorList().keySet());
          } else if (segmentRegistration.equals(SEGMENT_BOUNDARY_ASC)
              || segmentRegistration.equals(SEGMENT_BOUNDARY_DESC)) {
            Map<String, MtasDataItemNumberComparator> comparatorList = result
                .getComparatorList();
            HashSet<String> filteredKeySet = new HashSet<>();
            if (segmentRegistration.equals(SEGMENT_BOUNDARY_ASC)) {
              for (Entry<String, MtasDataItemNumberComparator> entry : comparatorList
                  .entrySet()) {
                if (entry.getValue().compareTo(segmentValueBoundary) < 0) {
                  filteredKeySet.add(entry.getKey());
                }
              }
            } else {
              for (Entry<String, MtasDataItemNumberComparator> entry : comparatorList
                  .entrySet()) {
                if (entry.getValue().compareTo(segmentValueBoundary) > 0) {
                  filteredKeySet.add(entry.getKey());
                }
              }
            }
            reduceToKeys(filteredKeySet);
            basicList.keySet().retainAll(filteredKeySet);
            result = new MtasDataCollectorResult<>(collectorType, sortType,
                sortDirection, basicList, start, number);
          }
        }
      } else if (collectorType.equals(DataCollector.COLLECTOR_TYPE_DATA)) {
        if (getSize() > 0) {
          result = new MtasDataCollectorResult<>(collectorType, getItem(0));
        } else {
          result = new MtasDataCollectorResult<>(collectorType, sortType,
              sortDirection);
        }
      } else {
        throw new IOException("type " + collectorType + " not supported");
      }
      closed = true;
    }
  }

  /**
   * Gets the collector type.
   *
   * @return the collector type
   */
  public String getCollectorType() {
    return collectorType;
  }

  /**
   * Gets the stats type.
   *
   * @return the stats type
   */
  public String getStatsType() {
    return statsType;
  }

  /**
   * Gets the data type.
   *
   * @return the data type
   */
  public String getDataType() {
    return dataType;
  }

  /**
   * Gets the size.
   *
   * @return the size
   */
  public int getSize() {
    return size;
  }

  /**
   * With total.
   *
   * @return true, if successful
   */
  public boolean withTotal() {
    return withTotal;
  }

  /**
   * Sets the with total.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void setWithTotal() throws IOException {
    if (collectorType.equals(DataCollector.COLLECTOR_TYPE_LIST)) {
      if (segmentName != null) {
        throw new IOException("can't get total with segmentRegistration");
      } else {
        withTotal = true;
      }
    } else {
      throw new IOException(
          "can't get total for dataCollector of type " + collectorType);
    }
  }


}