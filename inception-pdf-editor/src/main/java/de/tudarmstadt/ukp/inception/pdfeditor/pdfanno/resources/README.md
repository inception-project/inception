<p align="center"><img src="https://github.com/paperai/pdfanno/blob/master/pdfanno.gif" width="850"></p>

# PDFAnno
PDFAnno is a browser-based linguistic annotation tool for PDF documents.  
It offers functions for annotating PDF with labels and relations.  
For natural language processing and machine learning, it is suitable for development of gold-standard data with named entity spans, dependency relations, and coreference chains.

If you use PDFAnno, please cite the following paper:
```
Hiroyuki Shindo, Yohei Munesada and Yuji Matsumoto,
"PDFAnno: a Web-based Linguistic Annotation Tool for PDF Documents",
In Proceedings of LREC, 2018.
```

* [Online Demo (v0.4.1)](https://paperai.github.io/pdfanno/0.4.1/)

**It is highly recommended to use the latest version of Chrome.** (Firefox will also be supported in future.)

## Installation
If you install PDFAnno locally,
```
git clone https://github.com/paperai/pdfanno.git
cd pdfanno
npm install
cp .env.example .env
```

Then, edit `.env` as you like.  
The default values are:
```
SERVER_PORT=1000
```

### Run Server
```
npm run server
```

## Usage
1. Visit the [online demo](https://paperai.github.io/pdfanno/latest/) with the latest version of Chrome.
1. Load your PDF and annotation file (if any). Sample PDFs and annotations are downloadable from [here](https://cl.naist.jp/%7Eshindo/pdfanno_material.zip).
    * For PDFs located on your computer:  
    Put the PDFs and annotation files (if any) in the same directory, then specify the directory via `Browse` button.
    * For PDF available on the Web:  
    Access 'https://paperai.github.io/pdfanno/latest/?pdf=' + `<URL of the PDF>`  
    For example, https://paperai.github.io/pdfanno/latest/?pdf=http://www.aclweb.org/anthology/P12-1046.pdf.  
1. Annotate the PDF as you like.
1. Save your annotations via <img src="https://github.com/paperai/pdfanno/blob/master/icons/fa-download.png" width="2%"> button.  
If you continue the annotation, respecify your directory via `Browse` button to reload the PDF and anno file.

For security reasons, PDFAnno does NOT automatically save your annotations.  
Don't forget to download your current annotations!  

## Annotation Tools
| Icon | Description |
|:---:|:---:|
| <img src="https://github.com/paperai/pdfanno/blob/master/icons/fa-pencil.png" width="7%"> | Span highlighting. It is disallowed to cross page boundaries. |
| <img src="https://github.com/paperai/pdfanno/blob/master/icons/fa-long-arrow-right.png" width="7%"> | One-way relation. This is used for annotating dependency relation between spans. |
| <img src="https://github.com/paperai/pdfanno/blob/master/icons/fa-square-o.png" width="7%"> | Rectangle. It is disallowed to cross page boundaries. |

## Annotation File (.anno)
In PDFAnno, an annotation file (.anno) follows [TOML](https://github.com/toml-lang/toml) format.  
Here is an example of anno file:
```
pdfanno = "0.4.1"
pdfextract = "0.2.4"

[[spans]]
id = "1"
page = 1
label = "label1"
text = "AgBi 0.05 Sb 0.95 Te 2"
textrange = [1422,1438]

[[spans]]
id = "2"
page = 1
label = "label1"
text = "0.48 Wm [NO_UNICODE] 1 K [NO_UNICODE] 1 )"
textrange = [1386,1397]

[[relations]]
head = "1"
tail = "2"
label = "relation1"
```
where `textrange` corresponds to the start and end token id of `pdftxt`.  
`pdftxt` is a text file extracted from the original pdf file.  
You can download `pdftxt` via `pdf.txt` button at the top right of the screen.

## Reference Anno File
To support multi-user annotation, PDFAnno allows to load `reference anno file`.  
For example, if you create `a.anno` and an another annotator creates `b.anno` for the same PDF, load `a.anno` as usual, and load `b.anno` as a reference file. Then PDFAnno renders `a.anno` and `b.anno` with different colors each other. Rendering more than one reference file is also supported.   
This is useful to check inter-annotator agreement and resolving annotation conflicts.  
Note that the reference files are rendered as read-only.

## Contact
Please contact [hshindo](https://github.com/hshindo) or feel free to create an issue.

## LICENSE
MIT
