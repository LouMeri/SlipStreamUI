jQuery( function() { ( function( $$, $, undefined ) {

    // $(document.body).css("padding-top", $(".navbar").height() || 0);

    // $("#form-signin").bootstrapValidator();
    // $("#form-pwd-reset").bootstrapValidator();
    // $("#form-contact-us").bootstrapValidator();

    // $("#contact-request-textarea").maxlength({
    //     placement: "bottom",
    //     threshold: 50,
    //     showCharsTyped: false,
    //     warningClass: "label label-warning",
    //     limitReachedClass: "label label-danger",
    //     preText: "You can still write ",
    //     separator: " chars (",
    //     postText: " max)"
    // });

    if (window.navigator.standalone) {
        // The app is running in standalone mode on a mobile device.
        // Add 20px to the top to accomodate a translucent status bar by css.
        $("body").addClass("ss-standalone-webapp-mobile");
    }

    // Remove secondary menu if no actions
    $(".ss-secondary-menu-main-action span:last-of-type:empty")
        .closest("#ss-secondary-menu")
        .remove();

    // Remove secondary menu dropdown caret if no extra actions
    if ( ! $(".ss-secondary-menu-extra-actions-container .ss-secondary-menu-extra-action:first-child a").length) {
        $(".ss-secondary-extra-actions-toggle").remove();
    }

    if ($("body").hasClass("ss-page-type-chooser")) {
        // Make secondary-menubar fixed to top directly
        $("#ss-secondary-menubar-container").affix({
            offset: {
                top: 0
            }
        });
        $("#ss-secondary-menubar-container").on("affixed.bs.affix", function () {
            $("#ss-secondary-menubar-container.affix").css("top",0);
            $("#ss-secondary-menubar-placeholder").show();
        });
        $("#ss-secondary-menubar-container").on("affixed-top.bs.affix", function () {
            $("#ss-secondary-menubar-placeholder").hide();
        });
    } else {
        // Make secondary-menubar fixed to below the topbar after scrolling past the header
        $("#ss-secondary-menubar-container").affix({
            offset: {
                top: function () {
                    return ($("#header").offset().top + $("#header").outerHeight() - $("#topbar").outerHeight());
                }
            }
        });

        $("#ss-secondary-menubar-container").on("affixed.bs.affix", function () {
            $("#ss-secondary-menubar-container.affix").css("top",$("#topbar").outerHeight());
            $("#ss-secondary-menubar-placeholder").show();
        });
        $("#ss-secondary-menubar-container").on("affixed-top.bs.affix", function () {
            $("#ss-secondary-menubar-container.affix-top").css("top",0);
            $("#ss-secondary-menubar-placeholder").hide();
        });
    }

    // Set up forms

    function updateRequestForModule(request, $form) {
        var module = $form.getSlipStreamModel().module;
        if ($$.util.meta.isPageType("new")) {
            request.url(module.getURI() + "?new=true");
        } else {
            request.url(module.getURI());
        }
        $$.util.form.addHiddenField($form, "name", module.getFullName());
        $$.util.form.addHiddenField($form, "category", module.getCategoryName());

        if (module.isOfCategory("image")) {
            // Add scripts as hidden form fields
            $("pre.ss-code-editor").each(function (){
                var thisId = $(this).attr("id"),
                    code = $$.codeArea.getCode(thisId);
                $$.util.form.addHiddenField($form, thisId + "--script", code);
            });
        }

        return;
    }

    function updateRequestForUser(request, $form) {
        if ($$.util.meta.isPageType("edit")) {
            $$.util.form.addHiddenField($form, "name", $("#name").text());
        }
        request.url("/user/");
        if ($$.util.meta.isPageType("new")) {
            request.settings.url += $("#name").val();
        } else {
            request.settings.url += $("#name").text();
        }
    }

    function updateRequest(request, $form) {
        switch ($$.util.meta.getViewName()) {
            case "user":
                updateRequestForUser(request, $form);
                break;
            case "module":
                updateRequestForModule(request, $form);
                break;
            default:
                // nothing to do
                break;
        }
    }

    $$.request
        .put()
        .onSuccessFollowRedirectInResponseHeader()
        .useToSubmitForm("#save-form", updateRequest);

    $$.request
        .put()
        .onSuccessFollowRedirectInResponseHeader()
        .useToSubmitForm("#create-form", updateRequest);

    // $("body").getSlipStreamModel().module.dump();

}( window.SlipStream = window.SlipStream || {}, jQuery ));});
