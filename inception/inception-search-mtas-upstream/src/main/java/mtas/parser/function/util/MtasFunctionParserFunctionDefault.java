package mtas.parser.function.util;

import java.io.IOException;

import mtas.codec.util.CodecUtil;

/**
 * The Class MtasFunctionParserFunctionDefault.
 */
public class MtasFunctionParserFunctionDefault
    extends MtasFunctionParserFunction {

  /**
   * Instantiates a new mtas function parser function default.
   *
   * @param numberOfArguments the number of arguments
   */
  public MtasFunctionParserFunctionDefault(int numberOfArguments) {
    this.dataType = CodecUtil.DATA_TYPE_LONG;
    this.needPositions = false;
    this.sumRule = true;
    this.degree = numberOfArguments > 0 ? 1 : 0;
    for (int i = 0; i < numberOfArguments; i++) {
      this.needArgument.add(i);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.parser.function.util.MtasFunctionParserFunction#getValueDouble(long[],
   * long)
   */
  @Override
  public double getValueDouble(long[] argsQ, long[] argsD, long n, long d) throws IOException {
    double value = 0;
    if (argsQ != null) {
      for (long a : argsQ) {
        value += a;
      }
    }
    return value;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * mtas.parser.function.util.MtasFunctionParserFunction#getValueLong(long[],
   * long)
   */
  @Override
  public long getValueLong(long[] argsQ, long[] argsD, long n, long d) throws IOException {
    long value = 0;
    if (argsQ != null) {
      for (long a : argsQ) {
        value += a;
      }
    }
    return value;
  }

}
