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

import esbuild from 'esbuild'
import esbuildSvelte from 'esbuild-svelte'
import sveltePreprocess from 'svelte-preprocess'
import yargs from 'yargs/yargs'
import { hideBin } from 'yargs/helpers'
import fs from 'fs-extra'

const argv = yargs(hideBin(process.argv)).argv

const packagePath = 'de/tudarmstadt/ukp/inception/project/export/settings'

let outbase = `../../../target/js/${packagePath}`

const defaults = {
  mainFields: ['svelte', 'browser', 'module', 'main'],
  format: 'esm',
  plugins: [
    esbuildSvelte({
      preprocess: sveltePreprocess()
    })
  ],
  bundle: true,
  sourcemap: false,
  minify: !argv.live,
  target: 'es6',
  loader: { '.ts': 'ts' },
  logLevel: 'info'
}

if (argv.live) {
  defaults.watch = {
    onRebuild (error, result) {
      if (error) console.error('watch build failed:', error)
      else console.log('watch build succeeded:', result)
    }
  }
  outbase = `../../../target/classes/${packagePath}`
} else {
  fs.emptyDirSync(outbase)
}
fs.mkdirsSync(`${outbase}`)

esbuild.build(Object.assign({
  entryPoints: ['src/RunningExportsPanel.svelte'],
  outfile: `${outbase}/RunningExportsPanel.min.js`
}, defaults))
.catch(() => process.exit(1))
