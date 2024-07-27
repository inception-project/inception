package mtas.codec.util.heatmap;

import java.util.Map;
import java.util.Map.Entry;

/**
 * The Class CellValues.
 */
public class CellValues {

  /** The values. */
  private long[] values;
  
  /** The function values long. */
  // for now, split into double and long
  private long[][] functionValuesLong;
  
  /** The function values double. */
  private double[][] functionValuesDouble;
  
  /** The function values error. */
  private Map<String, Integer>[] functionValuesError;

  /**
   * Instantiates a new cell values.
   *
   * @param values the values
   * @param functionValuesLong the function values long
   * @param functionValuesDouble the function values double
   * @param functionValuesError the function values error
   */
  public CellValues(long[] values, long[][] functionValuesLong, double[][] functionValuesDouble,
      Map<String, Integer>[] functionValuesError) {
    this.values = values.clone();
    if (functionValuesLong == null) {
      this.functionValuesLong = new long[0][];
    } else {
      this.functionValuesLong = new long[functionValuesLong.length][];
      for(int i=0; i<functionValuesLong.length; i++) {
        if(functionValuesLong[i]==null) {
          this.functionValuesLong[i] = null;
        } else {
          this.functionValuesLong[i] = functionValuesLong[i].clone();
        }
      }
    }
    if (functionValuesDouble == null) {
      this.functionValuesDouble = new double[0][];
    } else {
      this.functionValuesDouble = new double[functionValuesDouble.length][];
      for(int i=0; i<functionValuesDouble.length; i++) {
        if(functionValuesDouble[i]==null) {
          this.functionValuesDouble[i] = null;
        } else {
          this.functionValuesDouble[i] = functionValuesDouble[i].clone();
        }
      }
    }
    if (functionValuesError == null) {
      this.functionValuesError = new Map[0];
    } else {
      this.functionValuesError = functionValuesError.clone();
    }
  }

  /**
   * Merge.
   *
   * @param newCellValues the new cell values
   */
  // Merge with other (same size) CellValues
  public void merge(CellValues newCellValues) {
    long[] newValues = new long[values.length + newCellValues.values.length];
    long[][] newFunctionValuesLong = new long[functionValuesLong.length][];
    double[][] newFunctionValuesDouble = new double[functionValuesDouble.length][];
    // merge values
    System.arraycopy(values, 0, newValues, 0, values.length);
    System.arraycopy(newCellValues.values, 0, newValues, values.length, newCellValues.values.length);
    // merge function values long
    for (int i = 0; i < functionValuesLong.length; i++) {
      newFunctionValuesLong[i] = new long[functionValuesLong[i].length + newCellValues.functionValuesLong[i].length];
      System.arraycopy(functionValuesLong[i], 0, newFunctionValuesLong[i], 0, functionValuesLong[i].length);
      System.arraycopy(newCellValues.functionValuesLong[i], 0, newFunctionValuesLong[i], functionValuesLong[i].length,
          newCellValues.functionValuesLong[i].length);
    }
    // merge function values double
    for (int i = 0; i < functionValuesDouble.length; i++) {
      newFunctionValuesDouble[i] = new double[functionValuesDouble[i].length
          + newCellValues.functionValuesDouble[i].length];
      System.arraycopy(functionValuesDouble[i], 0, newFunctionValuesDouble[i], 0, functionValuesDouble[i].length);
      System.arraycopy(newCellValues.functionValuesDouble[i], 0, newFunctionValuesDouble[i],
          functionValuesDouble[i].length, newCellValues.functionValuesDouble[i].length);
    }
    // merge function value errors
    for (int i = 0; i < functionValuesError.length; i++) {
      for (Entry<String, Integer> entry : newCellValues.functionValuesError[i].entrySet()) {
        if (functionValuesError[i].containsKey(entry.getKey())) {
          functionValuesError[i].put(entry.getKey(), functionValuesError[i].get(entry.getKey()) + entry.getValue());
        } else {
          functionValuesError[i].put(entry.getKey(), entry.getValue());
        }
      }
    }
    // update
    values = newValues;
    functionValuesLong = newFunctionValuesLong;
    functionValuesDouble = newFunctionValuesDouble;
  }

  /**
   * Values length.
   *
   * @return the int
   */
  public int valuesLength() {
    return values.length;
  }

  /**
   * Values.
   *
   * @return the long[]
   */
  public long[] values() {
    return values.clone();
  }

  /**
   * Function values long length.
   *
   * @return the int
   */
  public int functionValuesLongLength() {
    return functionValuesLong.length;
  }

  /**
   * Function values long length.
   *
   * @param i the i
   * @return the int
   */
  public int functionValuesLongLength(int i) {
    return functionValuesLong[i].length;
  }

  /**
   * Function values long.
   *
   * @param i the i
   * @return the long[]
   */
  public long[] functionValuesLong(int i) {
    return functionValuesLong[i];
  }

  /**
   * Function values double length.
   *
   * @return the int
   */
  public int functionValuesDoubleLength() {
    return functionValuesDouble.length;
  }

  /**
   * Function values double length.
   *
   * @param i the i
   * @return the int
   */
  public int functionValuesDoubleLength(int i) {
    return functionValuesDouble[i].length;
  }

  /**
   * Function values double.
   *
   * @param i the i
   * @return the double[]
   */
  public double[] functionValuesDouble(int i) {
    return functionValuesDouble[i];
  }

  /**
   * Function values error.
   *
   * @param i the i
   * @return the map
   */
  public Map<String, Integer> functionValuesError(int i) {
    return functionValuesError[i];
  }

}
