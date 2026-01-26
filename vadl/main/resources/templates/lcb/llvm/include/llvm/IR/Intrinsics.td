let TargetPrefix = "[(${namespace})]" in {
[#th:block th:each="intrinsic: ${intrinsics}" ]
  def int_[(${intrinsic.name})] : Intrinsic<[ [(${intrinsic.result})] ],
                                                      [ [(${intrinsic.param})] ],
                                                      [  [(${intrinsic.attr})] ]>;
[/th:block]
}