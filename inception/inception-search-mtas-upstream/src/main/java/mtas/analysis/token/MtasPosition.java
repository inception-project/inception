package mtas.analysis.token;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * The Class MtasPosition.
 */
public class MtasPosition {

  /** The Constant POSITION_SINGLE. */
  public static final String POSITION_SINGLE = "single";

  /** The Constant POSITION_RANGE. */
  public static final String POSITION_RANGE = "range";

  /** The Constant POSITION_SET. */
  public static final String POSITION_SET = "set";

  /** The mtas position type. */
  private String mtasPositionType;

  /** The mtas position start. */
  private int mtasPositionStart;

  /** The mtas position end. */
  private int mtasPositionEnd;

  /** The mtas position list. */
  private int[] mtasPositionList = null;

  /**
   * Instantiates a new mtas position.
   *
   * @param position the position
   */
  public MtasPosition(int position) {
    mtasPositionType = POSITION_SINGLE;
    mtasPositionStart = position;
  }

  /**
   * Instantiates a new mtas position.
   *
   * @param start the start
   * @param end the end
   */
  public MtasPosition(int start, int end) {
    if (start == end) {
      mtasPositionType = POSITION_SINGLE;
      mtasPositionStart = start;
    } else {
      mtasPositionType = POSITION_RANGE;
      mtasPositionStart = start;
      mtasPositionEnd = end;
    }
  }

  /**
   * Instantiates a new mtas position.
   *
   * @param positions the positions
   */
  public MtasPosition(int[] positions) {
    SortedSet<Integer> list = new TreeSet<>();
    for (int p : positions) {
      list.add(p);
    }
    if (list.size() == 1) {
      mtasPositionType = POSITION_SINGLE;
      mtasPositionStart = list.first();
    } else {
      mtasPositionType = POSITION_SET;
      mtasPositionList = list.stream().mapToInt(Number::intValue).toArray();
      mtasPositionStart = list.first();
      mtasPositionEnd = list.last();
      if (mtasPositionList.length == (1 + mtasPositionEnd
          - mtasPositionStart)) {
        mtasPositionType = POSITION_RANGE;
        mtasPositionList = null;
      }
    }
  }

  /**
   * Check type.
   *
   * @param type the type
   * @return the boolean
   */
  public Boolean checkType(String type) {
    if (mtasPositionType == null) {
      return false;
    } else {
      return mtasPositionType.equals(type);
    }
  }

  /**
   * Gets the start.
   *
   * @return the start
   */
  public Integer getStart() {
    return mtasPositionType == null ? null : mtasPositionStart;
  }

  /**
   * Gets the end.
   *
   * @return the end
   */
  public Integer getEnd() {
    if (mtasPositionType.equals(POSITION_RANGE)
        || mtasPositionType.equals(POSITION_SET)) {
      return mtasPositionEnd;
    } else if (mtasPositionType.equals(POSITION_SINGLE)) {
      return mtasPositionStart;
    } else {
      return null;
    }
  }

  /**
   * Gets the positions.
   *
   * @return the positions
   */
  public int[] getPositions() {
    return (mtasPositionType.equals(POSITION_SET))
        ? (int[]) mtasPositionList.clone() : null;
  }

  /**
   * Gets the length.
   *
   * @return the length
   */
  public Integer getLength() {
    if (mtasPositionType.equals(POSITION_SINGLE)) {
      return 1;
    } else if (mtasPositionType.equals(POSITION_RANGE)
        || mtasPositionType.equals(POSITION_SET)) {
      return 1 + mtasPositionEnd - mtasPositionStart;
    } else {
      return null;
    }
  }

  /**
   * Adds the.
   *
   * @param positions the positions
   */
  public void add(int[] positions) {
    SortedSet<Integer> list = new TreeSet<>();
    for (int p : positions) {
      list.add(p);
    }
    if (mtasPositionType.equals(POSITION_SINGLE)) {
      mtasPositionType = POSITION_SET;
      list.add(mtasPositionStart);
    } else if (mtasPositionType.equals(POSITION_RANGE)) {
      mtasPositionType = POSITION_SET;
      for (int i = mtasPositionStart; i <= mtasPositionEnd; i++) {
        list.add(i);
      }
    } else if (mtasPositionType.equals(POSITION_SET)) {
      for (int p : mtasPositionList) {
        list.add(p);
      }
    }
    mtasPositionList = list.stream().mapToInt(Number::intValue).toArray();
    mtasPositionStart = list.first();
    mtasPositionEnd = list.last();
    if (list.size() == 1) {
      mtasPositionType = POSITION_SINGLE;
      mtasPositionList = null;
    } else if (list.size() == (1 + mtasPositionEnd - mtasPositionStart)) {
      mtasPositionType = POSITION_RANGE;
      mtasPositionList = null;
    }
  }

  /**
   * Adds the.
   *
   * @param position the position
   */
  public void add(int position) {
    if (mtasPositionType.equals(POSITION_SINGLE)) {
      if (position != mtasPositionStart) {
        if (position == (mtasPositionStart + 1)) {
          mtasPositionType = POSITION_RANGE;
          mtasPositionEnd = position;
        } else if (position == (mtasPositionStart - 1)) {
          mtasPositionType = POSITION_RANGE;
          mtasPositionEnd = mtasPositionStart;
          mtasPositionStart = position;
        } else {
          mtasPositionType = POSITION_SET;
          SortedSet<Integer> list = new TreeSet<>();
          list.add(position);
          list.add(mtasPositionStart);
          mtasPositionList = list.stream().mapToInt(Number::intValue).toArray();
          mtasPositionStart = list.first();
          mtasPositionEnd = list.last();
        }
      }
    } else {
      SortedSet<Integer> list = new TreeSet<>();
      if (mtasPositionType.equals(POSITION_RANGE)) {
        mtasPositionType = POSITION_SET;
        for (int i = mtasPositionStart; i <= mtasPositionEnd; i++) {
          list.add(i);
        }
        list.add(position);
      } else if (mtasPositionType.equals(POSITION_SET)) {
        for (int p : mtasPositionList) {
          list.add(p);
        }
        list.add(position);
      }
      mtasPositionList = list.stream().mapToInt(Number::intValue).toArray();
      mtasPositionStart = list.first();
      mtasPositionEnd = list.last();
      if (list.size() == (1 + mtasPositionEnd - mtasPositionStart)) {
        mtasPositionType = POSITION_RANGE;
        mtasPositionList = null;
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (mtasPositionType == null) {
      return "[null]";
    } else if (mtasPositionType.equals(POSITION_SINGLE)) {
      return "[" + mtasPositionStart + "]";
    } else if (mtasPositionType.equals(POSITION_RANGE)) {
      return "[" + mtasPositionStart + "-" + mtasPositionEnd + "]";
    } else if (mtasPositionType.equals(POSITION_SET)) {
      return Arrays.toString(mtasPositionList);
    } else {
      return "[unknown]";
    }
  }

}
