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

public class TextOperator
{
    String unicode;
    float fx;
    float fy;
    float fw;
    float fh;
    float gx;
    float gy;
    float gw;
    float gh;

    public TextOperator(String unicode, float fx, float fy, float fw, float fh, float gx, float gy,
        float gw, float gh)
    {
        this.unicode = unicode;
        this.fx = fx;
        this.fy = fy;
        this.fw = fw;
        this.fh = fh;
        this.gx = gx;
        this.gy = gy;
        this.gw = gw;
        this.gh = gh;
    }
}
