Declarative Normalizer

The project is to enable you to annotate the fields in your POJO with the transfermation functions you want to apply on. The compiler will generate the code and do type checking for you.

There are a few projects have been done in java to provide annotative/declairtive validation frameworks. They are implemented using run-time reflection and dymanic typing. The casting excetion will blow your application up if you make mistake in annotations. In addition, Java Reflection calls are ten times slower than compiled function calls in performance. To overcome these shortcomings, I decide to implement this type of framework using complier-time code generation in Scala. Scala provides a feature "Macros". It enable the developer to write functions for the complier to execute. The Maros function will return Expression (AST). The complier then injects the expression in the call site to expand the call site code.


The example is in Test.scala.



