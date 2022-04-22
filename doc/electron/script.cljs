(ns script)

(def path js/path)

(defn foo []
  (str "Hello from nbb! The current working directory is: "
       (path.resolve ".")))

#js {:foo foo}
