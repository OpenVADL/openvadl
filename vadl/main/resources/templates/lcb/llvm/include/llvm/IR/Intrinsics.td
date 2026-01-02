let TargetPrefix = "[(${namespace})]" in {
[#th:block th:each="intrinsic: ${intrinsics}" ]
  def [(${intrinsic.name})] : DefaultAttrsIntrinsic<[ [(${intrinsic.result})] ],
                                                      [ [(${intrinsic.param})] ],
                                                      [  [(${intrinsic.attr})] ]>;
[/th:block]
}