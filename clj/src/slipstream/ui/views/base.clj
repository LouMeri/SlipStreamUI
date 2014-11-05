(ns slipstream.ui.views.base
  (:require [net.cgrand.enlive-html :as html :refer [deftemplate defsnippet]]
            [slipstream.ui.util.core :as u]
            [slipstream.ui.util.dev :as ud]
            [slipstream.ui.util.enlive :as ue]
            [slipstream.ui.util.clojure :as uc :refer [defn-memo]]
            [slipstream.ui.util.page-type :as page-type]
            [slipstream.ui.models.version :as version]
            [slipstream.ui.models.user.loggedin :as user-loggedin]
            [slipstream.ui.views.messages :as messages]
            [slipstream.ui.views.header :as header]
            [slipstream.ui.views.alerts :as alerts]
            [slipstream.ui.views.section :as section]
            [slipstream.ui.views.subsection :as subsection]
            [slipstream.ui.views.menubar :as menubar]
            [slipstream.ui.views.table :as table]
            [slipstream.ui.views.breadcrumbs :as breadcrumbs]
            [slipstream.ui.views.secondary-menu :as secondary-menu]
            [slipstream.ui.views.secondary-menu-actions :as action]
            [slipstream.ui.views.code-area :as code-area]
            [slipstream.ui.views.modal-dialogs :as modal-dialogs]))

(def base-template-filename (u/template-path-for "base.html"))

(def head-sel [:head])
(def page-title-sel (concat head-sel [:> :title]))
(def base-sel (concat head-sel [:> :base]))

(def css-container-sel head-sel)
(def css-sel (concat css-container-sel [:> :link]))
(def bottom-scripts-container-sel [:#bottom-scripts])
(def bottom-scripts-sel (concat bottom-scripts-container-sel [:> :script]))

(def alert-container-sel [:#alert-container])

(def topbar-sel [:#topbar])
(def menubar-sel [:#menubar])
(def header-sel [:#header])
(def secondary-menubar-sel [:#ss-secondary-menubar-container])
(def breadcrumbs-sel [:#ss-breadcrumb-container :> :ol])
(def secondary-menu-sel [:#ss-secondary-menu])
(def content-sel [:#ss-content])
(def footer-sel [:#footer])
(def modal-dialogs-placeholder-sel [:#ss-modal-dialogs-placeholder])

(def error-page-cls "ss-error-page")
(def dev-mode-page-cls "ss-dev-mode-page")
(def beta-page-cls "ss-beta-page")
(def placeholder-page-cls "ss-placeholder-page")
(def in-progress-page-cls "ss-in-progress-page")

(def edit-page-actions
  [action/save
   action/cancel
   action/delete])

(def new-page-actions
  [action/create
   action/cancel])

(defn-memo ^:private node-from-template
  [template-filename sel]
  ((html/snippet template-filename sel [] identity)))

(defn-memo ^:private node-from-base-template
  [sel]
  (set (node-from-template base-template-filename sel)))

(defn nodes-from-templates
  [sel template-filenames]
  (mapcat #(node-from-template % sel) template-filenames))

(defn additional-html
  [sel template-filenames]
  (->> template-filenames
       (remove nil?)
       (nodes-from-templates sel)
       distinct
       (remove (node-from-base-template sel))))

(ue/def-blank-snippet ^:private save-form-snip :form
  [content-transformation-fn]
  ue/this (if (page-type/edit?)
            (ue/set-id "save-form")
            (ue/set-id "create-form"))
  ue/this (html/set-attr :accept-charset "utf-8")
  ; ue/this (html/set-attr :method "post")
  ; ue/this (html/set-attr :action "?method=put")
  ue/this content-transformation-fn)

(defn- transform-content
  [content]
  (let [transformation-fn (ue/if-enlive-node content
                            (html/substitute content)
                            (section/build content))]
    (if (page-type/edit-or-new?)
      (html/content (save-form-snip transformation-fn))
      transformation-fn)))

(deftemplate base base-template-filename
  [{:keys [error-page?
           beta-page?
           placeholder-page?
           in-progress-page?
           page-title
           header
           resource-uri
           secondary-menu-actions
           content
           page-type
           alerts
           involved-templates]
    :as context}]
  [:body]               (ue/enable-class error-page? error-page-cls)
  [:body]               (ue/enable-class ud/*dev?* dev-mode-page-cls)
  [:body]               (ue/enable-class placeholder-page? placeholder-page-cls)
  [:body]               (ue/enable-class beta-page? beta-page-cls)
  [:body]               (ue/enable-class in-progress-page? in-progress-page-cls)
  [:body]               (html/add-class (str "ss-page-type-" (name page-type/*current-page-type*)))
  page-title-sel        (html/content (u/page-title (or page-title (:title header))))
  ; base-sel              (ue/when-set-href (not *dev?*) "/") ;; TODO: Is that needed eventually??!
  base-sel              (ue/set-href "/") ;; TODO: Is that needed eventually??!
  menubar-sel           (html/substitute (menubar/menubar))
  topbar-sel            (ue/remove-if (page-type/chooser?))
  breadcrumbs-sel       (breadcrumbs/transform context)
  secondary-menu-sel    (when-not (page-type/chooser?)
                          (secondary-menu/transform secondary-menu-actions))
  secondary-menubar-sel (ue/remove-if-not (or resource-uri secondary-menu-actions))
  [:#release-version]   (html/content @version/slipstream-release-version)
  footer-sel            (ue/remove-if (page-type/chooser?))
  css-container-sel     (html/append (additional-html css-sel involved-templates))
  header-sel            (when-not (page-type/chooser?)
                          (ue/if-enlive-node header
                            (html/substitute header)
                            (header/transform header)))
  content-sel           (transform-content content)
  alert-container-sel   (html/content (map alerts/alert alerts))
  alert-container-sel   (html/append (alerts/hidden-templates))
  bottom-scripts-container-sel  (html/append (additional-html bottom-scripts-sel involved-templates))
  modal-dialogs-placeholder-sel (html/content (modal-dialogs/all))
  [[:a (html/but (html/attr-starts :href "#"))]]  (if (page-type/chooser?) ;; TODO: Not do it when generating reports page (which currently uses still the :chooser page-type)
                                                    (ue/append-to-href "?chooser=true")
                                                    identity))

(defn generate
  [{:keys [header metadata template-filename content alerts] :as context}]
  (let [involved-templates [alerts/template-filename
                            menubar/template-filename
                            section/template-filename ;; TODO: only if sections in body.
                            subsection/template-filename ;; TODO: only if subsections in body.
                            table/template-filename ;; TODO: only if tables in body.
                            code-area/template-filename ;; TODO: only if code-areas in body.
                            modal-dialogs/template-filename ;; TODO: only if modal-dialogs in body.
                            template-filename
                            ]]
    (println "Generating base for" template-filename " - Title" (:title header))
    ; (println "   user:" user)
    ; (println "   content type:" (type (first content)))
    (base
      (cond-> context
        (page-type/edit?)     (assoc :secondary-menu-actions edit-page-actions)
        (page-type/new?)      (assoc :secondary-menu-actions new-page-actions)
        :always               (assoc
                                :involved-templates involved-templates)))))