jQuery( function() { ( function( $$, $, undefined ) {

    function selectFromCombobox(e) {
        var $combobox = $(e),
            targetTab = $combobox.children("option:selected").attr("value");
            $tabAnchor = $combobox.parent().find("a[href='" + targetTab + "']");
        $tabAnchor.tab("show");
        return;
    }

    function enableSubsections() {
        // Match the height of the tab contents to the highest to avoid jumping
        // NOTE: Doesn't work if the element is not visible on page load (e.g.
        //       in a hidden section) since the height will be 0.
        $(".ss-subsection-group, .ss-subsection-group-stacked").each(function (){
            var $thisTabPanes = $(this).find(".tab-pane"),
                highestHight = Math.max.apply(null,
                    $thisTabPanes.map(function(){
                        return $(this).height();
                    }));
            $thisTabPanes.css("min-height", highestHight);
            return;
        });

        // Sync the combobox .ss-subsection-activator-xs-group at page load
        $(".ss-subsection-activator-xs-group").each(function (){
            var $this = $(this),
                activeSubsectionId = $this.parent().find("li.active a").attr("href");
            $this.val(activeSubsectionId);
        });

        // Enable tab functionality, keeping in sync the combobox .ss-subsection-activator-xs-group
        $(".ss-subsection-activator-group a").click(function (e) {
            var $tabAnchor  = $(this),
                targetTab   = $tabAnchor.attr("href"),
                $combobox   = $tabAnchor.closest("div").find(".ss-subsection-activator-xs-group");
            e.preventDefault();
            $combobox.val(targetTab);
            $tabAnchor.tab("show");
        });

        // Enable ss-subsection-activator-xs-group
        $(".ss-subsection-activator-xs-group").change( function () {
            selectFromCombobox(this);
        });
    }

    enableSubsections();

    var subsectionIdPrefix = "ss-subsection-",
        sep = $$.util.url.hash.segmentSeparator;


    // Ensure correct hash when opening subsections

    $(".ss-subsection-activator-group a[role=tab]").on("shown.bs.tab", function (e) {
        var subsectionTitle = $(this)
                                    .attr("href")
                                    .trimPrefix("#" + subsectionIdPrefix);
        window.location.hash = window.location.hash
                                    .trimFromLastIndexOf(sep) + sep + subsectionTitle;
    });

    $$.subsections = {
        reenableSubsections: enableSubsections,

        showByTitle: function($section, title) {
            return $section
                       .find(".ss-subsection-activator-group")
                           .find("a[href=#" + subsectionIdPrefix + title + "]")
                           .click()
                           .foundOne();
        }
    };

}( window.SlipStream = window.SlipStream || {}, jQuery ));});