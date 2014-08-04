(ns slipstream.ui.views.content
  (:require [slipstream.ui.views.section :as section]))

(defn build
  [content]
  (if (vector? content)
    (section/build content)
    (throw (Exception. "Not yet implemented"))))