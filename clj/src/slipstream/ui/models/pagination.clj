(ns slipstream.ui.models.pagination
  "Used by runs and vms models."
  (:require [net.cgrand.enlive-html :as html]
            [slipstream.ui.util.clojure :as uc]
            [slipstream.ui.util.enlive :as ue]))

(def items-per-page
  "Default value."
  25)

(defn- parse-pagination
  [attrs]
  {:offset      (-> attrs :offset uc/parse-pos-int)
   :limit       (-> attrs :limit uc/parse-pos-int)
   :count-shown (-> attrs :count uc/parse-pos-int)
   :count-total (-> attrs :totalCount uc/parse-pos-int)
   :cloud-name  (-> attrs :cloud)})

(defn parse
  [metadata]
  (-> metadata
      (html/select ue/this)
      first
      :attrs
      parse-pagination))
