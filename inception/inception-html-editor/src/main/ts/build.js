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
const pjson = require('./package.json')

const esbuild = require('esbuild')

const now = new Date()

const banner = `/* 
 * INCEpTION ${pjson.version} (${now.getFullYear()}-${('0' + (now.getMonth() + 1)).slice(-2)}-${('0' + (now.getDate() + 1)).slice(-2)})
 *
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
 *
 * Derived under the conditions of the MIT license from below:
 *
 * Based on Annotator v.1.2.10 (built at: 26 Feb 2015)
 * http://annotatorjs.org
 *
 * Copyright 2015, the Annotator project contributors.
 * Dual licensed under the MIT and GPLv3 licenses.
 * https://github.com/openannotation/annotator/blob/master/LICENSE
 */
`

const defaults = {
  bundle: true,
  banner: { js: banner, css: banner },
  sourcemap: true,
  minify: false,
  target: 'es6',
  loader: {
    '.ts': 'ts',
    '.png': 'dataurl'
  },
  logLevel: 'info'
}

let outbase = '../../../target/js/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/'

const argv = yargs(hideBin(process.argv)).argv

if (argv.live) {
  defaults.watch = {
    onRebuild (error, result) {
      if (error) console.error('watch build failed:', error)
      else console.log('watch build succeeded:', result)
    }
  }
  outbase = '../../../target/classes/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/'
}

esbuild.build(Object.assign({
  entryPoints: ['main.ts'],
  outfile: `${outbase}/AnnotatorJsEditor.min.js`,
  globalName: 'AnnotatorJsEditor'
}, defaults))
