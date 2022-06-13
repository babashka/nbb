(ns nbb.impl.schema
  (:refer-clojure :exclude [defn])
  (:require
   [nbb.core :as nbb]
   [schema.core]
   [sci.core :as sci])
  (:require-macros [schema.macros :as macros]))

(def sns (sci/create-ns 'schema.core nil))

(def schema-namespace
  (sci/copy-ns schema.core sns))

(clojure.core/defn maybe-split-first [pred s]
  (if (pred (first s))
    [(first s) (next s)]
    [nil s]))

(clojure.core/defn apply-prepost-conditions
  "Replicate pre/postcondition logic from clojure.core/fn."
  [body]
  (let [[conds body] (maybe-split-first #(and (map? %) (next body)) body)]
    (concat (map (fn [c] `(assert ~c)) (:pre conds))
            (if-let [post (:post conds)]
              `((let [~'% (do ~@body)]
                  ~@(map (fn [c] `(assert ~c)) post)
                  ~'%))
              body))))

(def ^:dynamic *compile-fn-validation* (atom true))

(clojure.core/defn compile-fn-validation?
  "Returns true if validation should be included at compile time, otherwise false.
   Validation is elided for any of the following cases:
   *   function has :never-validate metadata
   *   *compile-fn-validation* is false
   *   *assert* is false AND function is not :always-validate"
  [env fn-name]
  (let [fn-meta (meta fn-name)]
    (and
     @*compile-fn-validation*
     (not (:never-validate fn-meta))
     (or (:always-validate fn-meta)
         *assert*))))

(clojure.core/defn process-fn-arity
  "Process a single (bind & body) form, producing an output tag, schema-form,
   and arity-form which has asserts for validation purposes added that are
   executed when turned on, and have very low overhead otherwise.
   tag? is a prospective tag for the fn symbol based on the output schema.
   schema-bindings are bindings to lift eval outwards, so we don't build the schema
   every time we do the validation.
  
  :ufv-sym should name a local binding bound to `schema.utils/use-fn-validation`.
  
  5-args arity is deprecated."
  ([env fn-name output-schema-sym bind-meta arity-form]
   (process-fn-arity {:env env :fn-name fn-name :output-schema-sym output-schema-sym
                      :bind-meta bind-meta :arity-form arity-form :ufv-sym 'ufv__}))
  ([{[bind & body] :arity-form :keys [env fn-name output-schema-sym bind-meta ufv-sym]}]
   (assert! (vector? bind) "Got non-vector binding form %s" bind)
   (when-let [bad-meta (seq (filter (or (meta bind) {}) [:tag :s? :s :schema]))]
     (throw (RuntimeException. (str "Meta not supported on bindings, put on fn name" (vec bad-meta)))))
   (let [original-arglist bind
         bind (with-meta (process-arrow-schematized-args env bind) bind-meta)
         [regular-args rest-arg] (split-rest-arg env bind)
         input-schema-sym (gensym "input-schema")
         input-checker-sym (gensym "input-checker")
         output-checker-sym (gensym "output-checker")
         compile-validation (compile-fn-validation? env fn-name)]
     {:schema-binding [input-schema-sym (input-schema-form regular-args rest-arg)]
      :more-bindings (when compile-validation
                       [input-checker-sym `(delay (schema.core/checker ~input-schema-sym))
                        output-checker-sym `(delay (schema.core/checker ~output-schema-sym))])
      :arglist bind
      :raw-arglist original-arglist
      :arity-form (if compile-validation
                    (let [bind-syms (vec (repeatedly (count regular-args) gensym))
                          rest-sym (when rest-arg (gensym "rest"))
                          metad-bind-syms (with-meta (mapv #(with-meta %1 (meta %2)) bind-syms bind) bind-meta)]
                      (list
                        (if rest-arg
                          (into metad-bind-syms ['& rest-sym])
                          metad-bind-syms)
                        `(let [validate# ~(if (:always-validate (meta fn-name))
                                            `true
                                            `(if-cljs (deref ~ufv-sym)
                                                      (if-bb (deref ~ufv-sym) (.get ~ufv-sym))))]
                           (when validate#
                             (let [args# ~(if rest-arg
                                            `(list* ~@bind-syms ~rest-sym)
                                            bind-syms)]
                               (if schema.core/fn-validator
                                 (schema.core/fn-validator :input
                                                           '~fn-name
                                                           ~input-schema-sym
                                                           @~input-checker-sym
                                                           args#)
                                 (when-let [error# (@~input-checker-sym args#)]
                                   (error! (utils/format* "Input to %s does not match schema: \n\n\t \033[0;33m  %s \033[0m \n\n"
                                                          '~fn-name (pr-str error#))
                                           {:schema ~input-schema-sym :value args# :error error#})))))
                           (let [o# (loop ~(into (vec (interleave (map #(with-meta % {}) bind) bind-syms))
                                                 (when rest-arg [rest-arg rest-sym]))
                                      ~@(apply-prepost-conditions body))]
                             (when validate#
                               (if schema.core/fn-validator
                                 (schema.core/fn-validator :output
                                                           '~fn-name
                                                           ~output-schema-sym
                                                           @~output-checker-sym
                                                           o#)
                                 (when-let [error# (@~output-checker-sym o#)]
                                   (error! (utils/format* "Output of %s does not match schema: \n\n\t \033[0;33m  %s \033[0m \n\n"
                                                          '~fn-name (pr-str error#))
                                           {:schema ~output-schema-sym :value o# :error error#}))))
                             o#))))
                    (cons (into regular-args (when rest-arg ['& rest-arg]))
                          body))})))

(clojure.core/defn process-fn-
  "Process the fn args into a final tag proposal, schema form, schema bindings, and fn form"
  [env name fn-body]
  (let [compile-validation (compile-fn-validation? env name)
        output-schema (extract-schema-form name)
        output-schema-sym (gensym "output-schema")
        bind-meta (or (when-let [t (:tag (meta name))]
                        (when (primitive-sym? t)
                          {:tag t}))
                      {})
        ufv-sym (gensym "ufv")
        processed-arities (map #(process-fn-arity {:env env :fn-name name :output-schema-sym output-schema-sym
                                                   :bind-meta bind-meta :arity-form % :ufv-sym ufv-sym})
                               (if (vector? (first fn-body))
                                 [fn-body]
                                 fn-body))
        schema-bindings (map :schema-binding processed-arities)
        fn-forms (map :arity-form processed-arities)]
    {:outer-bindings (vec (concat
                           (when compile-validation
                             `[~(with-meta ufv-sym {:tag 'java.util.concurrent.atomic.AtomicReference}) schema.utils/use-fn-validation])
                           [output-schema-sym output-schema]
                           (apply concat schema-bindings)
                           (mapcat :more-bindings processed-arities)))
     :arglists (map :arglist processed-arities)
     :raw-arglists (map :raw-arglist processed-arities)
     :schema-form (if (= 1 (count processed-arities))
                    `(schema.core/->FnSchema ~output-schema-sym ~[(ffirst schema-bindings)])
                    `(schema.core/make-fn-schema ~output-schema-sym ~(mapv first schema-bindings)))
     :fn-body fn-forms}))


(clojure.core/defn ^:macro defn
  "Like clojure.core/defn, except that schema-style typehints can be given on
   the argument symbols and on the function name (for the return value).
   You can call s/fn-schema on the defined function to get its schema back, or
   use with-fn-validation to enable runtime checking of function inputs and
   outputs.
   (s/defn foo :- s/Num
    [x :- s/Int
     y :- s/Num]
    (* x y))
   (s/fn-schema foo)
   ==> (=> java.lang.Number Int java.lang.Number)
   (s/with-fn-validation (foo 1 2))
   ==> 2
   (s/with-fn-validation (foo 1.5 2))
   ==> Input to foo does not match schema: [(named (not (integer? 1.5)) x) nil]
   See (doc schema.core) for details of the :- syntax for arguments and return
   schemas.
   The overhead for checking if run-time validation should be used is very
   small -- about 5% of a very small fn call.  On top of that, actual
   validation costs what it costs.
   You can also turn on validation unconditionally for this fn only by
   putting ^:always-validate metadata on the fn name.
   Gotchas and limitations:
    - The output schema always goes on the fn name, not the arg vector. This
      means that all arities must share the same output schema. Schema will
      automatically propagate primitive hints to the arg vector and class hints
      to the fn name, so that you get the behavior you expect from Clojure.
    - All primitive schemas will be passed through as type hints to Clojure,
      despite their legality in a particular position.  E.g.,
        (s/defn foo [x :- int])
      will fail because Clojure does not allow primitive ints as fn arguments;
      in such cases, use the boxed Classes instead (e.g., Integer).
    - Schema metadata is only processed on top-level arguments.  I.e., you can
      use destructuring, but you must put schema metadata on the top-level
      arguments, not the destructured variables.
      Bad:  (s/defn foo [{:keys [x :- s/Int]}])
      Good: (s/defn foo [{:keys [x]} :- {:x s/Int}])
    - Only a specific subset of rest-arg destructuring is supported:
      - & rest works as expected
      - & [a b] works, with schemas for individual elements parsed out of the binding,
        or an overall schema on the vector
      - & {} is not supported.
    - Unlike clojure.core/defn, a final attr-map on multi-arity functions
      is not supported."
  [_&form &env & defn-args]
  (let [[name & more-defn-args] (macros/normalized-defn-args &env defn-args)
        {:keys [doc tag] :as standard-meta} (meta name)
        {:keys [outer-bindings schema-form fn-body arglists raw-arglists]} (process-fn- &env name more-defn-args)]
    `(let ~outer-bindings
       (let [ret# (clojure.core/defn ~(with-meta name {})
                    ~(assoc (apply dissoc standard-meta (when false #_(macros/primitive-sym? tag) [:tag]))
                       :doc (str
                             (str "Inputs: " (if (= 1 (count raw-arglists))
                                               (first raw-arglists)
                                               (apply list raw-arglists)))
                             (when-let [ret (when (= (second defn-args) :-) (nth defn-args 2))]
                               (str "\n  Returns: " ret))
                             (when doc (str  "\n\n  " doc)))
                       :raw-arglists (list 'quote raw-arglists)
                       :arglists (list 'quote arglists)
                       :schema schema-form)
                    ~@fn-body)]
         (utils/declare-class-schema! (utils/fn-schema-bearer ~name) ~schema-form)
         ret#))))

(clojure.core/defn init []
  (nbb/register-plugin!
   ::schema
   {:namespaces {'schema.core (assoc schema-namespace
                                     'defn (sci/copy-var defn sns))}}))

