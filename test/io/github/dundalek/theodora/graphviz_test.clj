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

(def syntax-error-filenames
  #{"1676.dot" ; seems like invalid utf8 encoding, antlr does not like it: token recognition error
    "1411.dot" ; file has syntax error
    "1308_1.dot" ; file has syntax error

    "russian.gv" ; would it need to update ranges in grammar?

    ;; contains two toplevel `digraphs`, graphviz renders only the first one and ignores the second one, we get parser error
    "1845.dot"
    "multi.gv"})

(def ignored-filenames
  #{"1332.dot" ; looks visually same
    "1879.dot" ; looks visually same

    ;; seems to hang/timeout
    "2371.dot"
    "b100.gv"
    "b103.gv"
    "b104.gv"

    "2471.dot" ; pretty large graph, not sure what is wrong
    "2239.dot" ; pretty large graph, not sure what is wrong, could it be nested subgraphs?

    "2516.dot" ; invalid HTML in attribute

    "html2.gv" ; problem with html label, does not get detected as html `label=<long line 1<BR/>line 2<BR ALIGN="LEFT"/>line 3<BR ALIGN="RIGHT"/>>`

    "polypoly.gv" ; numeric ids like`0000`, leading zeroes get stripped results as `0`
    "root.gv"}) ; `label=07` gets changed to `label=7`

(def all-test-files
  (->> (concat
        (fs/glob "." "tmp/graphviz/tests/*.dot")
        (fs/glob "." "tmp/graphviz/tests/graphs/*.gv"))
       (sort-by fs/file-name)))

(def svg-roundtrip-files
  (->> all-test-files
       (remove (comp (into ignored-filenames syntax-error-filenames) fs/file-name))))

(def syntax-error-files
  (->> all-test-files
       (filter (comp syntax-error-filenames fs/file-name))))

(deftest syntax-errors
  (doseq [file syntax-error-files]
    (let [input (slurp (fs/file file))]
      (testing (str file)
        (is (thrown? ParseError (dc/dot (parser/parse input))))))))

(deftest svg-rountrip
  (doseq [file svg-roundtrip-files]
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

  ;; mkdir -p tmp/{dot,svg}/{actual,expected}
  (doseq [file svg-roundtrip-files]
    (try
      (println "Processing" (fs/file-name file))
      (let [input (slurp (fs/file file))
            filename (str (fs/file-name file) ".svg")
            out (dc/dot (parser/parse input))]
        (spit (str "tmp/dot/expected/" (fs/file-name file)) input)
        (spit (str "tmp/dot/actual/" (fs/file-name file)) out)
        (with-timeout
          (spit (str "tmp/svg/expected/" filename) (svg input)))
        (with-timeout
          (spit (str "tmp/svg/actual/" filename) (svg out))))
      (catch Exception _e
        (println "Failed on" file)))))
