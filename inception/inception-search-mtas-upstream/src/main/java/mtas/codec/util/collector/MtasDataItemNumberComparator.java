package mtas.codec.util.collector;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import mtas.codec.util.CodecUtil;

/**
 * The Class MtasDataItemNumberComparator.
 *
 * @param <T> the generic type
 */
public final class MtasDataItemNumberComparator<T extends Number & Comparable<T>>
    implements Comparable<T>, Serializable, Cloneable {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /** The value. */
  T value;

  /** The sort direction. */
  String sortDirection;

  /**
   * Instantiates a new mtas data item number comparator.
   *
   * @param value the value
   * @param sortDirection the sort direction
   */
  public MtasDataItemNumberComparator(T value, String sortDirection) {
    this.value = value;
    this.sortDirection = sortDirection;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#clone()
   */
  @Override
  public MtasDataItemNumberComparator<T> clone() {
    return new MtasDataItemNumberComparator<>(this.value, this.sortDirection);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(T compareValue) {
    return value.compareTo(compareValue);
  }

  /**
   * Gets the value.
   *
   * @return the value
   */
  public T getValue() {
    return value;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return value.toString();
  }

  /**
   * Adds the.
   *
   * @param newValue the new value
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("unchecked")
  public void add(T newValue) throws IOException {
    if (value instanceof Integer && newValue instanceof Integer) {
      value = (T) Integer.valueOf(value.intValue() + newValue.intValue());
    } else if (value instanceof Long && newValue instanceof Long) {
      value = (T) Long.valueOf(value.longValue() + newValue.longValue());
    } else if (value instanceof Float && newValue instanceof Float) {
      value = (T) Float.valueOf(value.floatValue() + newValue.floatValue());
    } else if (value instanceof Double && newValue instanceof Double) {
      value = (T) Double.valueOf(value.doubleValue() + newValue.longValue());
    } else {
      throw new IOException("incompatible NumberComparators");
    }
  }

  /**
   * Subtract.
   *
   * @param newValue the new value
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("unchecked")
  public void subtract(T newValue) throws IOException {
    if (value instanceof Integer && newValue instanceof Integer) {
      value = (T) Integer.valueOf(value.intValue() - newValue.intValue());
    } else if (value instanceof Long && newValue instanceof Long) {
      value = (T) Long.valueOf(value.longValue() - newValue.longValue());
    } else if (value instanceof Float && newValue instanceof Float) {
      value = (T) Float.valueOf(value.floatValue() - newValue.floatValue());
    } else if (value instanceof Double && newValue instanceof Double) {
      value = (T) Double.valueOf(value.doubleValue() - newValue.longValue());
    } else {
      throw new IOException("incompatible NumberComparators");
    }
  }

  /**
   * Recompute boundary.
   *
   * @param n the n
   * @return the mtas data item number comparator
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public MtasDataItemNumberComparator<T> recomputeBoundary(int n)
      throws IOException {
    if (sortDirection.equals(CodecUtil.SORT_DESC)) {
      if (value instanceof Integer) {
        return new MtasDataItemNumberComparator(
            Math.floorDiv((Integer) value, n), sortDirection);
      } else if (value instanceof Long) {
        return new MtasDataItemNumberComparator(Math.floorDiv((Long) value, n),
            sortDirection);
      } else if (value instanceof Float) {
        return new MtasDataItemNumberComparator(((Float) value) / n,
            sortDirection);
      } else if (value instanceof Double) {
        return new MtasDataItemNumberComparator(((Double) value) / n,
            sortDirection);
      } else {
        throw new IOException("unknown NumberComparator");
      }
    } else if (sortDirection.equals(CodecUtil.SORT_ASC)) {
      return new MtasDataItemNumberComparator(getValue(), sortDirection);
    } else {
      throw new IOException("unknown sortDirection " + sortDirection);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MtasDataItemNumberComparator<?> that = (MtasDataItemNumberComparator<?>) obj;
    return value.equals(that.value);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.getClass().getSimpleName(), value, sortDirection);   
  }

}