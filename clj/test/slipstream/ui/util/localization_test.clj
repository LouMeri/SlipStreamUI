(ns slipstream.ui.util.localization-test
  (:use [expectations]
        [slipstream.ui.util.localization]))

(expect
  nil
  (@#'slipstream.ui.util.localization/replace-nil-args-with-blank-str nil))

(expect
  nil
  (@#'slipstream.ui.util.localization/replace-nil-args-with-blank-str []))

(expect
  ["foo" "bar"]
  (@#'slipstream.ui.util.localization/replace-nil-args-with-blank-str ["foo" "bar"]))

(expect
  ["" "foo" "" "bar"]
  (@#'slipstream.ui.util.localization/replace-nil-args-with-blank-str [nil "foo" nil "bar"]))

(expect
  ["" "" "" ""]
  (@#'slipstream.ui.util.localization/replace-nil-args-with-blank-str [nil "" nil ""]))

(expect
  ["" "" "" ""]
  (@#'slipstream.ui.util.localization/replace-nil-args-with-blank-str [nil nil nil nil]))
