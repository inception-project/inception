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

// =======================================================================
// File lives in 
// /inception/inception-build/src/main/resources/inception/vite.config.js
// =======================================================================

import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import { svelteTesting } from '@testing-library/svelte/vite';

export default defineConfig(({mode}) => ({
  plugins: [
    svelte(),
    svelteTesting()
  ],
  test: {
    globals: true,
    environment: 'jsdom'
  },
  resolve: {
    conditions: mode === 'test' ? ['browser'] : [],
  }
}))
