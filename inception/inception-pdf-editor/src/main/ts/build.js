/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
const esbuild = require('esbuild')
const fs = require('fs-extra');

let outbase = "../../../target/js/de/tudarmstadt/ukp/inception/pdfeditor/resources/";

fs.emptyDirSync(outbase)
fs.mkdirsSync(`${outbase}/pdfjs/`)

defaults = {
  bundle: true,
  sourcemap: true,
  minify: true,
  target: "es6",
  loader: { ".ts": "ts" },
  logLevel: 'info'
}

esbuild.build(Object.assign({
  entryPoints: ["src/pdfanno/pdfanno.ts"],
  outfile: `${outbase}/pdfanno.page.bundle.js`,
  globalName: "pdfanno.page",
}, defaults))

esbuild.build(Object.assign({
  entryPoints: ["src/pdfjs/viewer.js"],
  outfile: `${outbase}/viewer.bundle.js`,
  globalName: "viewer",
  external: ['images/*']
}, defaults))

esbuild.build(Object.assign({
  entryPoints: ["src/pdfjs/l10n.ts"],
  outfile: `${outbase}/l10n.bundle.js`,
  globalName: "l10n",
}, defaults))

fs.copySync('index.html', `${outbase}/index.html`)
fs.copySync('index-debug.html', `${outbase}/index-debug.html`)
fs.copySync('../pdfjs/images', `${outbase}/images`)
fs.copySync('../pdfjs/locale', `${outbase}/locale`)
fs.copySync('node_modules/pdfjs-dist/cmaps', `${outbase}/cmaps`)
fs.copySync('node_modules/pdfjs-dist/web/compatibility.js', `${outbase}/compatibility.bundle.js`)
fs.copySync('node_modules/pdfjs-dist/web/compatibility.js.map', `${outbase}/compatibility.bundle.js.map`)

fs.copySync('node_modules/pdfjs-dist/build/pdf.js', `${outbase}/pdfjs/pdf.js`)
fs.copySync('node_modules/pdfjs-dist/build/pdf.js.map', `${outbase}/pdfjs/pdf.js.map`)
fs.copySync('node_modules/pdfjs-dist/build/pdf.min.js', `${outbase}/pdfjs/pdf.min.js`)
fs.copySync('node_modules/pdfjs-dist/build/pdf.worker.js', `${outbase}/pdfjs/pdf.worker.js`)
fs.copySync('node_modules/pdfjs-dist/build/pdf.worker.js.map', `${outbase}/pdfjs/pdf.worker.js.map`)
fs.copySync('node_modules/pdfjs-dist/build/pdf.worker.min.js', `${outbase}/pdfjs/pdf.worker.min.js`)
