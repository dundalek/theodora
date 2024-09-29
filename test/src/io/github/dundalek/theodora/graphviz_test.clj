(ns io.github.dundalek.theodora.graphviz-test
  (:require
   [babashka.fs :as fs]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [dorothy.core :as dc]
   [io.github.dundalek.theodora.test-utils]
   [theodora.core :as core])
  (:import
   (clj_antlr ParseError)
   (java.util.concurrent TimeUnit)))

(defn svg [s]
  (:out (sh  "dot" "-Tsvg" :in s)))

(defmacro with-timeout [& body]
  `(.get (future ~@body) 5 TimeUnit/SECONDS))

(def syntax-error-filenames
  #{;; seems like invalid utf8 encoding (graphviz has fallbacks and -Gcharset=latin1 option)
    ;; antlr does not like it: token recognition error
    "1676.dot"
    "share/Latin1.gv"

    ;; file has syntax error
    "1411.dot"
    "1308_1.dot"
    "share/b545.gv"

    "graphs/russian.gv" ; would it need to update ranges in grammar?

    ;; contains two toplevel `digraphs`, graphviz renders only the first one and ignores the second one, we get parser error
    "1845.dot"
    "graphs/multi.gv"})

(def ignored-filenames
  #{"1332.dot" ; looks visually same
    "1879.dot" ; looks visually same

    ;; seems to hang/timeout
    "2371.dot"
    "graphs/b100.gv"
    "graphs/b103.gv"
    "graphs/b104.gv"

    "2471.dot" ; pretty large graph, not sure what is wrong
    "2239.dot" ; pretty large graph, not sure what is wrong, could it be nested subgraphs?

    "2516.dot" ; invalid HTML in attribute

    "graphs/html2.gv" ; problem with html label, does not get detected as html `label=<long line 1<BR/>line 2<BR ALIGN="LEFT"/>line 3<BR ALIGN="RIGHT"/>>`

    "graphs/polypoly.gv" ; numeric ids like`0000`, leading zeroes get stripped results as `0`
    "share/polypoly.gv"
    "graphs/root.gv"}) ; `label=07` gets changed to `label=7`

(def graphviz-test-dir "tmp/graphviz/tests/")

(defn path-in-test-dir [path]
  (str/replace-first (str path) graphviz-test-dir ""))

(def all-test-files
  (->> (concat
        (fs/glob graphviz-test-dir "*.dot")
        ; (fs/glob graphviz-test-dir "**.gv"))
        (fs/glob graphviz-test-dir "graphs/*.gv")
        (fs/glob graphviz-test-dir "share/*.gv")
        (fs/glob graphviz-test-dir "regression_tests/**.gv"))
       (sort-by path-in-test-dir)))

(def svg-roundtrip-files
  (->> all-test-files
       (remove (comp (into ignored-filenames syntax-error-filenames) path-in-test-dir))))

(def syntax-error-files
  (->> all-test-files
       (filter (comp syntax-error-filenames path-in-test-dir))))

(def transform-roundtrip-files
  (->> all-test-files
       (remove (comp syntax-error-filenames path-in-test-dir))))

(deftest syntax-errors
  (doseq [file syntax-error-files]
    (testing (str file)
      (let [input (slurp (fs/file file))]
        (is (thrown? ParseError (dc/dot (core/parse input))))))))

(deftest svg-rountrip
  (doseq [file svg-roundtrip-files]
    (testing (str file)
      (let [input (slurp (fs/file file))]
        ; (is (= (with-timeout (svg input))
        ;        (with-timeout (svg (dc/dot (parser/parse input))))))
        (is (= (svg input)
               (svg (dc/dot (core/parse input)))))))))

(deftest transform-rountrip
  (doseq [file transform-roundtrip-files]
    (testing (str file)
      (let [input (slurp (fs/file file))
            transformed (dc/dot (core/parse input))]
        (is (= transformed (dc/dot (core/parse transformed))))))))

(comment
  (def file (fs/file "tmp/graphviz/tests/1845.dot"))

  (spit "tmp/simplex.dot" (dc/dot (core/parse (slurp file))))
  (= (slurp file)
     (dc/dot (core/parse (slurp file))))

  (def label (-> (core/parse (slurp file))
                 :statements
                 first
                 :attrs
                 (get "label")))

  ;; mkdir -p tmp/{dot,svg}/{actual,expected}/{share,graphs,regression_tests}
  (doseq [file svg-roundtrip-files]
    (try
      (println "Processing" (path-in-test-dir file))
      (let [input (slurp (fs/file file))
            filename (str (path-in-test-dir file) ".svg")
            out (dc/dot (core/parse input))]
        (spit (str "tmp/dot/expected/" (path-in-test-dir file)) input)
        (spit (str "tmp/dot/actual/" (path-in-test-dir file)) out)
        (with-timeout
          (spit (str "tmp/svg/expected/" filename) (svg input)))
        (with-timeout
          (spit (str "tmp/svg/actual/" filename) (svg out))))
      (catch Exception _e
        (println "Failed on" file)))))
