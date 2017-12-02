(ns snek.core
    (:require ))

(enable-console-print!)

(declare game graphics timer game-over-text)

(def game-width 600)
(def game-height 400)

(defonce app-state
  (atom {:text "Hello world!"}))

(def black 0x000000)
(def white 0xFFFFFF)
(def orange 0xFFA500)
(def tile-size 40)
(def grid-width (/ game-width tile-size))
(def grid-height (/ game-height tile-size))

(defn fill-tile [x y color]
  (doto graphics
    (.beginFill color)
      (.drawRect
       (* x tile-size)
       (* y tile-size)
       (dec tile-size)
       (dec tile-size))
      (.endFill)))

(defn draw-grid []
  (doseq [x (range 0 grid-width)
          y (range 0 grid-height)]
    (fill-tile x y white)))

(defn render [state]
  (.clear graphics)
  (draw-grid)
  (doseq [[x y] (:snake state)]
    (fill-tile x y black))
  (let [[fx fy] (:food state)]
    (fill-tile fx fy orange)))

(defn ph-update []
  (set! (.-delay timer) (:timer-delay @app-state))
  (set! (.-visible game-over-text) (:game-over @app-state)))

(defn step-head [[x y] [dx dy]]
  [(mod (+ x dx) grid-width)
   (mod (+ y dy) grid-height)])

(defn register-key! [callback key]
  (let [key (.input.keyboard.addKey game key)]
    (.onDown.add key callback)))

(defn move-snake [state]
  (let [snake-len (count (:snake state))
        snake-tail (take (dec snake-len) (:snake state))]
    (assoc state :snake
           (into [(step-head (first (:snake state)) (:dir state))]
                 snake-tail))))

(defn tiles-without-snake [snake]
  (for [x (range 0 grid-width)
        y (range 0 grid-height)
        :when (not-any? #(= [x y] %) snake)]
    [x y]))

(defn food-spawn [snake]
  (rand-nth (tiles-without-snake snake)))

(defn grow-snake-and-spawn-more-food [state]
  (let [snake (:snake state)]
    (assoc state
           :snake (conj snake (last snake))
           :food (food-spawn snake)
           :timer-delay (* (:timer-delay state) 0.95))))

(defn check-food [state]
  (let [snake-head (first (:snake state))]
    (if (= (:food state) snake-head)
      (grow-snake-and-spawn-more-food state)
      state)))

(defn snake-collided? [snake]
  (some #{(first snake)} (rest snake)))

(defn check-for-game-over [state]
  (if (snake-collided? (:snake state))
    (assoc state :game-over true)
    state))

(defn tick []
  (when-not (:game-over @app-state)
    (swap! app-state move-snake)
    (swap! app-state check-food)
    (swap! app-state check-for-game-over))
  (render @app-state))

(defn preload [])

(defn make-dir-callback! [key dir]
  (register-key! #(swap! app-state assoc :dir dir) key))

(defn restart! []
  (let [start-snake [[7 4] [7 3]]]
    (swap! app-state assoc
           :snake start-snake
           :dir (rand-nth [[-1 0] [1 0] [0 -1] [0 1]])
           :food (food-spawn start-snake)
           :timer-delay 500
           :game-over false)))

(defn create []
  (def graphics (.add.graphics game 0 0))
  (def game-over-text (.add.text game
                                 (.-centerX game.world)
                                 (.-centerY game.world)
                                 "GAME OVER\npress R to restart"
                                 #js {:font "65px Arial"
                                      :fill "#CCAA88"
                                      :align "center"}))
  (.anchor.setTo game-over-text 0.5 0.5)
  (def timer (.time.events.loop game 500 tick))
  (restart!)
  (make-dir-callback! Phaser.Keyboard.W [0 -1])
  (make-dir-callback! Phaser.Keyboard.A [-1 0])
  (make-dir-callback! Phaser.Keyboard.S [0 1])
  (make-dir-callback! Phaser.Keyboard.D [1 0])
  (register-key! restart! Phaser.Keyboard.R)
  (tick))

(def state-object
  #js {:preload preload
       :create create
       :update ph-update})

(defonce game
  (Phaser.Game. game-width game-height Phaser.AUTO "app" state-object))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )

(comment
  @app-state
  (move-snake {:snake [[7 4]] :dir [1 0]})
  (swap! app-state assoc :dir [-1 -1])
  (swap! app-state assoc :snake [[7 4] [7 3]])
  (register-key! #(println "hej") Phaser.Keyboard.G)
  )
