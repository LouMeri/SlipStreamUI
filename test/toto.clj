(ns slipstream.ui.toto
  (:require [net.cgrand.enlive-html :as html]
            [slipstream.ui.utils :as utils]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.string :as string])
  (:import (java.util.regex Pattern)))

(def header-sel [:#header])

(def header-template-html "slipstream/ui/views/header.html")

(def header-menu [:.menu_bar])

(html/defsnippet header-top-bar-snip header-template header-menu
  [{username :username issuper :issuper}]
  [:#header-username :> :a] (html/do-> 
                                (html/content username)
                                (html/set-attr :href (str "/users/" username)))
  [:#header-username] (if (= nil username)
                          nil
                          identity)
  [#{:#header-users :#header-config}] (if (= "true" issuper)
                                            identity
                                            nil)
  [:#header-loginout :a] (if (= nil username)
                          (html/do-> 
                                (html/content "login")
                                (html/set-attr :href "/login"))
                          identity))

(def titles-sel [:#titles])

(html/defsnippet header-titles header-template titles-sel
  [{title :title title-sub :title-sub title-desc :title-desc}]
  [:#header-title] (html/content title)
  [:#header-title-sub] (html/content title-sub)
  [:#header-title-desc] (html/content title-desc))

(def breadcrumb-sel [:#breadcrumb])

(defn- breadcrumb-href
  "root-uri, e.g. 'module/' in the case of modules, or 'user/'
   in the case of users"
  [names index root-uri]
  (if (= "" (names index))
    root-uri
    (str 
      root-uri 
      "/" 
      (reduce 
        #(str %1 (if (= "" %1) "" "/") %2) 
        "" 
        (subvec names 0 (inc index))))))
                                 
(html/defsnippet header-breadcrumb header-template breadcrumb-sel
  [{name :name root-uri :root-uri}]
  [[:li (html/nth-of-type 2)]] (html/clone-for 
                                 [i (range (count (string/split name #"/")))] 
                                 [:a]
                                 (let 
                                   [names (string/split name #"/")
                                    href (breadcrumb-href names i root-uri)
                                    short-name (names i)]
                                   (html/do-> 
                                     (html/content (if (= "" short-name)
                                                     root-uri short-name))
                                     (html/set-attr :href href)))))

(def interaction-top-sel [:#interaction_top])

(html/defsnippet header-buttons header-template interaction-top-sel
  [{buttons :buttons}]
  [[:li (html/nth-of-type 1)]] (html/clone-for 
                                 [button buttons] 
                                 [:button] 
                                 (html/content button))
  [[:li html/last-of-type]] nil)

(defn- gen-titles [title sub desc]
  {:title title :title-sub sub :title-desc desc})

(defn- gen-module-titles [module]
  (gen-titles 
    (:name (:attrs module))
    (str "Version: " (:version (:attrs module)))
    (:description (:attrs module))))

(defn- titles [root]
  (case (:tag root)
    :list (gen-titles "Projects" "All projects" "This root project is shared with all SlipStream users")
    (gen-module-titles root)))

(defn- user-attrs [root]
  (-> (html/select root [:user]) first :attrs))

(defn- root-uri [root]
  (if (= :list (:tag root))
    "module"
    (first (string/split 
             (-> root :attrs :resourceuri)
             #"/"))))

(defn- super? [root]
  (= "true" (:issuper (user-attrs root))))
  
(defn- buttons [root edit?]
  (let
    [type (:tag root)
     super? (super? root)
     edit ["Save" "Cancel" "Delete"]]
    (case type
      :list ["New Project" "Import..."]
      :imageModule (if edit?
                     edit
                     ["Build" "Run" "Edit" "Copy..." (when super? "Publish")])
      :deploymentModule (if edit?
                          edit
                          ["Run" "Run..." "Edit" "Copy..." (when super? "Publish")])
      :projectModule (if edit?
                       edit
                       ["Edit" "New Project" "New Machine Image" "New Deployment"]))))

(html/defsnippet header header-template-html header-sel
  [root edit?]
  header-menu (html/substitute
                (header-top-bar-snip
                  (user-attrs root)))
  titles-sel (html/substitute
               (header-titles
                 (titles root)))
  breadcrumb-sel (html/substitute
                   (header-breadcrumb
                     {:name (if (= :list (:tag root))
                              "" 
                              (-> root :attrs :name))
                      :root-uri (root-uri root)}))
  interaction-top-sel (html/substitute
                        (header-buttons
                          {:buttons (buttons root edit?)}))
  )
