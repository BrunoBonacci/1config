(ns com.brunobonacci.oneconfig.diff
  (:import name.fraser.neil.plaintext.diff_match_patch
           name.fraser.neil.plaintext.diff_match_patch$Diff
           name.fraser.neil.plaintext.diff_match_patch$Operation)
  (:require [clojure.string :as str]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                          ----==| D I F F |==----                           ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn diff-strings
  "It compares two text and returns a list of their differences on a
  character level"
  [^String s1 ^String s2]
  (let [dmp (diff_match_patch.)
        diff (.diff_main dmp s1 s2 false)
        _ (.diff_cleanupSemantic dmp diff)
        - (.diff_cleanupEfficiency dmp diff)]
    (->> diff
       (map (fn [^diff_match_patch$Diff d]
              [(cond
                 (.equals (.-operation d) diff_match_patch$Operation/INSERT) :+
                 (.equals (.-operation d) diff_match_patch$Operation/DELETE) :-
                 (.equals (.-operation d) diff_match_patch$Operation/EQUAL)  :=)
               (.-text d)])))))



(defn diff-lines
  "It compares two text and returns a list of their differences on a line level"
  [^String s1 ^String s2]
  (let [dmp (diff_match_patch.)
        lines (.diff_linesToChars dmp s1 s2)
        diff (.diff_main dmp (.-chars1 lines) (.-chars2 lines) false)
        _ (.diff_charsToLines dmp diff (.-lineArray lines))
        _ (.diff_cleanupSemantic dmp diff)
        - (.diff_cleanupEfficiency dmp diff)]
    (->> diff
       (map (fn [^diff_match_patch$Diff d]
              [(cond
                 (.equals (.-operation d) diff_match_patch$Operation/INSERT) :+
                 (.equals (.-operation d) diff_match_patch$Operation/DELETE) :-
                 (.equals (.-operation d) diff_match_patch$Operation/EQUAL)  :=)
               (.-text d)])))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                   ----==| F O R M A T   D I F F |==----                    ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def ESC \u001b)
(def ANSI-CODES
  {:reset              (str ESC "[0m")
   :bright             (str ESC "[1m")
   :blink-slow         (str ESC "[5m")
   :underline          (str ESC "[4m")
   :underline-off      (str ESC "[24m")
   :inverse            (str ESC "[7m")
   :inverse-off        (str ESC "[27m")
   :strikethrough      (str ESC "[9m")
   :strikethrough-off  (str ESC "[29m")

   :default            (str ESC "[39m")
   :white              (str ESC "[37m")
   :black              (str ESC "[30m")
   :red                (str ESC "[31m")
   :green              (str ESC "[32m")
   :blue               (str ESC "[34m")
   :yellow             (str ESC "[33m")
   :magenta            (str ESC "[35m")
   :cyan               (str ESC "[36m")

   :bg-default         (str ESC "[49m")
   :bg-white           (str ESC "[47m")
   :bg-black           (str ESC "[40m")
   :bg-red             (str ESC "[41m")
   :bg-green           (str ESC "[42m")
   :bg-blue            (str ESC "[44m")
   :bg-yellow          (str ESC "[43m")
   :bg-magenta         (str ESC "[45m")
   :bg-cyan            (str ESC "[46m")
   })



;; expects something one of [:- str] [:+ str] [:= str]
(defmulti format-diff first)



(defmethod format-diff :+
  [[_ text]]
  (str (:bg-green ANSI-CODES) text (:reset ANSI-CODES)))



(defmethod format-diff :-
  [[_ text]]
  (str (:bg-red ANSI-CODES) text (:reset ANSI-CODES)))



(defmethod format-diff :=
  [[_ text]]
  (str (:reset ANSI-CODES) text))



(defn colorize-diff
  [diffs]
  (str (reduce str (map format-diff diffs)) (:reset ANSI-CODES)))





(comment

  (diff-strings "hello diff" "Hello diff!")
  ;; => ([:- "h"] [:+ "H"] [:= "ello diff"] [:+ "!"])

  (def sample2
    [
     "
Shopping list:
Oranges
Bananas
Coffee
Chocolate
Bread
"
     "
Shopping list:
Sicilian Oranges
Coffee
Chocolate
Bread
Italian Wine
"])

  (apply diff-lines sample2)

  ;; ([:= "\nShopping list:\n"]
  ;;  [:+ "Sicilian "]
  ;;  [:= "Oranges\n"]
  ;;  [:- "Bananas\n"]
  ;;  [:= "Coffee\nChocolate\nBread\n"]
  ;;  [:+ "Italian Wine\n"])


  (println (colorize-diff (apply diff-lines sample2)))

  )
