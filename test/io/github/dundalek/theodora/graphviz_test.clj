(ns io.github.dundalek.theodora.graphviz-test
  (:require
   [babashka.fs :as fs]
   [clojure.java.shell :refer [sh]]
   [clojure.test :refer [deftest is testing]]
   [dorothy.core :as dc]
   [io.github.dundalek.theodora.parser :as parser]
   [io.github.dundalek.theodora.test-utils])
  (:import
   (java.util.concurrent TimeUnit)
   (clj_antlr ParseError)))

(defn svg [s]
  (:out (sh  "dot" "-Tsvg" :in s)))

(defmacro with-timeout [& body]
  `(.get (future ~@body) 5 TimeUnit/SECONDS))

(def syntax-error-files
  #{"1845.dot" ; contains two toplevel `digraphs`, graphviz renders only the first one and ignores the second one, we get parser error
    "1676.dot" ; seems like invalid utf8 encoding, antlr does not like it: token recognition error
    "1411.dot" ; file has syntax error
    "1308_1.dot"}) ; file has syntax error

(def ignored-files
  #{"1332.dot" ; looks visually same
    "1879.dot" ; looks visually same

    "2371.dot" ; seems to hang
    "2471.dot" ; pretty large graph, not sure what is wrong
    "2239.dot" ; pretty large graph, not sure what is wrong, could it be nested subgraphs?

    ; \N escape sequence
    "2193.dot"
    "Heawood.gv"
    "Petersen.gv"
    "dd.gv"
    "viewport.gv"
    ;; https://graphviz.org/docs/attr-types/escString/
    ;; there are also \G \E \T \H \L
    ;; and \l \r
    "url.gv"

    "2516.dot" ; invalid HTML in attribute

    ;; https://github.com/daveray/dorothy/issues/18
    "2563.dot" ; node and edge need to be escaped: `node [class=node]` -> `node [class="node"]` (dorothy issue)
    "grdlinear.gv"}) ; likely need to escape `graph [label=Graph];`

(def test-files
  (->> (concat
        (fs/glob "." "tmp/graphviz/tests/*.dot"))
        ; (fs/glob "." "tmp/graphviz/tests/graphs/*.gv"))
       (remove (comp (into ignored-files syntax-error-files) fs/file-name))
       (sort-by fs/file-name)))

(deftest syntax-errors
  (doseq [file syntax-error-files]
    (let [input (slurp (fs/file "tmp/graphviz/tests" file))]
      (testing (str file)
        (is (thrown? ParseError (dc/dot (parser/parse input))))))))

(deftest files
  (doseq [file test-files]
    (let [input (slurp (fs/file file))]
      (testing (str file)
        (is (= (with-timeout (svg input))
               (with-timeout (svg (dc/dot (parser/parse input))))))))))

(comment
  (def file (fs/file "tmp/graphviz/tests/1845.dot"))

  (spit "tmp/simplex.dot" (dc/dot (parser/parse (slurp file))))
  (= (slurp file)
     (dc/dot (parser/parse (slurp file))))

  (def label (-> (parser/parse (slurp file))
                 :statements
                 first
                 :attrs
                 (get "label")))

  (doseq [file test-files]
    (try
      (println "Processing" (fs/file-name file))
      (let [input (slurp (fs/file file))
            filename (str (fs/file-name file) ".svg")
            out (dc/dot (parser/parse input))]
        (spit (str "tmp/dot/expected/" (fs/file-name file)) input)
        (spit (str "tmp/dot/actual/" (fs/file-name file)) out)
        (with-timeout
          (spit (str "tmp/expected/" filename) (svg input)))
        (with-timeout
          (spit (str "tmp/actual/" filename) (svg out))))
      (catch Exception _e
        (println "Failed on" file)))))
