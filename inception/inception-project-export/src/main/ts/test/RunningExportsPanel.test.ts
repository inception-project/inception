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
import { it, expect } from 'vitest'
import RunningExportsPanel from '../src/RunningExportsPanel.svelte'
import { render } from '@testing-library/svelte'

it('Shows the loading indicator', async () => {
  const { getByText } = render(RunningExportsPanel, {
    props: {
      connected: false,
      wsEndpointUrl: 'ws://localhost:8080',
    }
  })

  const loadIndicator = getByText('Connecting...')

  expect(loadIndicator).to.be.not.null
})

it('Shows the activities', async () => {
  const { queryByText, getByText } = render(RunningExportsPanel, {
    props: {
      connected: true,
      exports: [
        {
          id: '1',
          title: 'test-download',
          progress: 0.5,
          state: 'RUNNING'
        }
      ]
    }
  })

  expect(queryByText('Connecting...')).to.be.null
  expect(getByText('test-download')).to.be.not.null
})
