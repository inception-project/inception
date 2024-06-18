package mtas.codec.util.distance;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.util.BytesRef;

import mtas.analysis.token.MtasToken;

/**
 * The Class Distance.
 */
public abstract class Distance {

  /** The prefix. */
  protected final String prefix;
  
  /** The base. */
  protected final String base;
  
  /** The minimum. */
  public final Double minimum;
  
  /** The maximum. */
  public final Double maximum;
  
  /** The parameters. */
  protected final Map<String, String> parameters;
  
  /** The prefix offset. */
  protected final int prefixOffset;

  /** The Constant DOUBLE_TOLERANCE. */
  private static final double DOUBLE_TOLERANCE = 5E-16;

  /** The Constant NAME. */
  public static final String NAME = "distance";

  /**
   * Instantiates a new distance.
   *
   * @param prefix the prefix
   * @param base the base
   * @param minimum the minimum
   * @param maximum the maximum
   * @param parameters the parameters
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Distance(String prefix, String base, Double minimum, Double maximum, Map<String, String> parameters)
      throws IOException {
    this.prefix = prefix;
    this.base = base;
    this.minimum = minimum == null ? null : minimum - DOUBLE_TOLERANCE;
    this.maximum = maximum == null ? null : maximum + DOUBLE_TOLERANCE;
    this.parameters = parameters;
    prefixOffset = prefix.length() + MtasToken.DELIMITER.length();
  }

  /**
   * Compute.
   *
   * @param term the term
   * @return the double
   */
  public abstract double compute(BytesRef term);

  /**
   * Compute.
   *
   * @param key the key
   * @return the double
   */
  public abstract double compute(String key);

  /**
   * Validate.
   *
   * @param term the term
   * @return true, if successful
   */
  public boolean validate(BytesRef term) {
    return validateMaximum(term) && validateMinimum(term);
  }

  /**
   * Validate maximum.
   *
   * @param term the term
   * @return true, if successful
   */
  public abstract boolean validateMaximum(BytesRef term);

  /**
   * Validate minimum.
   *
   * @param term the term
   * @return true, if successful
   */
  public abstract boolean validateMinimum(BytesRef term);

}
