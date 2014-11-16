jQuery( function() { ( function( $$, util, $, undefined ) {

    // String object prototype extensions

    $.extend(String.prototype, {

        startsWith: function(str) {
            return (this.match("^" + str) == str);
        },

        endsWith: function(str) {
            return (this.match(str + "$") == str);
        },

        trimFromLastIndexOf: function(str) {
            var lastIndexOfStr = this.lastIndexOf(str);
            if (lastIndexOfStr === -1) {
                return this.toString();
            } else {
                return this.substring(0, lastIndexOfStr);
            }
        },

        trimLastURLSegment: function() {
            return this.trimFromLastIndexOf("/");
        },

        trimUpToFirstIndexOf: function(str) {
            // Remove all chars from 'this' up to and including 'str'.
            var firstIndexOfStr = this.indexOf(str);
            if (firstIndexOfStr === -1) {
                return this.toString();
            } else {
                return this.substring(firstIndexOfStr + 1);
            }
        },

        trimUpToLastIndexOf: function(str) {
            // Remove all chars from 'this' up to and including 'str'.
            var lastIndexOfStr = this.lastIndexOf(str);
            if (lastIndexOfStr === this.length) {
                return this.toString();
            } else {
                return this.substring(lastIndexOfStr + 1, this.length);
            }
        },

        trimPrefix: function(prefix) {
            // Remove 'prefix' string from the begining of 'this' string.
            var firstIndexOfStr = this.indexOf(prefix);
            if (firstIndexOfStr !== 0) {
                return this.toString();
            } else {
                return this.substring(prefix.length, this.length);
            }
        },

        ensurePrefix: function(prefix) {
            // Ensure that 'this' string begins with the 'prefix' string.
            var firstIndexOfStr = this.indexOf(prefix);
            if (firstIndexOfStr === 0) {
                return this.toString();
            } else {
                return prefix + this;
            }
        },

        prefixWith: function(prefix) {
            return prefix + this;
        },

        removeLeadingSlash: function() {
            return this.trimPrefix("/");
        },

        trimSuffix: function(suffix) {
            // Remove 'suffix' string from the end of 'this' string.
            var lastIndexOfStr = this.lastIndexOf(suffix),
                newLength = this.length - suffix.length;
            if (lastIndexOfStr !== newLength) {
                return this.toString();
            } else {
                return this.substring(0, newLength);
            }
        },

        mightBeAnEmailAddress: function() {
            // As mentioned in http://stackoverflow.com/a/202528 the RFC of the
            // format of email address is so complex, that the only real way to
            // validate it is to send it an email ;)
            // How ever we can perform a basic validation to catch basic things:
            return this.match(".+\\@.+\\..+") ? true : false;
        }
    });


    // Array object prototype extensions

    $.extend(Array.prototype, {
        call: function(thisArg, arg1, arg2, arg3, arg4) {
            // Equivalent to Function.prototype.call() on an Array of fns.
            this.forEach(function(f){
                if ($.isFunction(f)) {
                    f.call(thisArg, arg1, arg2, arg3, arg4);
                }
            });
        }
    });


    // Boolean object prototype extensions

    $.extend(Boolean.prototype, {
        not: function() {
            // Might be more clear to use in long jQuery chined API calls than
            // a mere '!' at the very begining of the call.
            return ! this.valueOf();
        }
    });


    // jQuery extensions

    $.fn.extend({
        foundNothing: function() {
            // A more idiomatic way to check if a jQuery selection has no matches.
            return this.length === 0;
        },

        foundAny: function() {
            // A more idiomatic way to check if a jQuery selection has any matches.
            return this.length !== 0;
        },

        foundOne: function() {
            // A more idiomatic way to check if a jQuery selection has one match.
            return this.length === 1;
        },

        foundMany: function() {
            // A more idiomatic way to check if a jQuery selection has more than one match.
            return this.length > 1;
        },

        // Toggle disabled status of buttons, inputs and anchors
        // Inspired from: http://stackoverflow.com/a/16788240
        disable: function(state) {
            return this.each(function() {
                var $this = $(this);
                if($this.is('input, button'))
                  this.disabled = state;
                else
                  $this.toggleClass('disabled', state);
            });
        },
        enable: function(state) {
            return this.disable(!state);
        },

        // Inspired from: http://stackoverflow.com/a/1186309
        // If more complex form serialization is needed, see https://github.com/macek/jquery-serialize-object
        serializeObject: function () {
            var o = {};
            var a = this.serializeArray();
            $.each(a, function() {
                $$.util.setOrPush(o, this.name, this.value);
            });
            return o;
        },

        onAltEnterPress: function (callback) {
            if ($.isFunction(callback)) {
                $(this).keypress(function (e) {
                    if (e.altKey && e.keyCode === 13) {
                        e.preventDefault();
                        callback.call(this);
                      }
                });
            }
            return this;
        },

        clickWhenEnabled: function (callback) {
            // <a> tags do not honor the 'disabled' property. Therefore, Bootstrap
            // offers the helper class 'disabled', to make them look like so.
            // However they are still clickable (the .click() callback will still
            // be called), and therefor we have to disable it manually here.
            var $this = $(this);
            $this.click( function (event) {
                if (! $this.hasClass("disabled") && $.isFunction(callback)) {
                    callback.call($this, event);
                } else if ($this.attr("disabled-reason") === "ss-super-only-action") {
                    $$.alert.showWarning("Your must be have administrator access to perform this action.");
                }
            });
            return this;
        },

        addFormHiddenField: function (fieldName, fieldValue) {
            $(this).filter("form").each(function () {
                var $form = $(this);
                // Clean up hidden field with the same name before adding it.
                $form.children("input:hidden[name=" + fieldName + "]").remove();
                $("<input>")
                    .attr("type", "hidden")
                    .attr("name", fieldName)
                    .attr("value", fieldValue)
                    .appendTo($form);
            });
            return this;
        },

        setFormField: function (fieldName, fieldValue) {
            $(this).filter("form").each(function () {
                var $form = $(this);
                // If the field doesn't exists already in the form, this is a no-op.
                $form.find("input[name=" + fieldName + "]").val(fieldValue);
            });
            return this;
        },

        toggleFormInputValidationState: function (state) {
            var $this = $(this),
                $formGroup = $this.closest(".form-group");
            if ($formGroup.foundNothing()) {
                throw "No .form-group element can be found from jQuery selection.";
            }
            if ($.type(state) === "boolean") {
                $formGroup
                    .toggleClass("has-success", state)
                    .toggleClass("has-error",  !state);
            } else {
                // If the 'state' is not a boolean, we toggle the current state
                $formGroup
                    .toggleClass("has-success")
                    .toggleClass("has-error");
            }
            // Do the same with the submit button, if no .has-error in form
            var hasErrors = $this.closest("form").find(".has-error").foundAny();
            $this
                .closest("form")
                .find("button[type=submit]")
                .disable(hasErrors);
            return this;
        },

        focusFirstInput: function() {
            var $firstElem = $(this).find("input[type=text], textarea").first();
            if ($firstElem.foundNothing()) {
                return this;
            }
            var strLength= $firstElem.val().length * 2; // x 2 to ensure cursor always ends up at the end
            $firstElem.focus();
            $firstElem[0].setSelectionRange(strLength, strLength);
            return this;
        },

        onTextInputChange: function(callback) {
            var $textInputFields = $(this).filter("input[type=text], textarea");
            if ($textInputFields.foundNothing()) {
                return this;
            }
            // Inspired from: http://stackoverflow.com/a/6458946
            $textInputFields.on('input', callback);
            return this;
        },

        askConfirmation: function (callbackOnOKButtonPress) {
            var $modalDialog = $(this).filter("div.modal");
            if ($modalDialog.foundNothing()) {
                throw "No modal dialog in jQuery selection.";
            }
            if ($modalDialog.length !== 1) {
                throw "More than one modal dialog in jQuery selection. Please select only one.";
            }
            if (callbackOnOKButtonPress &&
                $modalDialog.data("callbackOnOKButtonPress") + "" !== callbackOnOKButtonPress + "") {
                // Add the callbackOnOKButtonPress only once, not on every askConfirmation event
                $modalDialog.find(".ss-ok-btn").on("click", callbackOnOKButtonPress);
                $modalDialog.data("callbackOnOKButtonPress", callbackOnOKButtonPress);
            }
            $modalDialog.modal("show");
            return this;
        },

        scheduleAlertDismiss: function(millis){
            var $alertElem = $(this).filter("div[role=alert]:not(.hidden)");
            $alertElem.data("dismissTimeout", setTimeout(function(){
                $alertElem.hide("slow", function() {
                    $alertElem.remove();
                });
            }, millis || 5000));
            if (! $alertElem.data("mouseHandlersAlreadySetup")) {
                // Schedule mouse handlers only once
                $alertElem.mouseenter(function() {
                    // Cancelling alert dismiss
                    clearTimeout($alertElem.data("dismissTimeout"));
                });
                $alertElem.mouseleave(function() {
                    // Scheduling new dismiss
                    $alertElem.scheduleAlertDismiss(millis);
                });
                $alertElem.data("mouseHandlersAlreadySetup", true);
            }
            return this;
        }

    });

    util.string = {
        caseInsensitiveEqual: function (str1, str2) {
            if ($.type(str1) !== "string" || $.type(str2) !== "string") {
                return false;
            }
            return str1.toUpperCase() ===  str2.toUpperCase();
        }
    };

    // TODO: How to extend Object to have this function?
    // $.fn.setOrPush = function (key, value) {
    // Object.prototype.setOrPush = function (key, value) {
    util.setOrPush = function (object, key, value) {
        if (object[key] !== undefined) {
            if (!object[key].push) {
                object[key] = [object[key]];
            }
            object[key].push(value || '');
        } else {
            object[key] = value || '';
        }
        return object;
    };

    util.url = {
        getCurrentURLBase: function () {
            var path = window
                        .location
                        .pathname  // URL without query params
                        .trimSuffix("/new")
                        .trimSuffix("/module");
            return path;
        },
        getParentResourceURL: function () {
            var path = window.location.pathname; // URL without query params
            return path.trimLastURLSegment();
        },
        redirectTo: function (url) {
            // TODO: Which one if the correct way to redirect?
            // window.location = url;
            return window.location.assign(url);
        },
        reloadPageWithoutHashInURL: function () {
            return this.redirectTo(window.location.href.split('#')[0]);
        },
        redirectToCurrentURLBase: function () {
            return this.redirectTo(this.getCurrentURLBase());
        },
        redirectToParentResourceURL: function () {
            return this.redirectTo(this.getParentResourceURL());
        }
    };

    util.urlQueryParams = {
        // If the query param key string in not contained in any other key, this is faster:
        getValue: function (param) {
            try {
                return window.location.search.split(param+"=")[1].split("&")[0];
            } catch (e) {
                return undefined;
            }
        },
        getValuePrecise: function (param) {
            var query = window.location.search;
            if (!query) {
                return undefined;
            }
            var entries = query.substring(1,query.length).split("&");
            for (var index in entries){
                var keyVal = entries[index].split("=");
                if (keyVal[0] == param){
                    return keyVal[1];
                }
            }
        }
    };

    util.meta = {
        getMetaValue: function (name, $elem) {
            if ($elem) {
                return $elem.find("meta[name=" + name + "]").attr("content");
            } else {
                return $("meta[name=" + name + "]").attr("content");
            }
        },
        getPageType: function ($elem) {
            // Page type is one of 'view', 'edit', 'new', 'chooser', etc...
            // as in the slipstream.ui.util.page-type Clojure namespace.
            return this.getMetaValue("ss-page-type", $elem);
        },
        isPageType: function (pageType, $elem) {
            return util.string.caseInsensitiveEqual(this.getPageType($elem), pageType);
        },
        getUserType: function ($elem) {
            // User type is one of 'super' or 'regular',
            // as in the slipstream.ui.util.curent-user/type-name Clojure fn.
            return this.getMetaValue("ss-user-type", $elem);
        },
        isSuperUserLoggegIn: function ($elem) {
            return util.string.caseInsensitiveEqual(this.getUserType($elem), "super");
        },
        getViewName: function ($elem) {
            // View name is one of 'user', 'module', 'dashboard', etc...
            // Technically it is the last segment of the view's Clojure namespace.
            // See clj/src/slipstream/ui/views/base.clj:215 or nearby ;)
            return this.getMetaValue("ss-view-name", $elem);
        },
        isViewName: function (viewName, $elem) {
            return util.string.caseInsensitiveEqual(this.getViewName($elem), viewName);
        }
    };

}( window.SlipStream = window.SlipStream || {}, window.SlipStream.util = {}, jQuery ));});
