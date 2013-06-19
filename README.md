# javastar

Write Java inside Clojure:

```clojure
(require '[tailrecursion.javastar :refer [java*]])

(defn sum-doubles [arr]
  (java* [] double [doubles]
    "double s = 0;
     double[] arr = ~{};
     for(int i = 0; i < arr.length; i++) {
       s += arr[i];
     }
     return s;"
     arr))

(sum-doubles (double-array 10 1.0)) ;=> 10.0

(defn hi-from-java [name]
  (java* [] String [String] "return \"hi \" + ~{} + \"!\";" name))

(hi-from-java "Bob") ;=> "hi Bob!"

(defn add2 [x y]
  (java* [] long [long long] "return ~{} + ~{};" x y))

(add2 1 2) ;=> 3

(java* [clojure.lang.Var clojure.lang.RT]
       Object [String String]
       "Var str = RT.var(\"clojure.core\",\"str\");
        return str.invoke(~{},\" \", ~{}, \"!\");"
       "holy"
       "cow") ;=> "holy cow!"
```

Requires Java 1.6 JDK or higher.

## Dependency [![Build Status](https://travis-ci.org/tailrecursion/javastar.png?branch=master)](https://travis-ci.org/tailrecursion/javastar)

```clojure
[tailrecursion/javastar "1.1.6"]
```

## License

Copyright © 2013 Alan Dipert

Distributed under the Eclipse Public License, the same as Clojure.
