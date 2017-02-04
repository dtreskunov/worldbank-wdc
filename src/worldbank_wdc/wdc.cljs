(ns worldbank-wdc.wdc
  (:require-macros [cljs.core.async.macros :as async])
  (:require [cljs.spec :as s]
            [cljs.core.async :as async]))

(s/check-asserts true)

(s/def ::id string?)
(s/def ::alias string?)
(s/def ::description string?)
(s/def ::incrementColumnId string?)
(s/def ::dataType #{"bool" "date" "datetime" "float" "int" "string"})
(s/def ::aggType #{"avg" "count" "count_dist" "median" "sum"})
(s/def ::columnRole #{"dimension" "measure"})
(s/def ::columnType #{"continuous" "discrete"})
(s/def ::geoRole #{"area_code" "cbsa_msa" "city" "congressional_district" "country_region" "county" "state_province" "zip_code_postcode"})
(s/def ::numberFormat #{"currency" "number" "percentage" "scientific"})
(s/def ::unitsFormat #{"billions_english" "billions_standard" "millions" "thousands"})
(s/def ::columnInfo (s/keys :req [::dataType
                                  ::id]
                            :opt [::aggType
                                  ::alias
                                  ::columnRole
                                  ::columnType
                                  ::description
                                  ::geoRole
                                  ::numberFormat
                                  ::unitsFormat]))
(s/def ::columns (s/coll-of ::columnInfo))
(s/def ::tableInfo (s/keys :req [::columns
                                 ::id]
                           :opt [::alias
                                 ::description
                                 ::incrementColumnId]))
(s/def ::tables (s/coll-of ::tableInfo))

; These work only when called from the emulator or Tableau
(defn in-auth-phase? [] (= "authPhase" (.-phase js/tableau)))
(defn in-gather-data-phase? [] (= "gatherDataPhase" (.-phase js/tableau)))
(defn in-interactive-phase? [] (= "interactivePhase" (.-phase js/tableau)))
(defn- set-connection-name! [s]
  (aset js/tableau "connectionName" s))

(defn- make-connector [get-schema get-data]
  (doto (.makeConnector js/tableau)
    (aset "getSchema" get-schema)
    (aset "getData" get-data)))

(defn- ns-keywordize-keys
  "Recursively transforms all map keys from strings to keywords in the given namespace."
  [ns m]
  (let [f (fn [[k v]] (if (string? k) [(keyword ns k) v] [k v]))]
    (clojure.walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(defn register! [get-schema get-rows-chan connection-name?]
  (let [get-schema* (fn [schema-callback]
                      (let [schema (s/assert ::tables (get-schema))]
                        (schema-callback (clj->js schema))))
        get-data* (fn [table done-callback]
                    (let [table-info (->> (.-tableInfo table)
                                          (js->clj)
                                          (ns-keywordize-keys (namespace ::tableInfo))
                                          (s/assert ::tableInfo))
                          rows-chan (get-rows-chan table-info)]
                      (async/go-loop []
                                     (when-let [rows (async/<! rows-chan)]
                                       (.appendRows table (clj->js rows))
                                       (recur))))
                    (done-callback))
        connector (make-connector get-schema* get-data*)]
    (some-> connection-name? (set-connection-name!))
    (println "Calling tableau.registerConnector")
    (.registerConnector js/tableau connector)))

(defn submit! []
  (.submit js/tableau))
