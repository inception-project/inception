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
class EvalChart{
   /**
   * @param {string} aContainerId    id of the component that holds the chart svg
   * @param {object} aInitData initial data should be of format array of websocket-messages
   * e.g.
    [
        {
            "actorName": "admin",
            "projectName": "Game of Thrones",
            "documentName": null,
            "timestamp": 1631015412000,
            "eventType": "RecommenderTaskUpdateEvent",
            "eventMsg": { 'recommenderName': "rec1",
                'accuracy': 0.0,
                'precision': 0.1,
                'recall': 0.2,
                'f1': 0.15},
            "errorMsg": null
        },
        {
            "actorName": "admin",
            "projectName": "Game of Thrones",
            "documentName": null,
            "timestamp": 1631015412000,
            "eventType": "RecommenderTaskUpdateEvent",
            "eventMsg": { 'recommenderName': "rec2",
                'accuracy': 0.3,
                'precision': 0.4,
                'recall': 0.2,
                'f1': 0.3},
            "errorMsg": null
        }
    ]
   */
    constructor(aContainerId, aInitData){
        // chart data will be of format
        // chartData = {
        //      'rec1' : [{ 'date' : 1631015412000,
        //          'acc' : 0.1,
        //          'precision' : 0.3,
        //          ...
        //          },
        //          'acc' : 0.2,
        //          'precision' : 0.4,
        //          ...
        //          },
        //          ],
        //      'rec2' : [{
        //          'acc' : 0.0,
        //          ...
        //          }, ...]
        //      }
        // recommenders with less evaluations will be padded with 0.0 at the beginning
        this.chartData = this.msgToChartData(aInitData);
        // TODO: change, let's select one of the recommenders for a first test
        this.yAccessor = (d) => d.acc;
        this.xAccessor = (_, i) => i + 1;

        // sizes
        const chartContainer = d3.select('#' + aContainerId)
        const margin = { 'right': 5, 'left': 30, 'top': 5, bottom: '20' };
        const width = chartContainer.node().clientWidth;
        const height = chartContainer.node().clientHeight;

        this.boundedWidth = width - margin.left - margin.right;
        this.boundedHeight = height - margin.top - margin.bottom;

        // scales 
        this.xScale = null;
        this.yScale = d3.scaleLinear()
            // extent determines min, max of data using accessor function
            // domain is data space
            .domain([0, 1])
            // range is graphic space: min max of graphic, starting at the top left corner (d3 coordinate system)
            .range([this.boundedHeight, 0]);

        // init chart
        // remove old chart if necessary
        let chartSvg = chartContainer.select('#evalChartSvg');
        if (chartSvg){
            chartSvg.remove();
        }
        this.chartGroup = chartContainer
            // add chart
            .append('svg')
            .attr('id', 'evalChartSvg')
            // set size
            .attr('width', width)
            .attr('height', height)
            // add grouping of all elements inside the svg (can transform them together)
            .append('g')
            // move bounded elements inside margins
            .attr('transform', "translate(" + margin.left + "," + margin.top + ")");
        // axes
        this.xAxis = this.chartGroup
            .append("g")
            .attr('id', 'x-axis')
            // move x-axis to the bottom of the chart
            .attr("transform", "translate(0," + this.boundedHeight + ")");
        const yAxisGenerator = d3.axisLeft().scale(this.yScale);
        this.yAxis = this.chartGroup
            .append("g")
            .call(yAxisGenerator);
        this.updateChart();
    }

    /**
      * @param {object} aEvalDatum    evaluation data item that should be added to the chart
      * should be of format
      * {
      *     'recommenderName' : 'rec1
      *     'accuracy': 0.0,
      *     'precision': 0.1,
      *     'recall': 0.2,
      *     'f1': 0.15
      * }
      */
    update(aEvalDatum) {
        this.chartData[aEvalDatum.recommenderName].push({
            'accuracy': aEvalDatum.accuracy,
            'precision': aEvalDatum.precision,
            'recall': aEvalDatum.recall,
            'f1': aEvalDatum.f1
        });

        this.updateChart();
    }

    msgToChartData(aMsgArr){
        let metricsByRec = {};
        let metricsNum = {};
        // sort data by recommender name
        aMsgArr.forEach(msg => {
            let recName = msg.eventMsg.recommenderName;
            if (metricsByRec[recName]){
                metricsByRec[recname].push({ 
                    'date' : msg.timestamp,
                    'metrics' : {
                        'accuracy' : msg.eventMsg.accuracy, 
                        'precision' : msg.eventMsg.precision,
                        'recall' : msg.eventMsg.recall,
                        'f1' : msg.eventMsg.f1
                    }
                });
                metricsNum[recName] += 1;
            } else
            {
                chartDatum[recName] = [];
                metricsNum[recName] = 0;
            }
        })
        let numDataPts = Math.max(...metricsNum.values());
        // sort all data points for each recommender by date
        // and prepend with 0 values if needed
        metricsByRec.forEach( (value) => {
            value.sort((a, b) => a.date - b.date);
            value.map(m => m.metrics);
            let prependArr = new Array(numDataPts - value.length).fill({
                'accuracy' : 0.0, 
                'precision' : 0.0,
                'recall' : 0.0,
                'f1' : 0.0
            })
            value = prependArr.concat(value);
        })
        return metricsByRec;
    };

    updateChart() {
        // update x-scale (min, max may have changed)
        //define scaling from data space to space inside graphic
        const xMinMax = d3.extent(this.chartData, this.xAccessor);
        this.xScale = d3.scaleLinear()
            .domain(xMinMax)
            .range([0, this.boundedWidth]);

        // update x-axis
        const xAxisGenerator = d3.axisBottom()
            .scale(this.xScale)
            // axis should have amount of x-values ticks with interval size 1
            .ticks(xMinMax[1], 'f');
        this.xAxis = this.chartGroup
            .select('#x-axis')
            .call(xAxisGenerator);

        // render chart lines
        const evalLine = d3
            .line()
            .x((d, i) => this.xScale(this.xAccessor(d, i)))
            .y((d) => this.yScale(this.yAccessor(d)))
            .curve(d3.curveStepAfter);

        const lineSelection = this.chartGroup
            .selectAll('.eval-line')
            .data([this.chartData]);

        // handle incoming and changing data
        lineSelection.join(
            // add new lines with class eval-line (to select)
            enter =>
                enter
                    .append('path')
                    .attr('class', 'eval-line')
                    .attr("fill", "none")
                    .attr("stroke-width", 2)
                    .attr("stroke", "black")
                    .attr('d', evalLine),
            update =>
                update
                    .transition()
                    .duration(750)
                    .attr('d', evalLine),
            exit => exit.remove()
        );
    }
}