jQuery( function() { ( function( $$, $, undefined ) {

    $("#ss-save-dialog .ss-save-btn").click(function() {
        var $saveForm = $("#save-form");
        $$.util.form.addHiddenField($saveForm, "comment", $("#ss-commit-message").val());
        $saveForm.submit();
    });

    $("#ss-delete-dialog .ss-delete-btn").click(function() {
        $$.request
            .delete($$.util.url.getCurrentURLBase())
            .onSuccessRedirectURL($$.util.url.getParentResourceURL())
            .onErrorAlert("Unable to delete",
                "Something wrong happened when trying to delete this resource." +
                " Maybe the server is unreachable, or the connection is down." +
                "Please try later again.")
            .send();
    });

}( window.SlipStream = window.SlipStream || {}, jQuery ));});


