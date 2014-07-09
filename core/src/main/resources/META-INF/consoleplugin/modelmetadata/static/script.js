/**
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
var allModelData = [{
    key: "Instantiated resource models with mappings",
    values: []
}];

/**
 * Convert the JSON response containing all resource model statistics into
 * data for the scatter plot.
 */
var fillAllModelStatisticsData = function (idx, stat) {
    if (stat.instantiations != 0 && stat.mappings != 0) {
        allModelData[0].values.push({
            x: stat.mappings,
            y: stat.instantiations,
            size: stat.averageMappingDuration.toFixed(2),
            shape: "circle",
            type: stat.type,
            mappableFields: stat.mappableFields,
            avgDuration: stat.averageMappingDuration.toFixed(2),
            durationMedian: stat.mappingDurationMedian.toFixed(2)
        });
    }
};

/**
 * Render the scatter plot for all resource model statistics.
 */
var showScatterplotOfAllModelStatistics = function (modelStatistics) {
    $.each(modelStatistics, fillAllModelStatisticsData);
    var chart;
    nv.addGraph(function () {
        chart = nv.models.scatterChart()
            .tooltipContent(function (key, x, y, elem) {
                return '<h3>' + elem.point.type + '<br />Mappable fields: ' + elem.point.mappableFields + '<br />Average mapping duration: ' + elem.point.avgDuration + ' ms<br />Mapping duration median: ' + elem.point.durationMedian + ' ms</h3>'
            })
            .showDistX(true)
            .showDistY(true)
            .useVoronoi(true)
            .color(d3.scale.category10().range());

        chart.xAxis.tickValues([1, 5, 10, 50, 100, 1000, 10000, 100000]);
        chart.xAxis.axisLabel("Total number of mappings");

        chart.yAxis.tickValues([1, 10, 100, 1000, 10000, 100000]);
        chart.yAxis.axisLabel("Instantiations");

        d3.select('#scatterplot svg')
            .datum(allModelData)
            .transition().duration(500)
            .call(chart);

        nv.utils.windowResize(chart.update);

        chart.dispatch.on('stateChange', function (e) {
            'New State:' , JSON.stringify(e);
        });

        chart.scatter.dispatch.on('elementClick', function(e) {
            loadStaticsOfModel(e.point.type);
        });

        return chart;
    });
};

/**
 * Request the statistical data of a specific resource model type and initialize
 * the bar chart with the mapping time frequencies data.
 */
var loadStaticsOfModel = function (typeName) {
    var barChart = [
        {key: "Mapping duration frequencies", values: []}
    ];
    $.ajax({
        url: "modelmetadata/api/statistics/" + typeName,
        dataType: 'json',
        success: function (modelStatistics) {
            $.each(modelStatistics.mappingDurationFrequencies, function (key, value) {
                barChart[0].values.push({"label": key, "value": value});
            });

            $(document).scrollTop($("#selected-model-name").offset().top - 10);

            $("#selected-model-name").text(typeName);

            nv.addGraph(function () {
                var chart = nv.models.discreteBarChart()
                    .x(function (d) {
                        return d.label
                    })
                    .y(function (d) {
                        return d.value
                    })
                    .staggerLabels(true)
                    .tooltips(false)
                    .showValues(true);

                chart.discretebar.yScale(d3.scale.pow().exponent(.2));
                chart.yAxis.axisLabel("Occurrences");
                chart.xAxis.axisLabel("Duration (ms)");

                d3.select('#barchart svg')
                    .datum(barChart)
                    .transition().duration(500)
                    .call(chart);


                nv.utils.windowResize(chart.update);

                return chart;
            });
        }
    })
};

/**
 * Request the statistical data of all resource models and initialize the scatter plot.
 */
var loadScatterPlot = function () {
    $.ajax({
        url: "modelmetadata/api/statistics",
        dataType: 'json',
        success: showScatterplotOfAllModelStatistics
    });
};

$(function() {
    $("span.modelReference").click(function() {
        loadStaticsOfModel($(this).text());
    });
});

$(loadScatterPlot);

/**
 * Reset all statistical data and re-load initial view afterwards.
 */
function resetResourceModelStatistics() {
    $.ajax({
        url: "modelmetadata/api/reset",
        dataType: 'json',
        success: loadScatterPlot
    });
}
