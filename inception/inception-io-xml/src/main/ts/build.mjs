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
import { readdirSync, writeFileSync, existsSync, mkdirSync } from 'fs'
import { join, extname, resolve, parse } from 'path'
import { compile } from 'sass';

const target = '../../../target/js/de/tudarmstadt/ukp/inception/io/xml/css/'

if (!existsSync(target)){
  mkdirSync(target, { recursive: true })
}

const compileSass = (fullPath) => {
  const baseName = parse(fullPath).name
  const compressedResult = compile(fullPath, {sourceMap: true, style: "compressed"})
  writeFileSync(`${target}/${baseName}.min.css`, compressedResult.css)
  writeFileSync(`${target}/${baseName}.min.map`, JSON.stringify(compressedResult.sourceMap))
  
  const result = compile(fullPath)
  writeFileSync(`${target}/${baseName}.css`, result.css)
}

const findScssFiles = (dir) => {
    readdirSync(dir).forEach(file => {
        const fullPath = join(dir, file)

        if (extname(fullPath) === '.scss') {
            compileSass(fullPath)
        }
    })
}

// Start the process
const srcDir = resolve('src')
findScssFiles(srcDir)
