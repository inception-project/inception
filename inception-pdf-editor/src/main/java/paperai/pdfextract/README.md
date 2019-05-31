# pdfextract
PDF extractor using PDFBox.  
The jar file can be found at [releases](https://github.com/paperai/pdfextract/releases).
* [PDFExtract.jl](https://github.com/hshindo/PDFExtract.jl): julia wrapper for pdfextract

## PDFExtractor
Extract texts and draws from PDF.
```
java -classpath pdfextract.jar paperai.pdfextract.PDFExtractor [file or directory]
```

For example,
```
java -classpath pdfextract.jar paperai.pdfextract.PDFExtractor xxx.pdf
```

<p align="center"><img src="https://github.com/paperai/pdfextract/blob/master/PDFExtractor.png" width="1200"></p>

In the figure, blue square indicates font coordinates, and red square indicates glyph coordinates.

### Output Format
Gzip file is generated.  
Each line is either `Text` or `Draw` as follows.

#### Text
1. Page number
1. Character or `NO_UNICODE` (when unicode mapping is unavailable)
1. Font coordinate (x, y, width, height)
1. Glyph coordinate (x, y, width, height)

#### Draw
1. Page number
1. Draw operation, either one of
    * `[STROKE_PATH]`
    * `[FILL_PATH]`
    * `[FILL_STROKE_PATH]`
    * `[CURVE_TO]`
    * `[LINE_TO]`
    * `[MOVE_TO]`
    * `[RECTANGLE]`
1. Coordinate

### Output Example
```
1	P	107.551 793.155 5.478471 10.705882	107.551 795.58496 5.424672 5.8550596
1	r	113.02947 793.155 3.4879298 10.705882	113.43296 797.48584 3.290669 3.9541826
1	o	116.113914 793.155 4.4832 10.705882	116.35601 797.48584 3.9541826 4.052813
1	c	120.597115 793.155 3.981082 10.705882	120.866104 797.48584 3.5417283 4.052813
1	e	124.57819 793.155 3.981082 10.705882	124.856155 797.48584 3.4161987 4.052813
1	e	128.55928 793.155 3.981082 10.705882	128.83723 797.48584 3.4161987 4.052813
1	d	132.54036 793.155 4.4832 10.705882	132.67485 795.316 4.590797 6.240615
1	i	137.02356 793.155 2.4926593 10.705882	137.4629 795.576 1.9277761 5.9626565
1	n	139.51622 793.155 4.4832 10.705882	139.64175 797.48584 4.124544 4.03488
1	g	143.99942 793.155 4.4832 10.705882	144.07115 797.48584 4.16041 5.801261
1	s	148.48262 793.155 3.4879298 10.705882	148.62608 797.47687 3.13824 4.0797124
...
...
4	[MOVE_TO] 323.779 200.93103
4	[LINE_TO] 509.279 200.93103
4	[LINE_TO] 509.279 62.675964
4	[LINE_TO] 323.779 62.675964
4	[RECTANGLE] 323.779 200.93103 509.279 200.93103 509.279 62.676025 323.779 62.676025
4	[FILL_PATH]
```

## ImageExtractor
Extract images from PDF as PNG format.
```
java -classpath pdfextract.jar paperai.pdfextract.ImageExtractor <file or directory> -dpi <dpi> -o <output directory>
```

For example,
```
java -classpath pdfextract.jar ImageExtractor xxx.pdf -dpi 300 -o /work
```
