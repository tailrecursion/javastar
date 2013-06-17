(ns tailrecursion.javastar
  (:require
   [alandipert.interpol8 :refer [interpolating]])
  (:import
   [javax.tools JavaCompiler SimpleJavaFileObject ToolProvider JavaFileObject$Kind]))

(defn source-object
  [class-name source]
  (proxy [SimpleJavaFileObject]
      [(java.net.URI/create (str "string:///"
                                 (.replace class-name \. \/)
                                 (. JavaFileObject$Kind/SOURCE extension)))
       JavaFileObject$Kind/SOURCE]
    (getCharContent [_] source)))

(defn class-object
  [class-name bos]
  (proxy [SimpleJavaFileObject]
      [(java.net.URI/create (str "string:///"
                                 (.replace ^String class-name \. \/)
                                 (. JavaFileObject$Kind/CLASS extension)))
       JavaFileObject$Kind/CLASS]
    (openOutputStream [] bos)))

(defn class-manager
  [manager]
  (let [bos (java.io.ByteArrayOutputStream.)]
    (proxy [javax.tools.ForwardingJavaFileManager] [manager]
      (getClassLoader [location]
        (proxy [clojure.lang.DynamicClassLoader] []
          (findClass [name]
            (proxy-super defineClass name (.toByteArray bos) nil))))
      (getJavaFileForOutput [location class-name kind sibling]
        (class-object class-name bos)))))

(defn compile-java
  [class-name source]
  (let [compiler (ToolProvider/getSystemJavaCompiler)
        mgr (class-manager (.getStandardFileManager compiler nil nil nil))
        task (.getTask compiler nil mgr nil nil nil [(source-object class-name source)])]
    (if (.call task)
      (.loadClass (.getClassLoader mgr nil) class-name)
      (throw (RuntimeException. "Error compiling inline Java.")))))

(defn occurrences
  "Count of the occurrences of substring in s."
  ([s substring]
     (occurrences 0 s substring))
  ([n ^String s ^String substring]
     (let [i (.indexOf s substring)]
       (if (neg? i)
         n
         (recur (inc n) (.substring s (inc i)) substring)))))

(defn substitute
  "Replace pattern in s with substitutions."
  [s pattern substitutions]
  (reduce #(.replaceFirst ^String %1 pattern %2) s substitutions))

(defn generate-class
  [return-type arg-types code]
  (let [class-name (str (gensym "tailrecursion_java_STAR_class"))
        n (occurrences code "~{}")
        arg-names (mapv str (repeatedly n gensym))
        arguments (->> (map #(str %1 " " %2) arg-types arg-names)
                       (interpose \,)
                       (apply str))
        method-body (substitute code "~\\{\\}" arg-names)
        class-body (interpolating
                    "public class #{class-name} {
                       public static #{return-type} m (#{arguments}) {
                         #{method-body}
                       }
                     }")]
    (compile-java class-name class-body)
    (symbol class-name)))

(def prim-aliases
  {'void     "void"
   'boolean  "boolean"
   'byte     "byte"
   'char     "char"
   'float    "float"
   'int      "int"
   'double   "double"
   'long     "long"
   'short    "short"
   'booleans "boolean []"
   'bytes    "byte []"
   'chars    "char []"
   'floats   "float []"
   'ints     "int []"
   'doubles  "double []"
   'longs    "long []"
   'shorts   "short []"})

(defn unalias [sym]
  (or (get prim-aliases sym)
      (if-let [klass (get (ns-imports *ns*) sym)]
        (.getName klass))
      (throw (IllegalArgumentException.
              (str "Unknown symbol: " (name sym))))))

(defmacro java*
  "Similar to ClojureScript's js*.  Compiles a Java code block with
  spliced args.

  Unlike js*, java* requires type information.  return-type and
  arg-types may be either Java classes or symbol aliases for primitive
  types and arrays.  See prim-aliases for available aliases.

  Example:

  (def java-add #(java* long [long long] \"return ~{} + ~{};\" %1 %2))
  (java-add 1 2) ;=> 3"
  [return-type arg-types code & args]
  (let [g (generate-class (unalias return-type) (map unalias arg-types) code)]
    `(. ~g ~'m ~@args)))
