# Developer's Guide
PDFAnno is built upon [pdf.js](https://github.com/mozilla/pdf.js) for PDF viewer.
We implement custom layers for rendering annotations on pdf.js.

### Install and Build
First, install [Node.js](https://nodejs.org/) and npm. The version of Node.js must be 6+.  
Then, run the following commands:
```
npm install
npm run front:publish:latest
```
where the output is on `docs/latest`, and you can access PDFAnno via `docs/latest/index.html`.  

For developing,
```
npm run server:dev
npm run front:dev
```
This command starts Webpack Dev Server and you can access  [http://localhost:8080/dist/index.html](http://localhost:8080/dist/index.html) in your browser.
