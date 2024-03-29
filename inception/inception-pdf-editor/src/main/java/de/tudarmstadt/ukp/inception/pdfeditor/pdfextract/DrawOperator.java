/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2017 Hiroyuki Shindo
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.tudarmstadt.ukp.inception.pdfeditor.pdfextract;

/**
 * @deprecated Superseded by the new PDF editor
 */
@Deprecated
public class DrawOperator
    implements Operator
{
    public static final String OP_FILL_STROKE_PATH = "FILL_STROKE_PATH";
    public static final String OP_FILL_PATH = "FILL_PATH";
    public static final String OP_STROKE_PATH = "STROKE_PATH";
    public static final String OP_CURVE_TO = "CURVE_TO";
    public static final String OP_LINE_TO = "LINE_TO";
    public static final String OP_RECTANGLE = "RECTANGLE";
    public static final String OP_MOVE_TO = "MOVE_TO";

    final String type;
    final float[] values;

    public DrawOperator(String type, float... values)
    {
        this.type = type;
        this.values = values;
    }
}
