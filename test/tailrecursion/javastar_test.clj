(ns tailrecursion.javastar-test
  (:require [clojure.test :refer :all]
            [tailrecursion.javastar :refer :all]))

(deftest aliases
  (let [arr (double-array 100 1.0)
        sum #(java* [] double [doubles]
                    "double s = 0;
                     double[] arr = ~{};
                     for(int i = 0; i < arr.length; i++) {
                       s += arr[i];
                     }
                     return s;" %)]
    (let [answer (sum arr)]
      (is (= answer 100.0))
      (is (= (class answer) Double)))))

(deftest n-args
  (let [greet #(java* [] java.lang.String [String String] "return ~{} +\", \" + ~{} + \"!\"; " %1 %2)]
    (= "hi, Bob!" (greet "hi" "Bob")))

  (is (= "holy cow!" (java* [clojure.lang.Var clojure.lang.RT]
                            String [String String]
                            "Var str = RT.var(\"clojure.core\",\"str\");
                             return (String) str.invoke(~{},\" \", ~{}, \"!\");"
                            "holy"
                            "cow"))))
