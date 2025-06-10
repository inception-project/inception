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
import { it, expect, afterEach, vi, beforeEach } from 'vitest'
import ActivitiesDashlet from '../src/ActivitiesDashlet.svelte'
import { render } from '@testing-library/svelte'

const mockActivities = [{
  id: 1,
  projectId: 2,
  documentId: 3,
  documentName: 'document.txt',
  user: 'username',
  annotator: 'annotator-username',
  timestamp: 1600945790000,
  link: '/inception/p/2/annotate/3',
  type: 'Annotation'
}]

beforeEach(() => {
  // Mock global fetch before each test
  global.fetch = vi.fn(() =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve(mockActivities)
    })
  )
})

afterEach(() => {
  vi.restoreAllMocks()
})

it('Shows the loading indicator', async () => {
  const { getByText } = render(ActivitiesDashlet, {
    props: {
      dataUrl: '',
      activities: []
    }
  })

  const loadIndicator = getByText('Loading...')

  expect(loadIndicator).to.be.not.null
})

it('Shows the activities', async () => {
  const { queryByText, getByText } = render(ActivitiesDashlet, {
    props: {
      dataUrl: '',
      loading: false,
      activities: [{
        id: 1,
        projectId: 2,
        documentId: 3,
        documentName: 'document.txt',
        user: 'username',
        annotator: 'annotator-username',
        timestamp: 1600945790000,
        link: '/inception/p/2/annotate/3',
        type: 'Annotation'
      }]
    }
  })

  expect(queryByText('Loading...')).to.be.null
  expect(getByText('document.txt')).to.have.property('href').to.match(/.*\/inception\/p\/2\/annotate\/3$/)
})
