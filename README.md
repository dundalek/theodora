# Theodora Graphviz parser

Theodora is a library to parse [Graphviz](https://graphviz.org/doc/info/lang.html) graphs as data in Clojure.

It is like [Dorothy](https://github.com/daveray/dorothy) in reverse, and uses the same representation, which enables graphs to be manipulated and serialized back to text.
It is extracted as a standalone library from [Stratify](https://github.com/dundalek/stratify).

## Getting started

Add dependency to `deps.edn`:

```clojure
{:deps {io.github.dundalek/theodora {:git/tag "" :git/sha ""}}}
```

Use the `theodora.core/parse` function to parse a graph:

```clojure
(require '[theodora.core :as theodora])

(theodora/parse "digraph example { A -> B }")
;; => 
; {:id "example"
;  :statements [{:attrs {}
;                :node-ids [{:id "A" :type :dorothy.core/node-id}
;                           {:id "B" :type :dorothy.core/node-id}]
;                :type :dorothy.core/edge}]
;  :strict? false
;  :type :dorothy.core/digraph}
```

For input `parse` accepts Strings, InputStreams, and Readers. To parse a file:

```clojure
(theodora/parse (clojure.java.io/reader "graph.dot"))
```

To serialize back to string add `dorothy/dorothy {:mvn/version "0.0.7"}` to deps and call `dorothy.core/dot`:

```clojure
(require '[dorothy.core :as dorothy])

(-> (theodora/parse "digraph example { A -> B }")
    (assoc :id "hello")
    (dorothy/dot))
;; =>
; "digraph hello {\nA -> B;\n}"
```

## Details

Properties:

1. Stringifying parsed graph preserves the information.  
   For most `image(input) == image(stringify(parse(input)))`  
   *When rendering with graphviz it will result in the same image. There are few exceptions like more complicated HTML labels.*

2. Stringifying parsed graph again will result in the same output including formatting.  
   For all `roundtrip(input) == roundtrip(roundtrip(input))`

3. However, stringifying parsed graph does not necessarily preserve formatting.  
   There exists `input != parse(stringify(input))`  
   *The output will likely be different from the original input, because Dorothy will normalize the output like putting statements on separate lines and separating with semicolons.*  

where:

- input: valid graph in DOT language
- parse: `theodora.core/parse`
- stringify: `dorothy.core/dot`
- image: `dot -Tsvg` command
- rountrip(input): `parse(stringify(input))`

## Limitations

- Only UTF-8 encoded input is supported.  
  *Compared to Graphviz which has an ability to specify different encodings like `-Gcharset=latin1`.*
- Backslashes are not unescaped.  
  *Because Graphviz defines special [escapes sequences](https://graphviz.org/docs/attr-types/escString/) like `\G`, `\N`, `\E`, unescaping backslashes and escaping back would be ambiguous.*  
  *There are three ways to specify a newline that differ by text alignment: `\n`, `\r`, and `\l`.*  
  *It may be useful to replace newlines manually like `(str/replace label #"\\[nrl]", "\n")`.*
- Dorothy does not seem to encode "edgeop", so it cannot express mixed edge types like `A -- B -> C`.  
  *It determines edge type based on graph type: `--` for `graph` or `->` for `digraph`.*

## License

MIT  
Copyright (c) 2024 Jakub Dundalek

Includes clj-antlr code distributed under Eclipse Public License  
Copyright © 2014 Kyle Kingsbury aphyr@aphyr.com, and Factual, Inc.

Includes DOT.g4 grammar distributed under the BSD 3-clause license  
Copyright (c) 2013 Terence Parr
