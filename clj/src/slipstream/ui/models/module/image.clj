(ns slipstream.ui.models.module.image
  (:require [net.cgrand.enlive-html :as html]
            [slipstream.ui.util.core :as u]
            [slipstream.ui.util.clojure :as uc]
            [slipstream.ui.util.page-type :as page-type]
            [slipstream.ui.util.localization :as localization]
            [slipstream.ui.models.runs :as runs]
            [slipstream.ui.models.parameters :as parameters]))

(localization/def-scoped-t)

;; Cloud image section

(defn- cloud-image-details
  [metadata]
  {:native-image?     (-> metadata :attrs :isBase uc/parse-boolean)
   :cloud-identifiers (->> (html/select metadata [:cloudImageIdentifier])
                           (map :attrs)
                           (map (juxt :cloudServiceName :cloudImageIdentifier))
                           (into (zipmap
                                   (->> (html/select metadata [:cloudNames :string html/text])
                                        (remove #{"default"}))
                                   (repeat nil))))
   :reference-image   (-> metadata :attrs :moduleReferenceUri (uc/trim-prefix "module/"))})


;; Os details section

(def ^:private platforms
  ["centos"
   "debian"
   "fedora"
   "opensuse"
   "redhat"
   "sles"
   "ubuntu"
   "windows"
   "other"])

(defn- os-details
  [metadata]
  {:platform         (->> metadata :attrs :platform (u/enum platforms :cloud-platforms))
   :login-username   (-> metadata :attrs :loginUser)})


;; Image creation metadata section

(defn- parse-recipe
  [recipe-type metadata]
  {:code (-> metadata (html/select [recipe-type html/text-node]) first)})

(defn- parse-package
  [package-metadata]
  (let [attrs (:attrs package-metadata)]
    (-> attrs
        (select-keys [:key
                      :name
                      :repository]))))

(defn- image-creation
  [metadata]
  {:recipe      (parse-recipe :recipe metadata)
   :packages    (->> (html/select metadata [:package])
                     (map parse-package)
                     (sort-by :name))
   :pre-recipe  (parse-recipe :prerecipe metadata)})


;; Deployment metadata section

(def ^:private target-names
  {:execute       "execute"
   :report        "report"
   :parameters    "parameters"
   :on-vm-add     "onvmadd"
   :on-vm-remove  "onvmremove"
   :pre-scale     "prescale"
   :post-scale    "postscale"})

(defn- assoc-target
  [m target metadata]
  (let [target-metadata (-> metadata
                            (html/select [[:target (html/attr= :name (target-names target))]])
                            first)]
    (assoc-in m [:targets target]
        {:code              (-> target-metadata (html/select [html/text-node]) first)
         :run-in-background (-> target-metadata :attrs :runInBackground uc/parse-boolean)})))

(def ^:private default-parameters-order
  {"instanceid" 1
   "hostname"   2})

(defn- transform-default-parameters
  [parameter]
  (if-let [new-order (-> parameter :name default-parameters-order)]
    (assoc parameter
      :order new-order
      :disabled? true
      :description  (->> parameter :name (format "deployment.default-parameter.%s.description") t)
      :placeholder  (->> parameter :name (format "deployment.default-parameter.%s.placeholder") t)
      :help-hint    (->> parameter :name (format "deployment.default-parameter.%s.help-hint") t))
    parameter))

(defn- deployment-parameters
  [parameters]
  (->> (parameters/categories-of-type parameters :deployment)
       parameters/flatten
       (map transform-default-parameters)
       (sort-by (juxt :order :category :name))
       vec))

(defn- deployment
  [metadata parameters]
  (-> {:parameters (deployment-parameters parameters)}
      (assoc-target :execute metadata)
      (assoc-target :report metadata)
      (assoc-target :pre-scale metadata)
      (assoc-target :post-scale metadata)
      (assoc-target :on-vm-add metadata)
      (assoc-target :on-vm-remove metadata)))


;; Metadata section build

(defn sections
  [metadata]
  (let [parameters (parameters/parse metadata)]
    (cond->
        {:cloud-image-details         (cloud-image-details metadata)
         :os-details                  (os-details metadata)
         :cloud-configuration         (parameters/categories-of-type parameters :global)
         :image-creation              (image-creation metadata)
         :deployment                  (deployment metadata parameters)}
      (page-type/view?) (assoc :runs  (runs/parse metadata)))))

