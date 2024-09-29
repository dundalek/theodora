(ns io.github.dundalek.theodora.report
  (:require
   [clojure.test :as test]
   [kaocha.hierarchy :as hierarchy]
   [kaocha.report :as report]))

(defn edots* [m]
  (if (or (hierarchy/fail-type? m)
          (hierarchy/error-type? m))
    (do
      (report/doc m)
      (test/with-test-out (println)))
    (report/dots* m)))

(def ^{:doc "Extended Dots:
Works like `dots` reporter but prints name for errored and failed tests like `documentation` reporter."}
  edots
  [edots* report/result])
