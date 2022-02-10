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
//const { sassPlugin } = require('esbuild-sass-plugin');

fs.emptyDirSync('dist')

defaults = {
  bundle: true,
  sourcemap: true,
  minify: true,
  target: "es6",
  loader: { ".ts": "ts" },
  logLevel: 'info'
//  plugins: [sassPlugin()]
}

esbuild.build(Object.assign({
  entryPoints: ["src/core/index.js"],
  outfile: "dist/pdfanno/pdfanno.core.bundle.js",
  globalName: "pdfanno.core"
}, defaults))

esbuild.build(Object.assign({
  entryPoints: ["src/pdfanno.js"],
  outfile: "dist/pdfanno/pdfanno.page.bundle.js",
  globalName: "pdfanno.page",
}, defaults))

esbuild.build(Object.assign({
  entryPoints: ["src/viewer.js"],
  outfile: "dist/pdfanno/viewer.bundle.js",
  globalName: "viewer",
  external: ['images/*']
}, defaults))

esbuild.build(Object.assign({
  entryPoints: ["src/compatibility.js"],
  outfile: "dist/pdfanno/compatibility.bundle.js",
  globalName: "compatibility",
}, defaults))

esbuild.build(Object.assign({
  entryPoints: ["src/l10n.js"],
  outfile: "dist/pdfanno/l10n.bundle.js",
  globalName: "l10n",
}, defaults))

fs.mkdirsSync('dist/pdfanno')
fs.copySync('index.html', 'dist/pdfanno/index.html')
fs.copySync('index-debug.html', 'dist/pdfanno/index-debug.html')
fs.copySync('node_modules/pdfjs-dist/cmaps', 'dist/pdfanno/cmaps')
fs.copySync('../pdfjs/images', 'dist/pdfanno/images')
fs.copySync('../pdfjs/locale', 'dist/pdfanno/locale')

fs.mkdirsSync('dist/pdfanno/pdfjs/')
fs.copySync('node_modules/pdfjs-dist/build/pdf.js', 'dist/pdfanno/pdfjs/pdf.js')
fs.copySync('node_modules/pdfjs-dist/build/pdf.js.map', 'dist/pdfanno/pdfjs/pdf.js.map')
fs.copySync('node_modules/pdfjs-dist/build/pdf.min.js', 'dist/pdfanno/pdfjs/pdf.min.js')
fs.copySync('node_modules/pdfjs-dist/build/pdf.worker.js', 'dist/pdfanno/pdfjs/pdf.worker.js')
fs.copySync('node_modules/pdfjs-dist/build/pdf.worker.js.map', 'dist/pdfanno/pdfjs/pdf.worker.js.map')
fs.copySync('node_modules/pdfjs-dist/build/pdf.worker.min.js', 'dist/pdfanno/pdfjs/pdf.worker.min.js')
