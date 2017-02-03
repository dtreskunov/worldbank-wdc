(ns worldbank-wdc.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET json-response-format]]
            [worldbank-wdc.wdc :as wdc]))

(enable-console-print!)

(defn get-schema [wdc-callback]
  (let [cols [{:id "mag"   :alias "magnitude" :dataType (.. js/tableau -dataTypeEnum -float)}
              {:id "title" :alias "title"     :dataType (.. js/tableau -dataTypeEnum -string)}
              {:id "url"   :alias "url"       :dataType (.. js/tableau -dataTypeEnum -float)}
              {:id "lat"   :alias "latitude"  :dataType (.. js/tableau -dataTypeEnum -float) :columnRole "dimension"}
              {:id "lon"   :alias "longitude" :dataType (.. js/tableau -dataTypeEnum -float) :columnRole "dimension"}]
        table-info {:id      "earthquakeFeed"
                    :alias   "Earthquakes with magnitude greater than 4.5 in the last seven days"
                    :columns cols}]
    (wdc-callback (clj->js [table-info]))))

(defn get-data [table wdc-callback]
  (defn on-success [response]
    (let [rows (map (fn [feat]
                      {"id" (feat "id")
                       "mag" (get-in feat ["properties" "mag"])
                       "title" (get-in feat ["properties" "title"])
                       "lon" (first (get-in feat ["geometry" "coordinates"]))
                       "lat" (second (get-in feat ["geometry" "coordinates"]))})
                    (response "features"))]
      (.appendRows table (clj->js rows))
      (wdc-callback)))
  (GET "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.geojson"
       {:response-format :json
        :handler on-success}))

(wdc/register-connector!
  (wdc/make-connector get-schema get-data))

(wdc/set-connection-name! "Earthquake Feed")

(defn submit! []
  (wdc/submit!))

(defn root-comp []
  [:div.container.container-table
   [:div.row.vertical-center-row
    [:div.text-center.col-md-4.col-md-offset-4
     [:input.btn.btn-success {:type "button" :value "Get Earthquake Data!" :on-click submit!}]]]])


(reagent/render [root-comp] (. js/document (getElementById "root")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
