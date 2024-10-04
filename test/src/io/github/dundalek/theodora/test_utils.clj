(ns io.github.dundalek.theodora.test-utils
  (:require
   [clojure.string :as str]
   [clojure.java.shell :refer [sh]]
   [dorothy.core :as dc]))

;; Hacky monkey-patching, try to contribute upstream to Dorothy
(alter-var-root #'dc/html-pattern (constantly #"(?s)^\s*<([a-zA-Z1-9_-]+)(\s|>).*</\1>\s*$"))

;; https://github.com/daveray/dorothy/issues/18
;; node and edge need to be escaped: `node [class=node]` -> `node [class="node"]` (dorothy issue)
;; also need to escape `graph [label=Graph];`
(def reserved-pattern #"(?i)node|edge|graph")
(defonce safe-id?-orig @#'dc/safe-id?)
(defn safe-id? [s]
  (and
   (nil? (re-matches reserved-pattern s))
   (safe-id?-orig s)))
(alter-var-root #'dc/safe-id? (constantly safe-id?))

(defn normalize-whitespace [s]
  (-> s str/trim (str/replace #"\s+" " ")))

(defn svg [s]
  (:out (sh  "dot" "-Tsvg" :in s)))

(defmacro with-timeout [& body]
  `(.get (future ~@body) 5 TimeUnit/SECONDS))
