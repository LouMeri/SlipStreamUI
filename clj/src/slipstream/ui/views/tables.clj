(ns slipstream.ui.views.tables
  "Predefined table rows."
  (:require [clojure.string :as s]
            [slipstream.ui.util.core :as u]
            [slipstream.ui.util.clojure :as uc]
            [slipstream.ui.util.page-type :as page-type]
            [slipstream.ui.util.current-user :as current-user]
            [slipstream.ui.util.localization :as localization]
            [slipstream.ui.util.icons :as icons]
            [slipstream.ui.views.table :as table]
            [slipstream.ui.models.parameters :as p]))

(localization/def-scoped-t)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- remove-button-cell
  [& {:keys [item-name size]}]
  (when (page-type/edit-or-new?)
    {:type :cell/remove-row-button
     :content {:item-name item-name
               :size size}}))

(defn- append-blank-row-in-edit-mode
  ([rows]
   (append-blank-row-in-edit-mode nil rows))
  ([blank-row rows]
   (if (page-type/edit-or-new?)
     (conj (vec rows) (assoc (or blank-row {}) :blank-row? true))
     rows)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- welcome-project-row
  [{:keys [name uri description owner version] :as welcome-project}]
  {:style nil
   :cells [{:type :cell/icon, :content icons/project}
           {:type :cell/link, :content {:text name, :href uri}}
           {:type :cell/text, :content description}
           {:type :cell/text, :content owner}
           {:type :cell/text, :content version}]})

(defn welcome-projects-table
  [welcome-projects]
  (table/build
    {:headers [nil :name :description :owner :version]
     :rows (map welcome-project-row welcome-projects)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- user-row
  [{:keys [username first-name last-name organization state last-online online?] :as user}]
  {:style (when online? :success)
   :cells [{:type :cell/icon,       :content icons/user}
           {:type :cell/username,   :content username}
           {:type :cell/text,       :content first-name}
           {:type :cell/text,       :content last-name}
           {:type :cell/text,       :content organization}
           {:type :cell/text,       :content state}
           {:type :cell/timestamp,  :content last-online}
           {:type :cell/link,  :content (when (current-user/not-is? username) {:href (u/user-uri username :edit true :action "delete"), :text (t :action.delete)})}]})

(defn users-table
  [users]
  (table/build
    {:headers [nil :username :first-name :last-name :organization :state :last-online :action]
     :rows (map user-row users)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- doc-row
  [{:keys [title basename] :as doc}]
  {:style nil
   :cells [{:type :cell/icon, :content icons/documentation}
           {:type :cell/text, :content title}
           {:type :cell/external-link, :content {:text (t :documentation.open-html)
                                                 :href (str "/html/" basename ".html")}}
           {:type :cell/external-link, :content {:text (t :documentation.open-pdf)
                                                 :href (str "/pdf/" basename ".pdf")}}
           {:type :cell/external-link, :content {:text (t :documentation.open-epub)
                                                 :href (str "/epub/" basename ".epub")}}]})

(defn docs-table
  [docs]
  (table/build
    {:headers [nil :name :html :pdf :epub]
     :rows (map doc-row docs)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- version-row
  [icon {:keys [version uri commit] :as version}]
  {:style nil
   :cells [{:type :cell/icon,       :content icon}
           {:type :cell/link,       :content {:text version :href uri}}
           {:type :cell/text,       :content (:comment commit)}
           {:type :cell/text,       :content (:author commit)}
           {:type :cell/timestamp,  :content (:date commit)}]})

(defn versions-table
  [icon versions]
  (table/build
    {:headers [nil :version :comment :author :date]
     :rows (map (partial version-row icon) versions)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- cell-type?
  [x]
  (and
    (keyword? x)
    (-> x namespace (= "cell"))))

(defn- cell-type-for
  [{:keys [type name] :as parameter}]
  (cond
    (cell-type? type) type
    (re-matches #".*:url\..*" name) :cell/url
    :else (case type
            "String"            :cell/text
            "RestrictedString"  :cell/text
            "Text"              :cell/textarea
            "RestrictedText"    :cell/textarea
            "Password"          :cell/password
            "Enum"              :cell/enum
            "Boolean"           :cell/boolean)))

(defn- ids-for-map-keys
  "For cell where the value is a map itself, we apply the id-format-fn to each key,
  and attach this as metadata of the value itself."
  [m id-format-fn]
  (into {:class (when (fn? id-format-fn) (id-format-fn ""))}
        (for [[k _] m :let [k-name (name k)]]
          [k (if (fn? id-format-fn)
               (id-format-fn k-name)
               k-name)])))

(defn- value-of
  [{:keys [name value id-format-fn built-from-map? read-only? required?] :as parameter} cell-type row-index]
  (let [formatted-name (if (fn? id-format-fn)
                         (id-format-fn name)
                         (format "parameter-%s--%s--value" name row-index))
        value-base (cond-> {:id formatted-name, :row-index row-index, :read-only? read-only?, :placeholder (when required? (t :required-parameter.placeholder))}
                           (not built-from-map?) (assoc :parameter parameter))]
    (case cell-type
      ; TODO: Using the same key for all cell
      ;       types (e.g. :value) would simplify this code
      :cell/textarea  (assoc value-base :text      value)
      :cell/text      (assoc value-base :text      value)
      :cell/timestamp (assoc value-base :timestamp value)
      :cell/set       (assoc value-base :set       value)
      :cell/email     (assoc value-base :email     value)
      :cell/enum      (assoc value-base :enum      value)
      :cell/url       (assoc value-base :url       value)
      :cell/boolean   (assoc value-base :value     value)
      :cell/password  (assoc value-base :password  value)
      :cell/map       (with-meta value {:ids (ids-for-map-keys value id-format-fn)})
      value)))

(defn- parameter-row
  [first-cols-keywords
   row-index
   {:keys [type editable? hidden? help-hint]
    :or {editable? (page-type/edit-or-new?)}
    :as parameter}]
  (let [cell-type (cell-type-for parameter)
        first-cells (mapv #(do {:type :cell/text, :content (get parameter %)}) first-cols-keywords)]
    {:style nil
     :hidden? hidden?
     :cells (conj first-cells
                  {:type cell-type, :content (value-of parameter cell-type row-index), :editable? editable?}
                  {:type :cell/help-hint, :content help-hint})}))

(defn build-parameters-table
  "The last columns are always value and hint. The first ones might change."
  [first-cols-keywords parameters]
  (table/build
    {:headers (concat first-cols-keywords [:value nil])
     :rows (map-indexed (partial parameter-row first-cols-keywords) parameters)}))

(defn parameters-table
  [parameters]
  (build-parameters-table [:description] parameters))

(defn runtime-parameters-table
  [parameters]
  (build-parameters-table [:name] parameters))

(defn service-catalog-parameters-table
  [parameters]
  (build-parameters-table [:name :description :category] parameters))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn user-summary-table
  [{:keys [username] :as user-summary-map}]
  (parameters-table
    (let [require-old-password? (and (page-type/edit?) (current-user/is? username))]
      (p/map->parameter-list user-summary-map
        :username       {:type :cell/text, :editable? (page-type/new?), :id-format-fn (constantly "name"), :required? true}
        :first-name     {:type :cell/text}
        :last-name      {:type :cell/text}
        :organization   {:type :cell/text}
        :email          {:type :cell/email, :required? true}
        :super?         {:type :cell/boolean,   :editable? (and (page-type/edit-or-new?) (current-user/super?)), :id-format-fn (constantly "issuper")}
        :creation       {:type :cell/timestamp, :editable? false}
        :password-new-1 {:type :cell/password,  :editable? true,  :hidden? (not (page-type/edit-or-new?)),  :id-format-fn (constantly "password1")}
        :password-new-2 {:type :cell/password,  :editable? true,  :hidden? (not (page-type/edit-or-new?)),  :id-format-fn (constantly "password2")}
        :password-old   {:type :cell/password,  :editable? true,  :hidden? (not require-old-password?),     :id-format-fn (constantly "oldPassword")}
        :state          {:type :cell/text,      :editable? false, :hidden? (page-type/edit-or-new?)}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn module-summary-table
  [module]
  (parameters-table
    (p/map->parameter-list module
      :name           {:type :cell/text,       :editable? (page-type/new?), :id-format-fn (constantly "ss-module-name"), :required? true}
      :uri            {:type :cell/module-version, :as-parameter :module-version, :editable? false, :hidden? (page-type/new?)}
      :description    {:type :cell/text}
      :comment        {:type :cell/text,       :hidden?  (page-type/edit-or-new?)}
      :category       {:type :cell/text,       :editable? false, :hidden? (page-type/new?)}
      :creation       {:type :cell/timestamp,  :editable? false, :hidden? (page-type/new?)}
      :publication    {:type :cell/timestamp,  :editable? false, :hidden? (->  module :publication not-empty not), :id-format-fn (constantly "ss-publication-date")}
      :last-modified  {:type :cell/timestamp,  :editable? false, :hidden? (page-type/new?)}
      :owner          {:type :cell/username,   :editable? false, :hidden? (page-type/new?)}
      :image          {:type :cell/text,       :hidden? (or (page-type/view?) (-> module :category (not= "Image"))), :id-format-fn (constantly "logoLink")})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- access-right-row-style
  [{:keys [owner-access? group-access? public-access?]}]
  (if-not owner-access?
    :danger
    (case [group-access? public-access?]
          [false         false]          :warning
          [true          true]           :success
          nil)))

(def ^:private access-rights-id-suffix
  {:get               "Get"
   :put               "Put"
   :delete            "Delete"
   :create-children   "CreateChildren"
   :post              "Post"})

(defn- access-right-row
  [access-rights [access-right-name access-rights-key]]
  (let [{:keys [owner-access? group-access? public-access?] :as access-rights} (get access-rights access-rights-key)
        id-suffix (access-rights-id-suffix access-rights-key)]
    {:style (when (page-type/view-or-chooser?) (access-right-row-style access-rights))
     :cells [{:type :cell/text,     :content access-right-name}
             {:type :cell/boolean,  :content {:value owner-access?,   :id (str "owner"  id-suffix)},  :editable? (page-type/edit-or-new?)}
             {:type :cell/boolean,  :content {:value group-access?,   :id (str "group"  id-suffix)},  :editable? (page-type/edit-or-new?)}
             {:type :cell/boolean,  :content {:value public-access?,  :id (str "public" id-suffix)},  :editable? (page-type/edit-or-new?)}]}))

(defn- access-rights-rows
  [module-type]
  (vector
    [(t :access.view)   :get]
    [(t :access.edit)   :put]
    [(t :access.delete) :delete]
    (case module-type
      :project    [(t :access.create-children)    :create-children]
      :image      [(t :access.build-or-run-image) :post]
      :deployment [(t :access.run-deployment)     :post])))

(defn access-rights-table
  [module]
  (let [module-type   (-> module :summary :category uc/keywordize)
        access-rights (-> module :authorization :access-rights)]
    (table/build
      {:headers [:access-right :owner :group :public]
       :rows (->> module-type
                  access-rights-rows
                  (map (partial access-right-row access-rights)))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn group-members-table
  [group-members]
  (parameters-table
    (p/map->parameter-list group-members
      :inherited-group-members? {:type :cell/boolean, :id-format-fn (constantly "inheritedGroupMembers")}
      :group-members            {:type :cell/set,     :id-format-fn (constantly "groupmembers")})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cloud-image-details-table
  [cloud-image-details]
  (parameters-table
    (p/map->parameter-list cloud-image-details
      :native-image?      {:type :cell/boolean, :id-format-fn (constantly "isbase")}
      :cloud-identifiers  {:type :cell/map,     :id-format-fn (partial format "cloudimageid_imageid_%s")}
      :reference-image    {:type :cell/reference-module})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn os-details-table
  [os-details]
  (parameters-table
    (p/map->parameter-list os-details
      :platform         {:type :cell/enum, :id-format-fn (constantly "platform")}
      :login-username   {:type :cell/text, :id-format-fn (constantly "loginUser")})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- run-row
  [{:keys [cloud-name uri module-uri start-time username uuid status tags type] :as run}]
  {:style nil
   :cells [{:type :cell/icon,      :content (icons/icon-for (or type :run))}
           {:type :cell/link,      :content {:text (uc/trim-from uuid \-), :href uri}}
           {:type :cell/url,       :content module-uri}
           {:type :cell/text,      :content status}
           {:type :cell/timestamp, :content start-time}
           {:type :cell/username,  :content username}
           {:type :cell/text,      :content tags}]})

(defn runs-table
  [docs]
  (table/build
    {:headers [nil :id :module :status :start-time :user :tags]
     :rows (map run-row docs)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(localization/with-prefixed-t :deployment-parameters-table

  (defn- deployment-parameter-cell-placeholder
    [field]
    (->> field name (format "blank-parameter.%s.placeholder") t))

  (defn- deployment-parameter-cell-id
    [row-index field]
    (format "parameter--entry--100%s--%s"
            row-index
            (name field)))

  (defmulti deployment-parameter-cell (comp last vector))

  (defmethod deployment-parameter-cell :type
    [{:keys [disabled? placeholder category] :as param} row-index field]
    {:type      :cell/hidden-input
     :content {:id    (when-not disabled? (deployment-parameter-cell-id row-index field))
               :value "String"}})

  (defmethod deployment-parameter-cell :category
    [{:keys [disabled? placeholder category] :as param} row-index field]
    {:type      :cell/enum
     :editable? (page-type/edit-or-new?)
     :content {:disabled? disabled?
               :id (when-not disabled? (deployment-parameter-cell-id row-index field))
               :enum (u/enum ["Output" "Input"] :deployment-parameter-category category)}})

  (defmethod deployment-parameter-cell :default
    [{:keys [disabled? placeholder category] :as param} row-index field]
    {:type :cell/text
     :editable? (page-type/edit-or-new?)
     :content {:disabled? disabled?
               :id (when-not disabled? (deployment-parameter-cell-id row-index field))
               :placeholder (or
                              placeholder
                              (deployment-parameter-cell-placeholder field))
               :text (get param field)}})

  (defn- deployment-parameter-remove-button-cell
    [param]
    (when-not (:disabled? param)
      (remove-button-cell)))

  (defn- deployment-parameter-row
    [row-index {:keys [category help-hint] :as param}]
    {:style  (when (page-type/view-or-chooser?)
               (case category
                 "Output" :info
                 "Input"  :warning
                 nil))
     :cells [(deployment-parameter-cell param row-index :name)
             (deployment-parameter-cell param row-index :description)
             (deployment-parameter-cell param row-index :category)
             (deployment-parameter-cell param row-index :value)
             (deployment-parameter-cell param row-index :type)
             (deployment-parameter-remove-button-cell param)
             {:type :cell/help-hint, :content help-hint}]})

  (defn deployment-parameters-table
    [deployment-parameters]
    (table/build
      {:class "ss-table-with-blank-last-row"
       :headers [:name :description :category :value nil nil]
       :rows (->> deployment-parameters
                  append-blank-row-in-edit-mode
                  (map-indexed deployment-parameter-row))}))

) ;; End of prefixed t scope

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(localization/with-prefixed-t :image-creation-packages-table

  (defn- image-creation-package-cell-placeholder
    [field]
    (->> field name (format "blank-parameter.%s.placeholder") t))

  (defn- image-creation-package-cell-id
    [row-index field]
    (format "package--%s--%s"
            row-index
            (name field)))

  (defn- image-creation-package-cell
    [row-index package field]
    {:type :cell/text
     :content {:text (get package field)
               :placeholder (image-creation-package-cell-placeholder field)
               :id (image-creation-package-cell-id row-index field)}
     :editable? (page-type/edit-or-new?)})

  (defn- image-creation-package-row
    [row-index package]
    {:style  nil
     :cells [(image-creation-package-cell row-index package :name)
             (image-creation-package-cell row-index package :repository)
             (image-creation-package-cell row-index package :key)
             (remove-button-cell)]})

  (defn image-creation-packages-table
    [image-creation-packages]
    (table/build
      {:class "ss-table-with-blank-last-row"
       :headers [:name :repository :key nil]
       :rows (->> image-creation-packages
                  append-blank-row-in-edit-mode
                  (map-indexed image-creation-package-row))}))

) ;; End of prefixed t scope

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- project-child-row
  [{:keys [category name description owner version uri] :as project-child}]
  {:style  nil
   :cells [{:type :cell/icon,     :content (icons/icon-for category)}
           {:type :cell/link,     :content {:text name, :href uri}}
           {:type :cell/text,     :content description}
           {:type :cell/username, :content owner}
           {:type :cell/text,     :content version}]})

(defn project-children-table
  [project-children]
  (table/build
    {:headers [nil :name :description :owner :version]
     :rows (map project-child-row project-children)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: The deployment nodes tables has grown a bit more complex than other tables.
;;       Consider refactoring.

(localization/with-prefixed-t :deployment-nodes-table

  (defmulti deployment-node-cell-inner-table-mapping-row (comp :target last vector))

  (defn- deployment-node-cell-mapping-value
    [mapping]
    (let [value (-> mapping :value uc/ensure-unquoted)]
      (cond
          (:mapped-value? mapping)  (t :parameter.bound-to-output value)
          (not-empty value)         (t :parameter.bound-to-value value)
          :else                     (t :parameter.bound-to-value.empty))))

  (defmethod deployment-node-cell-inner-table-mapping-row :page-type-category/read-only
    [node-index _ mapping-index mapping]
    {:cells [{:type :cell/text, :content {:class "ss-align-middle-right ss-label-cell"
                                          :text (t :parameter.input (:name mapping))}}
             {:type :cell/text, :content (deployment-node-cell-mapping-value mapping)}]})

  (defn- deployment-node-cell-mapping-options-enum
    [mapping]
    (let [selected (if (:mapped-value? mapping) :parameter.bind-to-output :parameter.bind-to-value)]
      (u/enum [:parameter.bind-to-output :parameter.bind-to-value] :mapping-options selected)))

  (defmethod deployment-node-cell-inner-table-mapping-row :page-type-category/editable
    [node-index _ mapping-index mapping]
    {:class "ss-deployment-node-parameter-mapping-row"
     :cells [{:type :cell/text, :editable? false, :content {:class  "ss-align-middle-right ss-label-cell"
                                                            :text   (t :parameter.input (:name mapping))}}
             {:type :cell/hidden-input,           :content {:value  (:name mapping)
                                                            :id     (format "node--%s--mappingtable--%s--input" node-index mapping-index)}}
             {:type :cell/enum, :editable? true,  :content {:enum   (deployment-node-cell-mapping-options-enum mapping)
                                                            :class  "ss-mapping-options"}}
             {:type :cell/text, :editable? true,  :content {:text   (-> mapping :value uc/ensure-unquoted)
                                                            :class  "ss-mapping-value"
                                                            :id     (format "node--%s--mappingtable--%s--output" node-index mapping-index)
                                                            :placeholder (t :new-parameter-mapping.placeholder.value)}}]})

  (defmethod deployment-node-cell-inner-table-mapping-row :deployment-run-dialog
    [_ node-name _ mapping]
    (when-not (:mapped-value? mapping)
      {:cells [{:type :cell/text, :editable? false, :content {:text   (t :parameter.input (:name mapping))}}
               {:type :cell/text, :editable? true,  :content {:text   (-> mapping :value uc/ensure-unquoted)
                                                              :id     (format "parameter--node--%s--%s" node-name (:name mapping))
                                                              :placeholder (t :new-parameter-mapping.placeholder.value)}}]}))

  (defn- deployment-node-cell-inner-table-mappings
    [deployment-node node-index]
    (->> deployment-node
         :mappings
         (map #(assoc % :target (:target deployment-node)))
         (map-indexed (partial deployment-node-cell-inner-table-mapping-row node-index (:name deployment-node)))))

  (defmulti deployment-node-cell-inner-table-first-rows (comp :target first vector))

  (defmethod deployment-node-cell-inner-table-first-rows :page-type/any
    [deployment-node node-index]
    [{:class "ss-deployment-node-reference-image-row"
      :cells [{:type :cell/text,              :content (t :reference-image.label)}
              {:type :cell/reference-module,  :content {:value (:reference-image deployment-node)
                                                        :colspan 2}, :editable? false}
              {:type :cell/hidden-input,      :content {:value (-> deployment-node :reference-image u/module-uri (uc/trim-prefix "/"))
                                                        :id (format "node--%s--imagelink" node-index)}}]}
     {:class "ss-deployment-node-default-multiplicity-row"
      :cells [{:type :cell/text,             :content (t :default-multiplicity.label)}
              {:type :cell/positive-number,  :content {:value (:default-multiplicity deployment-node)
                                                       :min-value 1
                                                       :id (format "node--%s--multiplicity--value" node-index)}, :editable? (page-type/edit-or-new?)}
              {:type :cell/blank} ; Two blank cells to maintain the layout
              {:type :cell/blank}]}
     {:class "ss-deployment-node-default-cloud-row"
      :cells [{:type :cell/text,             :content (t :default-cloud.label)}
              {:type :cell/enum,             :content {:enum (:default-cloud deployment-node)
                                                       :id (format "node--%s--cloudservice--value" node-index)}, :editable? (page-type/edit-or-new?)}]}])

  (defmethod deployment-node-cell-inner-table-first-rows :deployment-run-dialog
    [deployment-node _]
    [{:cells [{:type :cell/text,             :content (t :default-multiplicity.label)}
              {:type :cell/positive-number,  :content {:value (:default-multiplicity deployment-node)
                                                       :min-value 1
                                                       :id (format "parameter--node--%s--multiplicity" (:name deployment-node))}, :editable? true}]}
     {:cells [{:type :cell/text,             :content (t :default-cloud.label)}
              {:type :cell/enum,             :content {:enum (:default-cloud deployment-node)
                                                       :id (format "parameter--node--%s--cloudservice" (:name deployment-node))}, :editable? true}]}])

  (defn- deployment-node-cell-inner-table-mapping-header
    [deployment-node node-index]
    [{:class "ss-deployment-node-separator-row"
      :cells [{:type :cell/blank}]}
     {:class "ss-deployment-node-parameter-mappings-label-row"
      :cells [{:type :cell/text,             :content (t :parameter-mappings.label)}]}])

  (defn- deployment-node-cell-inner-table
    [node-index deployment-node]
    (let [mappgings? (-> deployment-node :mappings not-empty)
          mappgings-header? (and mappgings? (-> deployment-node :target (isa? :page-type/any)))]
      {:rows (cond-> deployment-node
          :always           (deployment-node-cell-inner-table-first-rows node-index)
          mappgings-header? (into (deployment-node-cell-inner-table-mapping-header deployment-node node-index))
          mappgings?        (into (deployment-node-cell-inner-table-mappings       deployment-node node-index)))}))

  (defmulti deployment-node-row page-type/current)

  (defmethod deployment-node-row :page-type-category/read-only
    [node-index {:keys [name reference-image] :as deployment-node}]
    {:style  nil
     :class (str "ss-deployment-node-row")
     :cells [{:type :cell/icon, :content icons/image}
             {:type :cell/text, :content {:text name, :class "ss-node-shortname"}}
             {:type :cell/inner-table, :content (deployment-node-cell-inner-table node-index deployment-node)}]})

  (defmethod deployment-node-row :page-type-category/editable
    [node-index {:keys [name reference-image template-node?] :as deployment-node}]
    {:style  nil
     :class (str "ss-deployment-node-row" (when template-node? " ss-deployment-template-row"))
     :data  (when name (assoc-in {} ["outputParams" name] (:output-parameters deployment-node)))
     :cells [{:type :cell/text, :editable? true, :content {:text name
                                                           :class "ss-node-shortname"
                                                           :error-help-hint (t :node-name-unique.error-help-hint)
                                                           :id (format "node--%s--shortname" node-index)
                                                           :placeholder (t (if template-node? :template-node.name.placeholder :node.name.placeholder))}}
            {:type :cell/multi, :visible-cell-index (if template-node? 1 0), :content [
              {:type :cell/inner-table,   :content (deployment-node-cell-inner-table node-index deployment-node)}
              {:type :cell/action-button, :content {:text (t :button-label.choose-reference), :class "ss-choose-node-reference-image-btn"}}]}
            (remove-button-cell :item-name (-> :node t s/lower-case))]})

  (defn deployment-nodes-table
    [deployment-nodes]
    (table/build
      {:class "ss-table-with-blank-last-row"
       :headers (if (page-type/view-or-chooser?)
                  [nil :name :default-configuration nil]  ;; Add a 1st column for the icon in view mode.
                  [:name :default-configuration nil])
       :rows (->> deployment-nodes
                  (map #(assoc % :target (page-type/current)))
                  ; (uc/map-in [:mappings] #(assoc % :target (page-type/current)))
                  (map-indexed deployment-node-row))}))


  ;; Similar table for the 'Run Deployment' dialog

  (defn run-deployment-node-parameters-table
    [deployment-nodes]
    (table/build
      {:rows (->> deployment-nodes
                  (map #(assoc % :target :deployment-run-dialog))
                  ; (uc/map-in [:mappings] #(assoc % :target :deployment-run-dialog))
                  (map-indexed deployment-node-row))}))

) ;; End of prefixed t scope

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn run-summary-table
  [run]
  (parameters-table
    (p/map->parameter-list run
      :module-uri         {:type :cell/url,       :editable? false}
      :category           {:type :cell/text,      :editable? false}
      :user               {:type :cell/username,  :editable? false}
      :start-time         {:type :cell/timestamp, :editable? false}
      :end-time           {:type :cell/timestamp, :editable? false}
      :last-state-change  {:type :cell/timestamp, :editable? false}
      :state              {:type :cell/text,      :editable? false}
      :type               {:type :cell/text,      :editable? false, :as-parameter :run-type}
      :uuid               {:type :cell/text,      :editable? false, :as-parameter :run-id}
      :tags               {:type :cell/text,      :editable? true})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- vm-row
  [{:keys [cloud-name run-uuid cloud-instance-id username state] :as vm}]
  {:style  nil
   :cells [{:type :cell/link,     :content {:text (uc/trim-from run-uuid \-), :href (str "/run/" run-uuid)}}
           {:type :cell/text,     :content (localization/with-prefixed-t :run.state
                                             (-> (or state :unknown) uc/keywordize t))}
           {:type :cell/username, :content username}
           {:type :cell/text,     :content cloud-instance-id}]})

(defn vms-table
  [vms]
  (table/build
    {:headers [:slipstream-id :state :user :cloud-instance-id]
     :rows (map vm-row vms)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
