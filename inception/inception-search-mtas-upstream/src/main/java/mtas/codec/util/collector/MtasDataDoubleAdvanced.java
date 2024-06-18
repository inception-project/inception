package mtas.codec.util.collector;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;

import mtas.codec.util.CodecUtil;

/**
 * The Class MtasDataDoubleAdvanced.
 */
public class MtasDataDoubleAdvanced extends MtasDataAdvanced<Double, Double> {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /**
   * Instantiates a new mtas data double advanced.
   *
   * @param collectorType the collector type
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
  public MtasDataDoubleAdvanced(String collectorType,
      SortedSet<String> statsItems, String sortType, String sortDirection,
      Integer start, Integer number, String[] subCollectorTypes,
      String[] subDataTypes, String[] subStatsTypes,
      SortedSet<String>[] subStatsItems, String[] subSortTypes,
      String[] subSortDirections, Integer[] subStart, Integer[] subNumber,
      String segmentRegistration, String boundary) throws IOException {
    super(collectorType, CodecUtil.DATA_TYPE_DOUBLE, statsItems, sortType,
        sortDirection, start, number, subCollectorTypes, subDataTypes,
        subStatsTypes, subStatsItems, subSortTypes, subSortDirections, subStart,
        subNumber, new MtasDataDoubleOperations(), segmentRegistration,
        boundary);
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.codec.util.DataCollector.MtasDataCollector#getItem(int)
   */
  @Override
  protected final MtasDataItemDoubleAdvanced getItem(int i) {
    if (i >= 0 && i < size) {
      return new MtasDataItemDoubleAdvanced(advancedValueSumList[i],
          advancedValueSumOfLogsList[i], advancedValueSumOfSquaresList[i],
          advancedValueMinList[i], advancedValueMaxList[i],
          advancedValueNList[i], hasSub() ? subCollectorListNextLevel[i] : null,
          getStatsItems(), sortType, sortDirection, errorNumber[i],
          errorList[i], sourceNumberList[i]);
    } else {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.codec.util.DataCollector.MtasDataCollector#add(long, long)
   */
  @Override
  public MtasDataCollector<?, ?> add(long valueSum, long valueN)
      throws IOException {
    throw new IOException("not supported");
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.codec.util.DataCollector.MtasDataCollector#add(long[], int)
   */
  @Override
  public MtasDataCollector<?, ?> add(long[] values, int number)
      throws IOException {
    MtasDataCollector<?, ?> dataCollector = getSubCollector(false);
    Double[] newValues = new Double[number];
    for (int i = 0; i < values.length; i++)
      newValues[i] = Long.valueOf(values[i]).doubleValue();
    setValue(newCurrentPosition, newValues, number, newCurrentExisting);
    return dataCollector;
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.codec.util.DataCollector.MtasDataCollector#add(double, long)
   */
  @Override
  public MtasDataCollector<?, ?> add(double valueSum, long valueN)
      throws IOException {
    throw new IOException("not supported");
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.codec.util.DataCollector.MtasDataCollector#add(double[], int)
   */
  @Override
  public MtasDataCollector<?, ?> add(double[] values, int number)
      throws IOException {
    MtasDataCollector<?, ?> dataCollector = getSubCollector(false);
    Double[] objectValues = Arrays.stream(values).boxed().toArray(Double[]::new);
    setValue(newCurrentPosition, objectValues, number,
        newCurrentExisting);
    return dataCollector;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.codec.util.DataCollector.MtasDataCollector#add(java.lang.String[],
   * long, long)
   */
  @Override
  public MtasDataCollector<?, ?> add(String key, long valueSum, long valueN)
      throws IOException {
    throw new IOException("not supported");
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.codec.util.DataCollector.MtasDataCollector#add(java.lang.String[],
   * long[], int)
   */
  @Override
  public MtasDataCollector<?, ?> add(String key, long[] values, int number)
      throws IOException {
    if (key != null) {
      Double[] newValues = new Double[number];
      for (int i = 0; i < values.length; i++)
        newValues[i] = Long.valueOf(values[i]).doubleValue();
      MtasDataCollector<?, ?> subCollector = getSubCollector(key, false);
      setValue(newCurrentPosition, newValues, number, newCurrentExisting);
      return subCollector;
    } else {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.codec.util.DataCollector.MtasDataCollector#add(java.lang.String[],
   * double, long)
   */
  @Override
  public MtasDataCollector<?, ?> add(String key, double valueSum, long valueN)
      throws IOException {
    throw new IOException("not supported");
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.codec.util.DataCollector.MtasDataCollector#add(java.lang.String[],
   * double[], int)
   */
  @Override
  public MtasDataCollector<?, ?> add(String key, double[] values, int number)
      throws IOException {
    if (key != null) {
      MtasDataCollector<?, ?> subCollector = getSubCollector(key, false);
      Double[] objectValues = Arrays.stream(values).boxed().toArray(Double[]::new);
      setValue(newCurrentPosition, objectValues, number,
          newCurrentExisting);
      return subCollector;
    } else {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.codec.util.DataCollector.MtasDataCollector#compareForComputingSegment(
   * java.lang.Number, java.lang.Number)
   */
  @Override
  protected boolean compareWithBoundary(Double value, Double boundary)
      throws IOException {
    if (segmentRegistration.equals(SEGMENT_SORT_ASC)
        || segmentRegistration.equals(SEGMENT_BOUNDARY_ASC)) {
      return value <= boundary;
    } else if (segmentRegistration.equals(SEGMENT_SORT_DESC)
        || segmentRegistration.equals(SEGMENT_BOUNDARY_DESC)) {
      return value >= boundary;
    } else {
      throw new IOException(
          "can't compare for segmentRegistration " + segmentRegistration);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.codec.util.DataCollector.MtasDataCollector#minimumForComputingSegment(
   * java.lang.Number, java.lang.Number)
   */
  @Override
  protected Double lastForComputingSegment(Double value, Double boundary)
      throws IOException {
    if (segmentRegistration.equals(SEGMENT_SORT_ASC)
        || segmentRegistration.equals(SEGMENT_BOUNDARY_ASC)) {
      return Math.max(value, boundary);
    } else if (segmentRegistration.equals(SEGMENT_SORT_DESC)
        || segmentRegistration.equals(SEGMENT_BOUNDARY_DESC)) {
      return Math.min(value, boundary);
    } else {
      throw new IOException(
          "can't compute last for segmentRegistration " + segmentRegistration);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.codec.util.DataCollector.MtasDataCollector#minimumForComputingSegment(
   * )
   */
  @Override
  protected Double lastForComputingSegment() throws IOException {
    if (segmentRegistration.equals(SEGMENT_SORT_ASC)
        || segmentRegistration.equals(SEGMENT_BOUNDARY_ASC)) {
      return Collections.max(segmentValueTopList);
    } else if (segmentRegistration.equals(SEGMENT_SORT_DESC)
        || segmentRegistration.equals(SEGMENT_BOUNDARY_DESC)) {
      return Collections.min(segmentValueTopList);
    } else {
      throw new IOException(
          "can't compute last for segmentRegistration " + segmentRegistration);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.codec.util.DataCollector.MtasDataCollector#boundaryForComputingSegment
   * ()
   */
  @Override
  protected Double boundaryForSegmentComputing(String segmentName)
      throws IOException {
    if (segmentRegistration.equals(SEGMENT_SORT_ASC)
        || segmentRegistration.equals(SEGMENT_SORT_DESC)) {
      Double boundary = boundaryForSegment(segmentName);
      if (boundary == null) {
        return null;
      } else {
        if (segmentRegistration.equals(SEGMENT_SORT_DESC)) {
          long correctionBoundary = 0;
          for (String otherSegmentName : segmentValueTopListLast.keySet()) {
            if (!otherSegmentName.equals(segmentName)) {
              Double otherBoundary = segmentValuesBoundary
                  .get(otherSegmentName);
              if (otherBoundary != null) {
                correctionBoundary += Math.max(0, otherBoundary - boundary);
              }
            }
          }
          return boundary + correctionBoundary;
        } else {
          return boundary;
        }
      }
    } else {
      throw new IOException("can't compute boundary for segmentRegistration "
          + segmentRegistration);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.codec.util.DataCollector.MtasDataCollector#boundaryForSegment()
   */
  @Override
  protected Double boundaryForSegment(String segmentName) throws IOException {
    if (segmentRegistration.equals(SEGMENT_SORT_ASC)
        || segmentRegistration.equals(SEGMENT_SORT_DESC)) {
      Double thisLast = segmentValueTopListLast.get(segmentName);
      if (thisLast == null) {
        return null;
      } else if (segmentRegistration.equals(SEGMENT_SORT_ASC)) {
        return thisLast * segmentNumber;
      } else {
        return thisLast / segmentNumber;
      }
    } else {
      throw new IOException("can't compute boundary for segmentRegistration "
          + segmentRegistration);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.codec.util.collector.MtasDataCollector#stringToBoundary(java.lang.
   * String, java.lang.Integer)
   */
  @Override
  protected Double stringToBoundary(String boundary, Integer segmentNumber)
      throws IOException {
    if (segmentRegistration.equals(SEGMENT_BOUNDARY_ASC)
        || segmentRegistration.equals(SEGMENT_BOUNDARY_DESC)) {
      if (segmentNumber == null) {
        return Double.valueOf(boundary);
      } else {
        return Double.valueOf(boundary) / segmentNumber;
      }
    } else {
      throw new IOException(
          "not available for segmentRegistration " + segmentRegistration);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.codec.util.collector.MtasDataCollector#validateSegmentBoundary(java.
   * lang.Object)
   */
  @Override
  public boolean validateSegmentBoundary(Object o) throws IOException {
    if (o instanceof Double) {
      return validateWithSegmentBoundary((Double) o);
    } else {
      throw new IOException("incorrect type");
    }
  }

}
