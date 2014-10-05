(ns slipstream.ui.views.module
  (:require [net.cgrand.enlive-html :as html]
            [slipstream.ui.util.localization :as localization]
            [slipstream.ui.util.clojure :as uc]
            [slipstream.ui.views.tables :as t]
            [slipstream.ui.views.module.image :as image]
            [slipstream.ui.views.module.project :as project]
            [slipstream.ui.views.module.deployment :as deployment]
            [slipstream.ui.models.user :as user]
            [slipstream.ui.models.module :as module-model]
            [slipstream.ui.models.modules :as modules-model]
            [slipstream.ui.models.version :as version]
            [slipstream.ui.util.icons :as icons]
            [slipstream.ui.views.base :as base]
            [slipstream.ui.views.common :as common]
            [slipstream.ui.views.module-base :as module-base]
            [slipstream.ui.views.header :as header]
            [slipstream.ui.views.image :as image-legacy]
            [slipstream.ui.views.deployment :as deployment-legacy]
            [slipstream.ui.views.project :as project-legacy]))

(def deployment-view-template-html (common/get-template "deployment-view.html"))
(def deployment-edit-template-html (common/get-template "deployment-edit.html"))
(def deployment-new-template-html (common/get-template "deployment-new.html"))

(defn header
  [module type]
  (if (module-base/ischooser? type)
    nil
    (module-base/header-snip module)))

(defn footer
  [type]
  (if (module-base/ischooser? type)
    nil
    nil))


;; View

(html/defsnippet deployment-view-snip deployment-view-template-html common/content-sel
  [module]
  identity)

(defmulti content-by-category-view
  (fn [module category]
    category))

(defmethod content-by-category-view "Image"
  [module category]
  common/content-sel (image-legacy/view-snip module))

(defmethod content-by-category-view "Deployment"
  [module category]
  common/content-sel (deployment-legacy/view-snip module))

(defmethod content-by-category-view "Project"
  [module category]
  common/content-sel (project-legacy/view-snip module))

;; Edit

(html/defsnippet deployment-edit-snip deployment-edit-template-html common/content-sel
  [module]
  identity)

(defmulti content-by-category-edit
  (fn [module category]
    category))

(defmethod content-by-category-edit "Image"
  [module category]
  common/content-sel (image-legacy/edit-snip module))

(defmethod content-by-category-edit "Deployment"
  [module category]
  common/content-sel (deployment-legacy/edit-snip module))

(defmethod content-by-category-edit "Project"
  [module category]
  common/content-sel (project-legacy/edit-snip module))

;; New

(html/defsnippet deployment-new-snip deployment-new-template-html common/content-sel
  [module]
  identity)

(defmulti content-by-category-new
  (fn [module category]
    category))

(defmethod content-by-category-new "Image"
  [module category]
  common/content-sel (image-legacy/new-snip module))

(defmethod content-by-category-new "Deployment"
  [module category]
  common/content-sel (deployment-legacy/new-snip module))

(defmethod content-by-category-new "Project"
  [module category]
  common/content-sel (project-legacy/new-snip module))

;; Dispatch function

(defmulti content
  "Multi method dispatched on type: view, edit, new, chooser"
  (fn [module type category]
    type))

(defmethod content "view"
  [module type category]
  (content-by-category-view module category))

(defmethod content "edit"
  [module type category]
  (content-by-category-edit module category))

(defmethod content "new"
  [module type category]
  (content-by-category-new module category))

(defmethod content "chooser"
  [module type category]
    (content-by-category-view module category))


;; javascript inclusion

(def js-scripts-default
  ["/external/jit/js/jit.js"
   "/external/jit/js/excanvas.js"
   "/js/sourcecode-editor.js"
   "/external/ace-editor/ace.js"
   "/external/ace-editor/ext-language_tools.js"
   "/external/ace-editor/mode-python.js"
   "/external/ace-editor/mode-sh.js"
   "/external/ace-editor/mode-javascript.js"
   "/external/ace-editor/mode-powershell.js"
   "/external/ace-editor/mode-perl.js"
   "/external/ace-editor/mode-ruby.js"
   "/external/ace-editor/mode-scheme.js"
   "/external/ace-editor/mode-plain_text.js"
   ])

(defmulti js-scripts
  (fn [type category]
    [type category]))

(defn js-scripts-chooser
  []
  (concat js-scripts-default ["/js/module-chooser.js"]))

(defn js-scripts-project-view
  []
  (concat js-scripts-default ["/js/project-view.js"]))

(defn js-scripts-image-view
  []
  (concat js-scripts-default ["/js/image-view.js"]))

(defn js-scripts-deployment-view
  []
  (concat js-scripts-default ["/js/deployment-view.js"]))

(defmethod js-scripts ["view" "Project"]
  [type category]
  (js-scripts-project-view))

(defmethod js-scripts ["edit" "Project"]
  [type category]
  (concat js-scripts-default ["/js/module.js" "/js/module-edit.js" "/js/project-edit.js"]))

(defmethod js-scripts ["new" "Project"]
  [type category]
  (concat js-scripts-default ["/js/module.js" "/js/module-new.js" "/js/project-new.js"]))

(defmethod js-scripts ["chooser" "Project"]
  [type category]
  (js-scripts-chooser))

(defmethod js-scripts ["view" "Image"]
  [type category]
  (js-scripts-image-view))

(defmethod js-scripts ["edit" "Image"]
  [type category]
  (concat js-scripts-default ["/js/module.js" "/js/module-edit.js" "/js/image-edit.js"]))

(defmethod js-scripts ["new" "Image"]
  [type category]
  (concat js-scripts-default ["/js/module.js" "/js/module-new.js" "/js/image-edit.js"]))

(defmethod js-scripts ["chooser" "Image"]
  [type category]
  (js-scripts-chooser))

(defmethod js-scripts ["view" "Deployment"]
  [type category]
  (js-scripts-deployment-view))

(defmethod js-scripts ["edit" "Deployment"]
  [type category]
  (concat js-scripts-default ["/js/module.js" "/js/module-edit.js" "/js/deployment-edit.js"]))

(defmethod js-scripts ["new" "Deployment"]
  [type category]
  (concat js-scripts-default ["/js/module.js" "/js/module-new.js" "/js/deployment-edit.js"]))

(defmethod js-scripts ["chooser" "Deployment"]
  [type category]
  (js-scripts-chooser))


;; Main function

(defn page-legacy [module type]
  (let
    [category (module-model/module-category module)]
    (base/base
      {:js-scripts (js-scripts type category)
       :title (common/title (module-model/module-name module))
       :header (header module type)
       :content (content module type category)
       :footer (footer type)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(localization/def-scoped-t)

(defn- header
  [summary]
  {:icon      (-> summary :category icons/icon-for)
   :title     (t :header.title (:category summary) (:short-name summary))
   :image     (-> summary :image)
   :subtitle  (t :header.subtitle (str (:version summary)
                  (when-let [desc (:description summary)]
                    (str " - " desc))))})

(defn- old-version-alert
  [latest-version?]
  (when-not latest-version?
    {:type :warning
     :msg (t :alert.old-version.msg)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti middle-sections (comp uc/keywordize :category :summary))

(defmethod middle-sections :project
  [module]
  (project/middle-sections module))

(defmethod middle-sections :image
  [module]
  (image/middle-sections module))

(defmethod middle-sections :deployment
  [module]
  (deployment/middle-sections module))

(defn- sections
  [module]
  (let [summary-section {:title (t :section.summary.title)
                         :content (t/module-summary-table (:summary module))}
        auth-section    {:title (t :section.authorizations.title)
                         :content [(t/access-rights-table module)
                                   (t/group-members-table (-> module :authorization))]}]
    (-> [summary-section (middle-sections module) auth-section]
        flatten
        vec)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- actions
  [module]
  (case (-> module :summary :category uc/keywordize)
    :project     project/actions
    :image       image/actions
    :deployment  deployment/actions))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page
  [metadata]
  (localization/with-lang-from-metadata
    (let [module (module-model/parse metadata)
          summary (:summary module)]
      (base/generate
        {:metadata metadata
         :header (header summary)
         :alerts [(-> summary :latest-version? old-version-alert)]
         :resource-uri (:uri summary)
         :secondary-menu-actions (actions module)
         :content (sections module)}))))
