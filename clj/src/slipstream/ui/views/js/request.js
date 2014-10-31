jQuery( function() { ( function( $$, $, undefined ) {

    function builder(method, url) {
        return {
            intern: {
                serialization: undefined,           // See .serialization() fn below
                onDataTypeParseError: undefined,    // See .onDataTypeParseErrorAlert() fn below
                always: undefined                   // See .always() fn below
            },
            settings: {
                type: method,     // values: "GET", "POST", "PUT", "DELETE"
                url: url,
                data: undefined,
                dataType: undefined,    // See .dataType() fn below
                contentType: undefined, // See .serialization() fn below
                success: undefined,     // See .onSuccess() fn below
                error: undefined        // See .onError() fn below
            },
            always: function (callback){
                // An alternative construct to the complete callback
                // option, the .always() method replaces the deprecated
                // .complete() method.

                // NB: Callback fn which doesn't belong to the ajax settings but
                // on the returned Promise object.

                // Callback signature: jqXHR.always(function( data|jqXHR, textStatus, jqXHR|errorThrown ) { });
                this.intern.always = callback;
                return this;
            },
            dataObject: function (object) {
                this.settings.data = object;
                return this;
            },
            dataType: function (type) {
                // The type of data that you're expecting back from the server. If
                // none is specified, jQuery will try to infer it based on the MIME
                // type of the response
                // 'type' must be one among: 'xml', 'json', 'script', or 'html'.
                this.settings.dataType = type;
                return this;
            },
            onSuccess: function (callback){
                // A function to be called if the request succeeds.
                // The function gets passed three arguments: The data
                // returned from the server, formatted according to
                // the dataType parameter; a string describing the
                // status; and the jqXHR object. If several callback
                // are passed, they will be called in turn.

                // Callback signature: function (data, textStatus, jqXHR) {}

                $$.util.setOrPush(this.settings, "success", callback);
                // this.settings.success = callback;
                return this;
            },
            onSuccessReloadPageWithoutQueryParamsInURL: function (){
                this.onSuccess(function () {
                    $$.util.url.redirectToCurrentURLBase();
                });
                return this;
            },
            onSuccessRedirectURL: function (url){
                this.onSuccess(function () {
                    $$.util.url.redirectTo(url);
                });
                return this;
            },
            onSuccessFollowRedirectInURL: function (){
                this.onSuccess(function () {
                    var redirectURL = $$.util.urlQueryParams.getValue("redirectURL"),
                        rootURL = "/";
                    $$.util.url.redirectTo(redirectURL || rootURL);
                });
                return this;
            },
            onSuccessFollowRedirectInResponseHeader: function (){
                this.onSuccess(function (data, textStatus, jqXHR) {
                    console.log(jqXHR.getResponseHeader("Location"));
                    $$.util.url.redirectTo(jqXHR.getResponseHeader("Location"));
                });
                return this;
            },
            onSuccessAlert: function (titleOrMsg, msg){
                var showSuccessAlert = function () { $$.Alert.showSuccess(titleOrMsg, msg); };
                this.onSuccess(showSuccessAlert);
                // this.settings.success = showSuccessAlert;
                return this;
            },
            onError: function (callback){
                // A function to be called if the request fails. The
                // function receives three arguments: The jqXHR
                // object, a string describing the type of error that
                // occurred and an optional exception object, if one
                // occurred. Possible values for the second argument
                // (besides null) are "timeout", "error", "abort", and
                // "parsererror". When an HTTP error occurs,
                // errorThrown receives the textual portion of the
                // HTTP status, such as "Not Found" or "Internal
                // Server Error."

                // Callback signature: function (jqXHR, textStatus, errorThrown) {}
                $$.util.setOrPush(this.settings, "error", callback);
                // this.settings.error = callback;
                return this;
            },
            onErrorAlert: function (titleOrMsg, msg){
                var showErrorAlert = function () { $$.Alert.showError(titleOrMsg, msg); };
                this.onError(showErrorAlert);
                return this;
            },
            onErrorStatusCodeAlert: function (statusCode, titleOrMsg, msg){
                var callback = function (jqXHR, textStatus, errorThrown) {
                    if (statusCode == jqXHR.status) {
                        $$.Alert.showError(titleOrMsg, msg);
                    }
                };
                this.onError(callback);
                return this;
            },
            onDataTypeParseErrorAlert: function (titleOrMsg, msg){
                // When a dataType is set, a default error alert is configured
                // when sending the request (see below).
                // onDataTypeParseErrorAlert(titleOrMsg, msg) overrides the default
                // alert.
                this.intern.onDataTypeParseError = function () {
                    $$.Alert.showError(titleOrMsg, msg);
                };
                return this;
            },
            serialization: function (serialization) {
                // The 'serialization' is used on the .send() fn below to set the
                // 'contentType' and to serialize the data object accordingly
                // jQuery defaults to "application/x-www-form-urlencoded; charset=UTF-8"
                // 'serialization' must be one among: 'json' or 'queryString'.
                this.intern.serialization = serialization;
                return this;
            },
            send: function () {
                switch (this.intern.serialization) {
                case "json":
                    this.settings.contentType = "application/json; charset=UTF-8";
                    if (this.settings.data) {
                        this.settings.data = JSON.stringify(this.settings.data);
                    }
                    break;
                case undefined:
                    // jQuery would handle this case per default in the same way,
                    // setting 'contentType' to "application/x-www-form-urlencoded; charset=UTF-8"
                    // but we do it explicitely to remove black magic.
                case "queryString":
                    this.settings.contentType = "application/x-www-form-urlencoded; charset=UTF-8";
                    if (this.settings.data) {
                        this.settings.data = $.param(this.settings.data);
                    }
                    break;
                default:
                    throw new Error( "Serialization type '" +
                        this.intern.serialization +
                        "' is not supported." +
                        " Try 'json' or 'queryString'.");
                }
                if (this.settings.dataType) {
                    var dataType = this.settings.dataType;
                    this.onError(
                        this.intern.onDataTypeParseError ||
                        function () {
                            $$.Alert.showError(
                                "AJAX Request Error",
                                "Unable to parse the data received from the server as '" + dataType + "'."
                                );
                        });
                }
                return jQuery.ajax(this.settings);
            },
            url: function (url) {
                this.settings.url = url;
                return this;
            },
            useToSubmitForm: function (sel, preSubmitCallback) {
                var request = this,
                    $form = $("form" + sel),
                    url = request.settings.url || $form.attr("action");
                // StatusCode 0: No internet connection.
                request.onErrorStatusCodeAlert(0, "Something strange out there",
                    "Sorry, but we're having trouble connecting to SlipStream. This problem is" +
                     "usually the result of a broken Internet connection. You can try" +
                     "refreshing this page and doing the request again.")
                    // .serialization("json")
                    .url(url);
                $form.off("submit");

                $form.submit(function (event) {
                    if ($form.data("submitted") === true) {
                        // Previously submitted - don't submit again
                        return false;
                    }
                    // Mark it so that the next submit can be ignored
                    $form.data("submitted", true);
                    event.preventDefault();

                    // The preSubmitCallback(request, $form) can be used to customise
                    // the request and the form before it's submitted.
                    if ( $.isFunction(preSubmitCallback) ) {
                        preSubmitCallback(request, $form);
                    }

                    console.log(">>>> Form values to be sent:");
                    console.log($(this).serializeObject());
                    console.log("<<<<");
                    request.dataObject($(this).serializeObject())
                        .always(request.intern.always)
                        .always(function () {
                            // Mark it so that the next submit can be performed
                            $form.data("submitted", false);
                        })
                        .send();
                    return false;
                });
            }
        };
    }

    $$.request = {
        get: function (url) {
            return builder("GET", url);
        },
        put: function (url) {
            return builder("PUT", url);
        },
        delete: function (url) {
            return builder("DELETE", url);
        },
        post: function (url) {
            return builder("POST", url);
        }
    };

}( window.SlipStream = window.SlipStream || {}, jQuery ));});
