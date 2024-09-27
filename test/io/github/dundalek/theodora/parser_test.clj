(ns io.github.dundalek.theodora.parser-test
  (:require
   [clojure.test :refer [are deftest is]]
   [io.github.dundalek.theodora.parser :as parser]
   [io.github.dundalek.theodora.test-utils :as utils]
   [dorothy.core :as dc]))

(deftest unescape-string
  (is (= "some_name" (parser/unescape-string "\"some_name\"")))
  (is (= "some\"name" (parser/unescape-string "\"some\\\"name\"")))
  (is (= "some\\foo" (parser/unescape-string "\"some\\foo\"")))
  (is (= "so\"me\\foo" (parser/unescape-string "\"so\\\"me\\foo\"")))
  (is (= "new\\nline" (parser/unescape-string "\"new\\nline\""))))

(deftest parse
  (is (= {:id "some_name" :statements [] :strict? false :type :dorothy.core/graph}
         (parser/parse "graph some_name { }")))

  (is (= {:id nil
          :statements [{:attrs {}
                        :node-ids [{:id "A" :type :dorothy.core/node-id}
                                   {:id "B" :type :dorothy.core/node-id}]
                        :type :dorothy.core/edge}
                       {:attrs {}
                        :node-ids [{:id "B" :type :dorothy.core/node-id}
                                   {:id "C" :type :dorothy.core/node-id}]
                        :type :dorothy.core/edge}]
          :strict? false
          :type :dorothy.core/digraph}
         (parser/parse "digraph { A -> B; B -> C; }"))))

(deftest parser-roundtrip
  (are [input] (= input (-> input parser/parse dc/dot utils/normalize-whitespace))

    ;; blank graph
    "graph { }"

    ;; strict keyword
    "strict digraph { }"

    ;; named graph
    "graph some_name { }"

    ;; node_stmt
    "graph { A; }"

     ;; id_ STRING
    "graph \"some\\\"name\" { }"

    ;; id_ NUMBER
    "graph 42 { }"
    "graph { 42; }"
    "graph { -42; }"
    "graph { 42.69; }"
    "graph { -42.69; }"
    ;; numeric id with leading zeros
    ; "graph { 000; }"
    ; "graph { 001; }"

    ;; id_ HTML_STRING
    "graph { A [label=<<b>bold label</b>>]; }"

    ;; node_stmt with port
    "graph { A:port; }"
    ;; node_stmt with port and compass
    "graph { A:port:sw; }"
    ;; from docs regarding compass: "the parser will actually accept any identifier."
    "graph { A:port:invalid_compass; }"

    ;; node_stmt with attr list
    "graph { A [color=blue]; }"
    "graph { A [color=blue,label=hello]; }"

    ;; likely bug in the grammar, in graphviz the = does not seem to be optional
    ;; a_list 	: 	ID '=' ID [ (';' | ',') ] [ a_list ]
    ;  "graph { a [fixedsize] }"))
    ;  "graph { a [fixedsize=] }"))

    ;; node_stmt with port and attr list
    "graph { A:port [color=blue]; }"

    ;; edge_stmt node_id
    "digraph { A -> B; }"
    "digraph { A -> B -> C; }"
    "digraph { A -> B; B -> C; }"

    "graph { A -- B; }"

    "digraph { A -> subgraph { B; C; } ; }"
    "digraph { A -> subgraph some_sub_name { B; C; } ; }"

    ;; edge_stmt with subgraph
    "digraph { subgraph { } -> B; }"
    "digraph { subgraph some_sub_name { } -> B; }"

    ;; edge_stmt with attrs
    "digraph { A -> B [color=blue]; }"

    ;; attr_stmt
    "graph { graph [label=hi]; }"
    "graph { node [label=hi]; }"
    "graph { edge [label=hi]; }"

    ;; subgraph
    "graph { subgraph { } ; }"
    "graph { subgraph some_sub_name { } ; }"))

(deftest parser-roundtrip-significant-whitespace
  (are [input] (= input (-> input parser/parse dc/dot))
    ;; attribute with newline escape
    "graph {\nA [label=\"new\\nline\\r\"];\n} "

    ;; html string with newline
    "graph {\nA [label=<<b>bold label\n</b>>];\n} "

    "graph {\nA [label=\"double back slash in label \\\\.\"];\n} "))

(deftest parser
  (are [input expected] (= expected (-> input parser/parse dc/dot utils/normalize-whitespace))

    ;; Casing is insensitive, but gets normalized to lowercase
    "STRICT GRAPH {}"
    "strict graph { }"

    "STRICT GRAPH some_name {}"
    "strict graph some_name { }"

    ;; Trailing semicolon
    "graph { A }"
    "graph { A; }"

    ;; String id
    "graph \"some_name\" {}"
    "graph some_name { }"

    ;; edge_stmt with subgraphs
    "digraph { A -> { B C } }"
    "digraph { A -> subgraph { B; C; } ; }"

    "digraph { { A B } -> C }"
    "digraph { subgraph { A; B; } -> C; }"

    "digraph { { A B } -> { C D } }"
    "digraph { subgraph { A; B; } -> subgraph { C; D; } ; }"

    ;; attribute lists get normalized into a single one
    "graph { A [color=blue label=hello] }"
    "graph { A [color=blue,label=hello]; }"

    "graph { A [color=blue] [label=hello] }"
    "graph { A [color=blue,label=hello]; }"

    "digraph { A -> B [color=blue] [arrowhead=open arrowtail=diamond ]}"
    "digraph { A -> B [color=blue,arrowhead=open,arrowtail=diamond]; }"

    ;; ID = ID sets graph attributes
    "digraph { color = blue ; A -> B ; label = hi }"
    "digraph { graph [color=blue]; A -> B; graph [label=hi]; }"

    ;; Subgraph with just braces without the subgraph keyword
    "graph { {} }"
    "graph { subgraph { } ; }"

    "digraph { { A -> B } }"
    "digraph { subgraph { A -> B; } ; }"

    ; semicolon as an attribute separator
    "graph { A [color=blue;label=hello] }"
    "graph { A [color=blue,label=hello]; }"))

