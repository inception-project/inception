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
function updateLearningCurveDiagram(arrayOfLearningCurves, errorMessage) {
    var xAxixType = 'indexed';
    var plotType = 'step';
    var xTick = {
    	format : function(a) {
    		return Math.round(1e2 * a) / 1e2;
    	}
    };
    
	// make the type of x-axis "category" (shows x in category intervals of size 1). 
    // It is for better visualization when the x-axis represents test data size (annotationpage)
    if ((arrayOfLearningCurves[0][2] - arrayOfLearningCurves[0][1]) > 1) 
    	xAxixType = "category";
    

    // if we just have one value per data-row, we cannot visualize a step
    var plotTypes = {};
	for (i = 0; i < arrayOfLearningCurves.length; i++) {
		if (arrayOfLearningCurves[i].length < 3) {
			plotTypes[arrayOfLearningCurves[i][0]] = 'scatter';
		} else {
			plotTypes[arrayOfLearningCurves[i][0]] = 'step';
		}
	}
    
	if (arrayOfLearningCurves[0].length < 3) {
		xTick["values"] = [ 0.0, 1.0 ];
	}
    
    // draw the chart with the help of the arrayOfLearningCurves
    var e = c3.generate({
        bindto: "#canvas",
        data: {
        	empty:{label:{text:errorMessage}},
            x: "x",
            columns: arrayOfLearningCurves,
            types: plotTypes
        },
        axis: {
            x: {
                type: xAxixType,
                tick : xTick
            },
            y: {
            	min: 0,
            	//to round off the decimal points of the y-axis values to 4 if it is a decimal number.
                tick: {
                    format: function(a) {
                        return Math.round(1e4 * a) / 1e4;
                    }
                }
            }
        }
    });
}
