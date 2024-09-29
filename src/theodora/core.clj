(ns theodora.core
  (:require
   [io.github.dundalek.theodora.parser :as parser]))

(defn parse [input]
  (parser/parse input))
