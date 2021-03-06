jQuery( function() { ( function( $$, $, undefined ) {

    $$.subsection.onShow(function(subsectionTitle, $subsectionContent) {
        var $dynamicCloudSubsection = $subsectionContent.find(".ss-dynamic-subsection");
        if ($dynamicCloudSubsection.foundNothing()) {
            return;
        }
        $$.request
            .get($dynamicCloudSubsection.data("content-load-url"))
            .dataType("html")
            .onSuccess(function (html){
                var $newContent = $(".ss-section-content", html);
                $dynamicCloudSubsection
                    .children("div:first-of-type")
                        .updateWith($newContent, {flash: true, flashDuration: 140, flashCategory: "transparent"});
            })
            .send();
    });

    function drawGauges(panel) {
        $(".ss-usage-gauge", panel).each(function(idx, elem) {
            var $elem = $(elem).empty();
            new JustGage({
              id: elem.id,
              value: $elem.data('quota-current'),
              min: 0,
              max: $elem.data('quota-max') || 20,
              title: $elem.data('quota-title'),
              levelColorsGradient: true,
              showInnerShadow: false
            });
        });
    }

    drawGauges($("#ss-usage-container").newPanel);

    function drawHistograms(panel) {
        if (panel === undefined) {
            panel = $(".ss-metering");
        }

        var from = $("#ss-metering-selector option:selected").val(),
            options = {
                'from': "-" + from + 's'
            };
        // Fixes GH-164 (https://github.com/slipstream/SlipStreamServer/issues/164)
        // Smooths the graph dependeing on which period we retrieving data from.
        // The online loop send data each 10 seconds whereas the online loop send
        // data each 4 minutes (240 seconds).
        if (from <= 6 * 60 * 60) {
            // For the 10 seconds resolution over 6 hours period
            // we smooth the graph for 24 points (240/10) at most.
            // We also grab more points to fill possible gap between
            // points that we remove before displaying the graph.
            options.target_func = function(target) {
                return 'keepLastValue(' + target + ',24)';
            };
            options.transform_func = function(series) {
                var _series = {};
                $.each(series, function(service, series) {
                    _series[service] = series.slice(24);
                });
                return _series;
            };
        } else if (from <= 7 * 24 * 60 * 60) {
            // For the 1 minute (60 seconds) resolution over 7 days period
            // we smooth the graph for 4 points (240/60) at most.
            // We also grab more points to fill possible gap between
            // points that we remove before displaying the graph.
            options.target_func = function(target) {
                return 'keepLastValue(' + target + ',4)';
            };
            options.transform_func = function(series) {
                var _series = {};
                $.each(series, function(service, series) {
                    _series[service] = series.slice(4);
                });
                return _series;
            };
        }
        $(panel).metrics(options);
    }

    $("#ss-metering-selector").change(function() {
        drawHistograms();
    });

    drawHistograms();

}( window.SlipStream = window.SlipStream || {}, jQuery ));});
