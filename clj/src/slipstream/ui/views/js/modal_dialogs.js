jQuery( function() { ( function( $$, $, undefined ) {

    $("#ss-save-dialog .ss-save-btn").click(function() {
        console.log("saving with commit message: " + $("#ss-commit-message").val());
        $("#save-form").submit();
    });

    $("#ss-delete-dialog .ss-delete-btn").click(function() {
        console.log("saving with commit message: " + $("#ss-commit-message").val());
        var baseURL = $$.util.url.getCurrentURLBase();
        // ,
        //     parentResourceURL ;
        $$.request
            .delete(baseURL)
            .onSuccessRedirectURL(baseURL)
            .onErrorAlert("Unable to delete",
                "Something wrong happened when trying to delete this resource." +
                " Maybe the server is unreachable, or the connection is down." +
                "Please try later again.")
            .send();
    });

}( window.SlipStream = window.SlipStream || {}, jQuery ));});


