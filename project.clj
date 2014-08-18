(defproject memory "1.0.0-SNAPSHOT"
  :description "A simple game of memory for in a web browser."

  :dependencies [[org.clojure/clojure "1.6.0"]]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-2311"]]
                   :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]}}

  :cljsbuild {:builds {:dev {:source-paths ["src"]
                             :compiler {:optimizations :advanced
                                        :output-to "public/memory.js"}}
                       :prod {:source-paths ["src"]
                             :compiler {:optimizations :whitespace
                                        :output-to "public/memory.js"}}}})
