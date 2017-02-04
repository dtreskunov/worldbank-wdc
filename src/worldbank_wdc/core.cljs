(ns worldbank-wdc.core
  (:require-macros [cljs.core.async.macros :as async])
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]
            [cljs.core.async :as async]
            [worldbank-wdc.wdc :as wdc]))

(enable-console-print!)

(defn get-schema []
  [{::wdc/id      "earthquakeFeed"
    ::wdc/alias   "Earthquakes with magnitude greater than 4.5 in the last seven days"
    ::wdc/columns [{::wdc/id       "id"
                    ::wdc/dataType "string"}
                   {::wdc/id       "mag"
                    ::wdc/alias    "magnitude"
                    ::wdc/dataType "float"}
                   {::wdc/id       "title"
                    ::wdc/dataType "string"}
                   {::wdc/id       "lat"
                    ::wdc/dataType "float"}
                   {::wdc/id       "lon"
                    ::wdc/dataType "float"}]}])

(defn get-rows-chan [table-info]
  (let [rows-chan (async/chan 1)
        response-chan (http/get "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.geojson"
                                {:keywordize-keys?  false   ; only works for http/jsonp -- https://github.com/r0man/cljs-http/issues/79
                                 :with-credentials? false})
        response->rows (fn [response]
                         (->> response
                              :body
                              :features
                              (map (fn [feat]
                                     {:id    (feat :id)
                                      :mag   (get-in feat [:properties :mag])
                                      :title (get-in feat [:properties :title])
                                      :lon   (first (get-in feat [:geometry :coordinates]))
                                      :lat   (second (get-in feat [:geometry :coordinates]))}))))]
    (async/go
      (->> (async/<! response-chan)
           (response->rows)
           (async/>! rows-chan)))
    rows-chan))

(wdc/register! get-schema get-rows-chan "Earthquake Feed")

(defn submit! []
  (wdc/submit!))

(defn root-comp []
  [:div.container.container-table
   [:div.row.vertical-center-row
    [:div.text-center.col-md-4.col-md-offset-4
     [:input.btn.btn-success {:type "button" :value "Get Data!" :on-click submit!}]]]])


(reagent/render [root-comp] (. js/document (getElementById "root")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
