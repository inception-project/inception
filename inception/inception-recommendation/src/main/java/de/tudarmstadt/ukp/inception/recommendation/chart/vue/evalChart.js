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
class EvalChart {
    /**
    * @param {string} aContainerId    id of the component that holds the chart svg
    * @param {object} aInitData initial data should be of format array of websocket-messages
    * e.g.
     [
         {
             "timestamp": 1631015412000
             "eventMsg": { 'name': "rec1",
                 'accuracy': 0.0,
                 'precision': 0.1,
                 'recall': 0.2,
                 'f1': 0.15}
         },
         {
             "timestamp": 1631015412000,
             "eventMsg": { 'name': "rec2",
                 'accuracy': 0.3,
                 'precision': 0.4,
                 'recall': 0.2,
                 'f1': 0.3}
         }
     ]
    */
    constructor(aContainerId, aInitData) {
        // chart data will be of format
        // chartData = [
        //    //rec 0
        //  [{ 
        //     'acc' : 0.1,
        //     'precision' : 0.3,
        //     ...
        //   },
        //   {  
        //     'acc' : 0.2,
        //     'precision' : 0.4,
        //     ...
        //   },
        //  ],
        //    // rec1  
        //  [{
        //      'acc' : 0.0,
        //      ...
        //      }, 
        //   ...
        //   ]
        // ]
        // recommenders with less evaluations will be padded with 0.0 at the beginning
        this.seriesNameToIndex = new Map();
        this.chartData = [];
        this.insertAll(aInitData);

        // TODO add dropdown to select metric
        this.yAccessor = (d) => d.accuracy;
        this.xAccessor = (_, i) => i + 1;

        // sizes
        const chartContainer = d3.select('#' + aContainerId)
        const margin = { 'right': 5, 'left': 30, 'top': 5, bottom: '20' };
        const width = chartContainer.node().clientWidth;
        const height = chartContainer.node().clientHeight;
        
        this.boundedWidth = width - margin.left - margin.right;
        this.boundedHeight = height - margin.top - margin.bottom;

        // colors
        this.lineColors = d3.scaleOrdinal().domain(this.chartData.keys())
            .range(['#8dd3c7', '#bebada', '#fb8072',
                '#80b1d3', '#fdb462', '#b3de69', '#fccde5', '#d9d9d9', '#bc80bd']);

        // scales 
        this.xScale = null;
        this.yScale = d3.scaleLinear()
            // extent determines min, max of data using accessor function
            // domain is data space
            .domain([0, 1])
            // range is graphic space: min max of graphic, starting at the top left corner 
            // (d3 coordinate system)
            .range([this.boundedHeight, 0]);

        // init chart
        // remove old chart if necessary
        let chartSvg = chartContainer.select('#evalChartSvg');
        if (chartSvg) {
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
      *     'name' : 'rec1
      *     'accuracy': 0.0,
      *     'precision': 0.1,
      *     'recall': 0.2,
      *     'f1': 0.15
      * }
      */
    update(aEvalDatum) {
        let seriesName = aEvalDatum.name;
        let isNewSeries = !this.seriesNameToIndex.has(seriesName);
        this.insertDatum(aEvalDatum);
        if (isNewSeries) {
            this.fillMissingValuesForSeries(this.seriesNameToIndex.get(seriesName),
                d3.max(this.chartData, a => a.length));
        }
        this.updateChart();
    }

    insertDatum(aEvalDatum) {
        if (!aEvalDatum) {
            return;
        }
        let seriesName = aEvalDatum.name;
        if (!this.seriesNameToIndex.has(seriesName)) {
            let seriesIdx = this.chartData.length;
            this.seriesNameToIndex.set(seriesName, seriesIdx);
            this.chartData[seriesIdx] = [];
        }
        this.chartData[this.seriesNameToIndex.get(seriesName)].push({
            'accuracy': aEvalDatum.accuracy,
            'precision': aEvalDatum.precision,
            'recall': aEvalDatum.recall,
            'f1': aEvalDatum.f1
        });
    }

    insertAll(aMsgArr) {
        // sort data by date and insert into data structures
        let sortedMsgs = aMsgArr.sort((a, b) => a.timestamp - b.timestamp);
        sortedMsgs.forEach(msg => this.insertDatum(JSON.parse(msg.eventMsg)));
        // prepend with zero for missing evaluations
        this.fillMissingValues();
    }

    fillMissingValues() {
        if (!this.chartData || this.chartData.length < 2) {
            return;
        }
        let numEvals = d3.max(this.chartData, a => a.length);
        for (let i = 0; i < this.chartData.length; i++) {
            this.fillMissingValuesForSeries(i, numEvals);
        }
    }

    fillMissingValuesForSeries(i, numEvals) {
        let currentMetrics = this.chartData[i];
        let numDataPts = currentMetrics.length;
        if (numEvals > numDataPts) {
            let prependArr = new Array(numEvals - numDataPts).fill({
                'accuracy': 0.0,
                'precision': 0.0,
                'recall': 0.0,
                'f1': 0.0
            });
            this.chartData[i] = prependArr.concat(currentMetrics);
        }
    }
    
	/**
     * return recommender names and their associated colors
     */
    getRecommenders(){
        let recs = [];
        for (const name of this.seriesNameToIndex.keys()){
            recs.push({
                'name' : name,
                'color' : this.lineColors(this.seriesNameToIndex.get(name)) 
            });
        }
        return recs;
    }

    updateChart() {
        // update x-scale (min, max may have changed)
        // define scaling from data space to space inside graphic
        const maxVals = d3.max(this.chartData, a => a.length);
        this.xScale = d3.scaleLinear()
            .domain([0, maxVals])
            .range([0, this.boundedWidth]);

        // update x-axis
        const xAxisGenerator = d3.axisBottom()
            .scale(this.xScale)
            // axis should have amount of x-values ticks with interval size 1
            .ticks(maxVals, 'f')
            .tickFormat("");
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
            .data(this.chartData);

        // handle incoming and changing data
        lineSelection.join(
            // add new lines with class eval-line (to select)
            enter =>
                enter
                    .append('path')
                    .attr('class', 'eval-line')
                    // select color depending on index in data 
                    // (refers to the associated recommender)
                    .attr("stroke", (_,i) => this.lineColors(i))
                    .attr('d', evalLine),
            update =>
                update
                    .transition()
                    .duration(500)
                    .attr('d', evalLine),
            exit => exit.remove()
        );
    }
}
