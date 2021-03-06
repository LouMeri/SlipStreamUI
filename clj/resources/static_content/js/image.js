jQuery( function() { ( function( $$, $, undefined ) {

    function toggleBaseImageFormInputs(isBase) {
        // Toggle as well the two form inputs in the section "Operating System Details" below.
        $(".cloudimageid_imageid_, #platform, #loginUser")
            .closest("tr")
                .enableRow(isBase,
                    {disableReason: "This can only be set up for native (aka 'base') images."});
        $(".ss-reference-module-name")
            .closest("tr")
                .enableRow(! isBase,
                    {disableReason: "A native (aka 'base') image does not need a reference module."});
    }

    var $isBaseCheckbox = $("input#isbase");

    $isBaseCheckbox.change(function() {
        var isBase = $(this).prop("checked");
        toggleBaseImageFormInputs(isBase);
    });

    // Trigger the 'change' event for set up the correct state of the forms.
    $isBaseCheckbox.change();

    // SSH option
    var $needSSHAccessElem = $("#ssh-access-enabled");

    $needSSHAccessElem
        .change(function(){
            if ( ! this.checked ) {
                $needSSHAccessElem.clearFormInputValidationState();
            } else {
                $needSSHAccessElem.setFormInputValidationState(false);
            }
        })
        .removeAttr("name"); // To prevent sending this value with the 'run' request, since it's not required


}( window.SlipStream = window.SlipStream || {}, jQuery ));});
