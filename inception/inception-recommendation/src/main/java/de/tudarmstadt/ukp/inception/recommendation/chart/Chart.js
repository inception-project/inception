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
function updateLearningCurveDiagram(selector, data) {
  var e = c3.generate({
    bindto: selector,
    size : { height: 200 },
    legend: { show: false },
    data: {
      empty: { label: { text: "No Data Available" } },
      json: data,
      x : 'run',
      axes: {
        score: 'y',
        trainSize: 'y2'
      },
      keys: {
        x: 'run',
        value: ['score', 'trainSize'] 
      },
      names: {
        score: 'Score',
        trainSize: 'Training instances'
      }
    },
    axis: {
      x: {
        type: "category",
        tick: { format: function(a) { return "#" + (a+1); } },
        label: 'Evaluation run',
      },
      y: {
        min: 0,
        //to round off the decimal points of the y-axis values to 4 if it is a decimal number.
        tick: { format: function(a) { return Math.round(1e4 * a) / 1e4; } },
        label: 'Score',
      },
      y2: {
        show: true,
        label: 'Training instances',
        min: 0
      }
    }
  });
}
