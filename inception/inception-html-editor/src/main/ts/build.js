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
const yargs = require('yargs/yargs')
const { hideBin } = require('yargs/helpers')

const esbuild = require('esbuild');
const fs = require('fs-extra');

let defaults = {
  bundle: true,
  sourcemap: true,
  minify: false,
  target: "es6",
  loader: { 
    ".ts": "ts",
    ".png": "dataurl"
  },
  logLevel: 'info',
//  external: [ '*.png' ],
}

let outbase = "../../../target/js/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/";

const argv = yargs(hideBin(process.argv)).argv

if (argv.live) {
  defaults['watch'] = {
    onRebuild(error, result) {
      if (error) console.error('watch build failed:', error)
      else console.log('watch build succeeded:', result)
    },
  };
  outbase = "../../../target/classes/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/";
}

esbuild.build(Object.assign({
  entryPoints: [ "main.ts" ],
  outfile: `${outbase}/AnnotatorJsEditor.min.js`,
  globalName: "AnnotatorJsEditor"
}, defaults))
