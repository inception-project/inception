/*
#Copyright 2019
#Ubiquitous Knowledge Processing (UKP) Lab
#Technische Universität Darmstadt
#
#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at
# 
# http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.
*/

/*
 * The method renders a c3 chart with the help of the data it receives from the server side.
 * 
 * the arrayOfLeaningCurves also includes x-axis.
 */
function updateLearningCurveDiagram(arrayOfLearningCurves) {
	
    var xAxixType = 'indexed';
    var plotType = 'step';
    var xTick = {
    		rotate : 0,
    		multiline : true,
    		format : function(a) {
    			return Math.round(1e2 * a) / 1e2;
    		}
    };
    
	//make the type of x-axis "category" when we have more than one learning curves. i.e when the request is from annotation recommender side bar. It is for better visualization when the x-axis represents test data size
    if ((arrayOfLearningCurves[0][2] - arrayOfLearningCurves[0][1]) > 1) 
    	xAxixType = "category";
    

    // if we just have one value, we cannot visualize a step
	if (arrayOfLearningCurves[0] && arrayOfLearningCurves[0].length < 3) {
		plotType = 'scatter';
		xTick["values"] = [ 0.0, 1.0 ];
	}
    
    // draw the chart with the help of the arrayOfLearningCurves. The type of
	// the graph is "step".
    var e = c3.generate({
        bindto: "#canvas",
        data: {
            x: "x",
            columns: arrayOfLearningCurves,
            type: plotType,
            empty:{label:{text:"No Data Available"}}
        },
        axis: {
            x: {
                type: xAxixType,
                tick : xTick
            },
            //to round off the decimal points of the y-axis values to 4 if it is a decimal number.
            y: {
                tick: {
                    format: function(a) {
                        return Math.round(1e4 * a) / 1e4;
                    }
                }
            }
        }
    });
}