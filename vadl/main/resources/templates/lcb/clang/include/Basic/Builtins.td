include "clang/Basic/BuiltinsBase.td"

class CustomTargetBuiltin<string prototype> : TargetBuiltin {
  let Spellings = ["__builtin_[(${namespace})]" # NAME];
  let Prototype = prototype;
  let Features = "";
}

[#th:block th:each="b : ${builtins}" ]
def [(${b.name})] : CustomTargetBuiltin<"[(${b.prototype})]">;
[/th:block]
