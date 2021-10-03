(ns nbb.reagent
  (:require
   [nbb.core :as nbb]
   [reagent.core :as r]
   [reagent.debug :as d :refer-macros [dev?]]
   [reagent.ratom :as ratom]
   [sci.core :as sci]))

;; The with-let macro from reagent.core. The only change is that the
;; interop/unchecked-aget+set were replaced by aget and aset.
(defn ^:macro with-let [_ _ bindings & body]
  (assert (vector? bindings)
          (str "with-let bindings must be a vector, not "
               (pr-str bindings)))
  (let [v (gensym "with-let")
        k (keyword v)
        init (gensym "init")
        ;; V is a reaction, which holds a JS array.
        ;; If the array is empty, initialize values and store to the
        ;; array, using binding index % 2 to access the array.
        ;; After init, the bindings are just bound to the values in the array.
        bs (into [init `(zero? (alength ~v))]
                 (map-indexed (fn [i x]
                                (if (even? i)
                                  x
                                  (let [j (quot i 2)]
                                    ;; Issue 525
                                    ;; If binding value is not yet set,
                                    ;; try setting it again. This should
                                    ;; also throw errors for each render
                                    ;; and prevent the body being called
                                    ;; if bindings throw errors.
                                    `(if (or ~init
                                             (not (.hasOwnProperty ~v ~j)))
                                       (aset ~v ~j ~x)
                                       (aget ~v ~j)))))
                              bindings))
        [forms destroy] (let [fin (last body)]
                          (if (and (list? fin)
                                   (= 'finally (first fin)))
                            [(butlast body) `(fn [] ~@(rest fin))]
                            [body nil]))
        add-destroy (when destroy
                      (list
                       `(let [destroy# ~destroy]
                          (if (reagent.ratom/reactive?)
                            (when (nil? (.-destroy ~v))
                              (set! (.-destroy ~v) destroy#))
                            (destroy#)))))
        asserting (dev?) #_(if *assert* true false)
        res (gensym "res")]
    `(let [~v (reagent.ratom/with-let-values ~k)]
       ~(when asserting
          `(when-some [c# (reagent.ratom/-ratom-context)]
             (when (== (.-generation ~v) (.-ratomGeneration c#))
               (d/error "Warning: The same with-let is being used more "
                        "than once in the same reactive context."))
             (set! (.-generation ~v) (.-ratomGeneration c#))))
       (let ~(into bs [res `(do ~@forms)])
         ~@add-destroy
         ~res))))

(def rns (sci/create-ns 'reagent.core nil))

(def reagent-namespace
  {'atom (sci/copy-var r/atom rns)
   'as-element (sci/copy-var r/as-element rns)
   'with-let (sci/copy-var with-let rns)
   'cursor (sci/copy-var r/cursor rns)})

(def rtmns (sci/create-ns 'reagent.ratom nil))

(defn -ratom-context
  "Read-only access to the ratom context."
  []
  ratom/*ratom-context*)

(def reagent-ratom-namespace
  {'with-let-values (sci/copy-var ratom/with-let-values rtmns)
   'reactive? (sci/copy-var ratom/reactive? rtmns)
   '-ratom-context (sci/copy-var -ratom-context rtmns)})

(def rdbgns (sci/create-ns 'reagent.debug nil))

(defn -tracking? []
  reagent.debug/tracking)

(defn ^:macro error
  "Print with console.error."
  [_ _ & forms]
  (when *assert*
    `(when (some? js/console)
       (.error (if (reagent.debug/-tracking?)
                 reagent.debug/track-console
                 js/console)
               (str ~@forms)))))

(def reagent-debug-namespace
  {'error (sci/copy-var error rdbgns)
   '-tracking? (sci/copy-var -tracking? rdbgns)
   'track-console (sci/copy-var d/track-console rdbgns)})

(defn init []
  (nbb/register-plugin!
   ::reagent
   {:namespaces {'reagent.core reagent-namespace
                 'reagent.ratom reagent-ratom-namespace
                 'reagent.debug reagent-debug-namespace}}))
