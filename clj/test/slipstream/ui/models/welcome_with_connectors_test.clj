(ns slipstream.ui.models.welcome-with-connectors-test
  (:use [expectations])
  (:require [slipstream.ui.util.core :as u]
            [slipstream.ui.util.clojure :as uc]
            [slipstream.ui.util.localization :as localization]
            [slipstream.ui.util.current-user :as current-user :refer [with-user-from-metadata]]
            [slipstream.ui.models.welcome :as model]))

(def raw-metadata-str
  (uc/slurp-resource "slipstream/ui/mockup_data/metadata_welcome_with_connectors.xml"))

(def ^:private welcome-metadata
  (u/clojurify-raw-metadata-str raw-metadata-str))

(def parsed-metadata
  {:published-apps []
   :projects []})

; (expect
;   parsed-metadata
;   (-> welcome-metadata model/parse))

(expect
  "Cloud1"
  (let [metadata welcome-metadata]
    (localization/with-lang :en
      (with-user-from-metadata
        (-> :available-clouds
            current-user/configuration
            u/enum-default-option
            :value)))))

(expect
  #{"Cloud2"}
  (let [metadata welcome-metadata]
    (localization/with-lang :en
      (with-user-from-metadata
        (-> :configured-clouds
            current-user/configuration)))))