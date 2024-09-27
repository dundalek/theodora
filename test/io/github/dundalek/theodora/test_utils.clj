(ns io.github.dundalek.theodora.test-utils
  (:require
   [clojure.string :as str]
   [dorothy.core :as dc]))

;; Hacky monkey-patching, try to contribute upstream to Dorothy
(alter-var-root #'dc/html-pattern (constantly #"(?s)^\s*<([a-zA-Z1-9_-]+)(\s|>).*</\1>\s*$"))
(alter-var-root #'dc/escape-quotes
                (constantly
                 (fn [s]
                   (-> s
                       (str/replace "\n" "\\n")
                       (str/replace "\"" "\\\"")))))

(defn normalize-whitespace [s]
  (-> s str/trim (str/replace #"\s+" " ")))
