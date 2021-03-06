(ns slipstream.ui.views.dashboard
  (:require [clojure.string :as s]
            [net.cgrand.enlive-html :as html]
            [slipstream.ui.util.core :as u]
            [slipstream.ui.util.enlive :as ue]
            [slipstream.ui.util.icons :as icons]
            [slipstream.ui.util.localization :as localization]
            [slipstream.ui.util.current-user :as current-user]
            [slipstream.ui.models.pagination :as pagination]
            [slipstream.ui.models.dashboard :as dashboard]
            [slipstream.ui.views.tables :as t]
            [slipstream.ui.views.base :as base]))

(localization/def-scoped-t)

(defmulti ^:private section (comp second vector))

;; Usage section

(ue/def-blank-snippet ^:private usage-snip [:div :div]
  [usage]
  ue/this (ue/set-class "ss-usage-container")
  ue/this (ue/content-for [:div :div] [cloud-usage usage]
              ue/this (ue/set-id "ss-usage-gauge-" (:cloud cloud-usage))
              ue/this (ue/set-class "ss-usage-gauge")
              ue/this (html/set-attr :data-quota-title (:cloud cloud-usage))
              ue/this (html/set-attr :data-quota-max (:quota cloud-usage))
              ue/this (html/set-attr :data-quota-current (:current-usage cloud-usage))))

(defmethod section ::quota
  [dashboard metadata-key]
  {:title   (localization/section-title metadata-key)
   :content (-> dashboard :quota :usage usage-snip)})

;; Runs and VMs section

(def ^:private spinner-icon
  (ue/blank-node :span :class "glyphicon glyphicon-refresh ss-subsection-content-spinner"))

(ue/def-blank-snippet ^:private dynamic-cloud-subsection-content-snip [:div :div]
  [metadata-key cloud-name]
  ue/this     (html/add-class "ss-dynamic-subsection")
  [:div :div] (html/add-class "ss-dynamic-subsection-content")
  ue/this     (ue/set-data :content-load-url (format "/%s?cloud=%s&offset=0&limit=%s"
                                                     ({::vms "vms"
                                                       ::runs "run"} metadata-key)
                                                     cloud-name
                                                     pagination/items-per-page))
  ue/this     (ue/set-id (name metadata-key) "-" cloud-name)
  [:div :div] (html/content spinner-icon))

(defn- cloud-subsection
  [metadata-key cloud-name]
  {:title   cloud-name
   :content (dynamic-cloud-subsection-content-snip metadata-key cloud-name)})

(defmethod section ::dynamic-cloud-subsection
  [{:keys [clouds]} metadata-key]
  {:title   (localization/section-title metadata-key)
   :content (map (partial cloud-subsection metadata-key) clouds)})

;; Metering section

(localization/with-prefixed-t :metering-options
  (defn- metering-options
    []
    [{:value 3840,    :text (t :last-hour)}
     {:value 86640,   :text (t :last-day)}
     {:value 604800,  :text (t :last-7-days)}
     {:value 2592000, :text (t :last-30-days)}]))

(ue/def-blank-snippet ^:private metering-selector-snip [:select :option]
  [options]
  [:select] (ue/set-id "ss-metering-selector")
  [:select] (ue/content-for [:option] [{:keys [value text]} options]
              ue/this (ue/set-value value)
              ue/this (html/content (str text))))

(def ^:private metering-metrics
  [
   :instance
   ; :vcpus
   ; :memory
   ; :disk
   ])

(defn- data-metric-value
  [metric]
  (format "slipstream.%s.usage.%s.*"
          (s/replace (current-user/username) "." "_")
          (name metric)))

(ue/def-blank-snippet ^:private metering-subsection-snip :div
  [metric]
  ue/this (ue/set-class "ss-metering metric")
  ue/this (html/set-attr :data-metric (data-metric-value metric))
  ue/this (html/prepend (metering-selector-snip (metering-options))))

(localization/with-prefixed-t :metering.metric
  (defn- metering-subsection
    [metric]
    {:title (t metric)
     :content (metering-subsection-snip metric)}))

(defmethod section ::metering
  [dashboard metadata-key]
  {:title   (localization/section-title metadata-key)
   :content (map metering-subsection metering-metrics)})

(derive ::runs ::dynamic-cloud-subsection)
(derive ::vms  ::dynamic-cloud-subsection)

(defn- sections
  [dashboard]
  (cond-> []
    (-> dashboard :quota :enabled?)     (conj ::quota)
    :always                             (conj ::runs)
    :always                             (conj ::vms)
    (-> dashboard :metering :enabled?)  (conj ::metering)))

(def ^:private html-dependencies
  {:css-filenames ["dashboard.css"]
   :external-js-filenames (concat
                            (map (partial format "jquery-flot/js/jquery.flot%s.min.js")
                                 ["" ".pie" ".time" ".stack" ".tooltip" ".resize"])
                            (map (partial format "justgage/js/%s.min.js")
                                 ["raphael.2.1.0" "justgage.1.0.1"]))
   :internal-js-filenames ["metering.js" "dashboard.js"]})

(defn page
  [metadata]
  (let [dashboard (dashboard/parse metadata)]
    (base/generate
      {:html-dependencies html-dependencies
       :header {:icon     icons/dashboard
                :title    (t :header.title)
                :subtitle (t :header.subtitle)}
       :resource-uri "/dashboard"
       :content (->> dashboard
                     sections
                     (map (partial section dashboard))
                     flatten)})))
