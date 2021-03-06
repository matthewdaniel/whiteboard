(defproject whiteboard "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [lein-npm "0.6.2"]
                 [camel-snake-kebab "0.4.2"]
                 [day8.re-frame/http-fx "v0.2.0"]
                ;;  [com.degel/re-frame-firebase "0.8.0"]
                 [org.clojure/clojurescript "1.10.597"
                  :exclusions [com.google.javascript/closure-compiler-unshaded
                               org.clojure/google-closure-library
                               org.clojure/google-closure-library-third-party]]
                 [thheller/shadow-cljs "2.8.83"]
                 [reagent "0.9.1"]
                 [re-frame "0.11.0"]
                 [secretary "1.2.3"]
                 [garden "1.3.9"]
                 [ns-tracker "0.4.0"]]

  :npm {:dependencies ["luxon" "1.23.0"
                       "uuid" "^8"
                       "save-svg-as-png" "^1.4.17"
                       "fraction.js" "^4"
                       "@firebase/app" "^0.6.18"
                       "@firebase/database" "^0.9.7"
                       "@firebase/storage" "^0.4.6"
                      ;  "@svgdotjs/svg.js" "^3"
                       "d3-interpolate" "^1"
                       "d3-shape" "^1"]}
  :plugins [[lein-garden "0.3.0"]
            [lein-shell "0.5.0"]]

  :min-lein-version "2.5.3"

  :jvm-opts ["-Xmx1G"]

  :source-paths ["src/clj" "src/cljs"]

  :test-paths   ["test/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"
                                    "resources/public/css"]


  :garden {:builds [{:id           "screen"
                     :source-paths ["src/clj"]
                     :stylesheet   whiteboard.css/screen
                     :compiler     {:output-to     "resources/public/css/screen.css"
                                    :pretty-print? true}}]}

  :shell {:commands {"open" {:windows ["cmd" "/c" "start"]
                             :macosx  "open"
                             :linux   "xdg-open"}}}

  :aliases {"dev"          ["with-profile" "dev" "do"
                            ["run" "-m" "shadow.cljs.devtools.cli" "watch" "app"]]
            "prod"         ["with-profile" "prod" "do"
                            ["run" "-m" "shadow.cljs.devtools.cli" "release" "app"]]
            "build-report" ["with-profile" "prod" "do"
                            ["run" "-m" "shadow.cljs.devtools.cli" "run" "shadow.cljs.build-report" "app" "target/build-report.html"]
                            ["shell" "open" "target/build-report.html"]]
            "karma"        ["with-profile" "prod" "do"
                            ["run" "-m" "shadow.cljs.devtools.cli" "compile" "karma-test"]
                            ["shell" "karma" "start" "--single-run" "--reporters" "junit,dots"]]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "1.0.0"]
                   [re-frisk "0.5.4.1"]]
    :source-paths ["dev"]}

   :prod {}}

  :prep-tasks [["garden" "once"]])
