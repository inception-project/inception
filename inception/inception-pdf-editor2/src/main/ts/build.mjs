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
import { sassPlugin } from 'esbuild-sass-plugin'
import fs from 'fs-extra'

const argv = yargs(hideBin(process.argv)).argv

const packagePath = 'de/tudarmstadt/ukp/inception/pdfeditor2/resources/'

let outbase = `../../../target/js/${packagePath}`
if (argv.live) {
  outbase = `../../../target/classes/${packagePath}`
}

const defaults = {
  entryPoints: ['src/main.ts'],
  globalName: 'PdfAnnotationEditor',
  outfile: `${outbase}/PdfAnnotationEditor.min.js`,
  bundle: true,
  sourcemap: true,
  minify: !argv.live,
  target: 'es2018',
  loader: { '.ts': 'ts' },
  logLevel: 'info',
  plugins: [
    sassPlugin(),
    esbuildSvelte({
      compilerOptions: { dev: argv.live },
      preprocess: sveltePreprocess(),
      filterWarnings: (warning) => {
        // Ignore warnings about unused CSS selectors in Svelte components which appear as we import
        // Bootstrap CSS files. We do not use all selectors in the files and thus the warnings are
        // expected.
        if (warning.code === 'css-unused-selector') {
          return false
        }
      }
    })
  ]
}

fs.mkdirsSync(`${outbase}`)
fs.emptyDirSync(outbase)
fs.copySync('pdfjs-web', `${outbase}`)
fs.copySync('node_modules/pdfjs-dist/build', `${outbase}`)
fs.copySync('node_modules/pdfjs-dist/cmaps', `${outbase}/cmaps`)
fs.copySync('node_modules/pdfjs-dist/standard_fonts', `${outbase}/standard_fonts`)

if (argv.live) {
  const context = await esbuild.context(defaults)
  await context.watch()
} else {
  esbuild.build(defaults)
}
