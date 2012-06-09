(ns memory.core
  (:require [goog.dom :as gdom]
            [goog.dom.classes :as gclasses]
            [goog.events :as gevents]
            [goog.style :as gstyle]
            [clojure.browser.repl :as repl]))

(def *number-of-cards* 10)
(def *number-of-group* 2)

(def cards (atom []))
(def current-selection (atom #{}))
(def clock-start (atom nil))

(defn clock-now [] (.valueOf (js/Date.)))

(defn card-element
  "Retrieve gdom element associate with card at pos."
  [pos]
  (aget (gdom/getElementsByTagNameAndClass "div" "card") pos))

(defn show-card!
  "Show card face for pos, when not already visible."
  [pos]
  (let [e (card-element pos)]
    (when-not (gdom/getElementByClass "face" e)
      (doto e
        (gclasses/add "open")
        (gdom/append (doto (gdom/createElement "div")
                       (gclasses/add "face")
                       (gdom/append (str (@cards pos)))))))))

(defn hide-card!
  "Hide card face for pos."
  [pos]
  (let [e (card-element pos)]
    (gclasses/remove e "open")
    (gdom/removeChildren e)))

(defn clear-card!
  "Clear card for pos from game."
  [pos]
  (gclasses/add (card-element pos) "cleared"))

(defn display-score!
  "Display score in time it took to finish the game."
  []
  (let [score (- (clock-now) @clock-start)
        hundreds (mod (.round js/Math (/ score 10)) 100)
        seconds (mod (.floor js/Math (/ score 1000)) 60)
        minutes (.floor js/Math (/ score 60000))]
    (set! (. (gdom/getElement "score") -innerHTML)
          (str minutes ":" (if (< seconds 10) "0") seconds "." (if (< hundreds 10) "0") hundreds))))

(defn game-over!
  "Render game over message."
  []
  (doto (gdom/getElement "board")
    (gstyle/showElement false)
    gdom/removeChildren)
  (display-score!)
  (gstyle/showElement (gdom/getElement "cover") true))

(def worker (atom nil))

(defn cards-left
  "List cards left in the game."
  []
  (filter identity @cards))

(defn process-selection!
  "Process current selection, removing the cards when a group is cleared."
  []
  (reset! worker nil)
  (let [cleared (= 1 (count (into #{} (map @cards @current-selection))))]
    (doseq [pos @current-selection] (hide-card! pos))
    (when cleared
      (doseq [pos @current-selection] (clear-card! pos))
      (swap! cards (fn [cards]
                     (reduce #(assoc %1 %2 nil) cards @current-selection))))
    (when (empty? (cards-left))
      (game-over!)))
  (reset! current-selection #{}))

(defn handle-card-click!
  "Handle card click for a given pos."
  [pos]
  (when @worker
    (.clearTimeout js/window @worker)
    (process-selection!))
  (when (and (@cards pos)
             (not (@current-selection pos))
             (< (count @current-selection) *number-of-group*))
    (swap! current-selection conj pos)
    (show-card! pos)
    (when (= (count @current-selection) *number-of-group*)
      (if (= (count (cards-left)) *number-of-group*)
        (process-selection!)
        (reset! worker (.setTimeout js/window process-selection! 1000))))))

(defn create-card-element!
  "Create dom element for a card and bind click listener."
  [pos]
  (doto (gdom/createElement "div")
    (gclasses/add "card")
    (gevents/listen fast-click-event
                    (fn [event]
                      (.preventDefault event)
                      (handle-card-click! pos)))))

(defn render-cards!
  "Render the cards on the board."
  []
  (let [board (gdom/getElement "board")]
    (gdom/removeChildren board)
    (dotimes [pos (count @cards)]
      (let [elm (create-card-element! pos)]
        (gdom/append board elm)))
    (gstyle/showElement board true)))

(def fast-click-event
  (if (js* "'ontouchstart' in window") "touchstart" "mousedown"))

(defn shuffle
  "Fisher–Yates shuffle"
  [coll]
  (reduce (fn [coll i]
            (let [j (rand-int (count coll))]
              (assoc coll i (nth coll j) j (nth coll i))))
          (vec coll)
          (range (count coll))))

(defn populate-cards!
  "Populate cards atom with a newly shuffled set of groups."
  []
  (reset! cards (vec (shuffle (mapcat (fn [a] (take *number-of-group* (repeat a)))
                                      (range *number-of-cards*))))))

(defn start-new-game!
  "Start a new game of memory."
  []
  (gstyle/showElement (gdom/getElement "cover") false)
  (populate-cards!)
  (render-cards!)
  (reset! clock-start (clock-now)))

(defn insert-style!
  "Insert window size specific style."
  []
  (let [floor #(.floor js/Math %)
        border 6
        margin (floor (max (/ window/innerWidth 200)
                           (/ window/innerHeight 200)))
        size (- (first (filter (fn [size]
                                 (< (floor (/ (* 2 *number-of-cards*)
                                              (floor (/ window/innerWidth size))))
                                    (floor (/ window/innerHeight size))))
                               (range 500 10 -10)))
                (* 2 margin)
                border)
        style (gdom/createElement "style")]
    (gdom/append style (str "div.card{margin:" margin "px;width:" size "px;height:" size "px}"
                            "body{font-size:" (floor (* size 0.8)) "px}"))
    (gdom/append document/body style)))

(defn ^:export load []
  (insert-style!)
  (gstyle/showElement (gdom/getElement "cover") true)
  (gevents/listen (gdom/getElement "play-link") "click" start-new-game!))

(when (re-find #"\?debug" (. window/location -href))
  (repl/connect "http://localhost:9000/repl"))