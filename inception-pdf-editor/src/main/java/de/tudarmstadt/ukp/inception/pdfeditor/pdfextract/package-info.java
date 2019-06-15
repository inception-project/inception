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
/**
 * <h1>pdfextract</h1>
 * 
 * Copyright (c) 2017 Hiroyuki Shindo
 * 
 * Original source: https://github.com/yuichiro-s/pdfextract
 * 
 * PDF extractor using PDFBox.  
 *
 * <h2>PDFExtractor</h2>
 * Extract texts and draws from PDF.
 * 
 * <pre>{@code
 * java -classpath pdfextract.jar paperai.pdfextract.PDFExtractor [file or directory]
 * }</pre>
 * 
 * For example,
 * <pre>{@code
 * java -classpath pdfextract.jar paperai.pdfextract.PDFExtractor xxx.pdf
 * }</pre>
 * 
 * <h3>Output Format</h3>
 * Gzip file is generated.  
 * Each line is either <b>Text</b> or <b>Draw</b> as follows.
 * 
 * <h4>Text</h4>
 * 
 * <ol>
 * <li>Page number</li>
 * <li>Character or <code>NO_UNICODE</code> (when unicode mapping is unavailable)</li>
 * <li>Font coordinate (x, y, width, height)</li>
 * <li>Glyph coordinate (x, y, width, height)</li>
 * </ol>
 * 
 * <h4>Draw</h4>
 * <ol>
 * <li>Page number</li>
 * <li>Draw operation, either one of
 *   <ul>
 *     <li><code>[STROKE_PATH]</code></li>
 *     <li><code>[FILL_PATH]</code></li>
 *     <li><code>[FILL_STROKE_PATH]</code></li>
 *     <li><code>[CURVE_TO]</code></li>
 *     <li><code>[LINE_TO]</code></li>
 *     <li><code>[MOVE_TO]</code></li>
 *     <li><code>[RECTANGLE]</code></li>
 *   </ul>
 * </li>
 * <li>Coordinate</li>
 * </ol>
 * 
 * <h3>Output Example</h3>
 * <pre>{@code
 * 1   P   107.551 793.155 5.478471 10.705882  107.551 795.58496 5.424672 5.8550596
 * 1   r   113.02947 793.155 3.4879298 10.705882   113.43296 797.48584 3.290669 3.9541826
 * 1   o   116.113914 793.155 4.4832 10.705882 116.35601 797.48584 3.9541826 4.052813
 * 1   c   120.597115 793.155 3.981082 10.705882   120.866104 797.48584 3.5417283 4.052813
 * 1   e   124.57819 793.155 3.981082 10.705882    124.856155 797.48584 3.4161987 4.052813
 * 1   e   128.55928 793.155 3.981082 10.705882    128.83723 797.48584 3.4161987 4.052813
 * 1   d   132.54036 793.155 4.4832 10.705882  132.67485 795.316 4.590797 6.240615
 * 1   i   137.02356 793.155 2.4926593 10.705882   137.4629 795.576 1.9277761 5.9626565
 * 1   n   139.51622 793.155 4.4832 10.705882  139.64175 797.48584 4.124544 4.03488
 * 1   g   143.99942 793.155 4.4832 10.705882  144.07115 797.48584 4.16041 5.801261
 * 1   s   148.48262 793.155 3.4879298 10.705882   148.62608 797.47687 3.13824 4.0797124
 * ...
 * ...
 * 4   [MOVE_TO] 323.779 200.93103
 * 4   [LINE_TO] 509.279 200.93103
 * 4   [LINE_TO] 509.279 62.675964
 * 4   [LINE_TO] 323.779 62.675964
 * 4   [RECTANGLE] 323.779 200.93103 509.279 200.93103 509.279 62.676025 323.779 62.676025
 * 4   [FILL_PATH]
 * }</pre>
 * 
 * <h2>ImageExtractor</h2>
 * Extract images from PDF as PNG format.
 * <pre>{@code
 * java -classpath pdfextract.jar paperai.pdfextract.ImageExtractor <file or directory> 
 *   -dpi <dpi> -o <output directory>
 * }</pre>
 * 
 * For example,
 * <pre>{@code
 * java -classpath pdfextract.jar ImageExtractor xxx.pdf -dpi 300 -o /work
 * }</pre>
 */
package de.tudarmstadt.ukp.inception.pdfeditor.pdfextract;
