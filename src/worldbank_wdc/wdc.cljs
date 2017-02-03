(ns worldbank-wdc.wdc)

(defn make-connector [get-schema get-data]
  (doto (.makeConnector js/tableau)
    (aset "getSchema" get-schema)
    (aset "getData" get-data)))

(defn register-connector! [connector]
  (println "Calling tableau.registerConnector")
  (.registerConnector js/tableau connector))

(defn set-connection-name! [s]
  (aset js/tableau "connectionName" s))

(defn submit! []
  (.submit js/tableau))
