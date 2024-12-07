package vadl.javaannotations;

import com.google.auto.service.AutoService;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * An annotation processor for generating dispatchers for handler classes.
 *
 * <p>This processor works with classes annotated with {@link DispatchFor} and
 * methods annotated with {@link Handler}. It performs the following tasks:
 * <ul>
 *   <li>Collects all handler methods in the annotated class and its supertypes.</li>
 *   <li>Identifies all subclasses of the specified base type within
 *   the {@code vadl.*} package.</li>
 *   <li>Ensures that every subclass has a corresponding handler method,
 *   emitting errors for unhandled subclasses.</li>
 *   <li>Generates a dispatcher class that routes objects of the base type
 *   to the appropriate handler methods.</li>
 * </ul>
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({
    "vadl.javaannotations.Handler",
    "vadl.javaannotations.DispatchFor"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17) // Adjust as needed
public class HandlerProcessor extends AbstractProcessor {

  private Filer filer;
  private Messager messager;
  private Elements elementUtils;
  private Types typeUtils;

  /**
   * Initializes the annotation processor.
   *
   * @param env the processing environment
   */
  @Override
  public synchronized void init(ProcessingEnvironment env) {
    super.init(env);
    filer = env.getFiler();
    messager = env.getMessager();
    elementUtils = env.getElementUtils();
    typeUtils = env.getTypeUtils();
  }

  /**
   * Processes annotations {@code @DispatchFor} and {@code @Handler}.
   *
   * @param annotations the set of annotation types to process
   * @param roundEnv    the environment for this processing round
   * @return {@code true} if the annotations are claimed by this processor, {@code false} otherwise
   */
  @Override
  public boolean process(
      Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    // Process classes annotated with @DispatchFor
    for (Element elem : roundEnv.getElementsAnnotatedWith(DispatchFor.class)) {
      if (elem.getKind() != ElementKind.CLASS) {
        messager.printMessage(Diagnostic.Kind.ERROR,
            "@DispatchFor can only be applied to classes", elem);
        continue;
      }
      TypeElement handlerClass = (TypeElement) elem;

      try {
        processHandlerClass(handlerClass, roundEnv);
      } catch (IOException e) {
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Error generating dispatcher: " + e.getMessage(), handlerClass);
      }
    }
    return false;
  }

  /**
   * Processes a handler class annotated with {@code @DispatchFor}.
   *
   * @param handlerClass the annotated handler class
   * @param roundEnv     the environment for this processing round
   * @throws IOException if an error occurs while generating the dispatcher class
   */
  private void processHandlerClass(TypeElement handlerClass,
                                   RoundEnvironment roundEnv)
      throws IOException {

    DispatchForData dispatchForData = getDispatchForDataFromAnnotation(handlerClass);
    TypeMirror baseType = dispatchForData.baseType;
    List<String> includePackages = dispatchForData.includePackages;

    // Collect handler methods from the class and its supertypes
    Map<String, HandlerMethod> handlerMethods =
        collectHandlerMethods(handlerClass, baseType, dispatchForData.returnType);

    if (handlerMethods.isEmpty()) {
      messager.printMessage(Diagnostic.Kind.WARNING,
          "No valid @Handler methods found in " + handlerClass.getSimpleName(), handlerClass);
      return;
    }

    // Collect all non-abstract subclasses of baseType within vadl.* package
    Set<TypeElement> allSubTypes = collectAllSubTypes(baseType, roundEnv, includePackages);

    // Check for unhandled subclasses
    checkForUnhandledSubclasses(handlerMethods, allSubTypes, handlerClass,
        dispatchForData.returnType);

    // Generate dispatcher
    generateDispatcher(handlerClass, baseType, handlerMethods, dispatchForData.returnType);
  }

  /**
   * Collects handler methods from the handler class and its supertypes.
   *
   * @param handlerClass the handler class
   * @param baseType     the base type specified in {@code @DispatchFor}
   * @return a map of parameter type strings to handler methods
   */
  private Map<String, HandlerMethod> collectHandlerMethods(
      TypeElement handlerClass, TypeMirror baseType, TypeMirror returnType) {

    Map<String, HandlerMethod> handlerMethods = new HashMap<>();
    Set<TypeElement> processedClasses = new HashSet<>();
    collectHandlerMethodsRecursive(handlerClass, baseType, handlerMethods, processedClasses,
        returnType); // Pass 'returnType'
    return handlerMethods;
  }


  private record DispatchForData(
      TypeMirror baseType,
      List<String> includePackages,
      TypeMirror returnType
  ) {
  }

  /**
   * Get dispatch data for the handler class annotated with {@link DispatchFor}.
   */
  private DispatchForData getDispatchForDataFromAnnotation(TypeElement handlerClass) {
    TypeMirror baseType = null;
    List<String> includePackages = new ArrayList<>();
    TypeMirror returnType = null; // Add this line

    for (AnnotationMirror annotation : handlerClass.getAnnotationMirrors()) {
      if (annotation.getAnnotationType().toString().equals(DispatchFor.class.getCanonicalName())) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
            annotation.getElementValues();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>
            entry : values.entrySet()) {
          String key = entry.getKey().getSimpleName().toString();
          if (key.equals("value")) {
            baseType = (TypeMirror) entry.getValue().getValue();
          } else if (key.equals("include")) {
            @SuppressWarnings("unchecked")
            List<? extends AnnotationValue> includeValues =
                (List<? extends AnnotationValue>) entry.getValue().getValue();
            for (AnnotationValue av : includeValues) {
              includePackages.add((String) av.getValue());
            }
          } else if (key.equals("returnType")) { // Add this block
            returnType = (TypeMirror) entry.getValue().getValue();
          }
        }
      }
    }
    if (baseType == null) {
      throw new IllegalStateException("@DispatchFor annotation is missing a 'value' element.");
    }
    // If returnType is null, default to Void
    if (returnType == null) {
      returnType = elementUtils.getTypeElement("java.lang.Void").asType();
    }
    return new DispatchForData(baseType, includePackages, returnType);
  }

  /**
   * Recursively collects handler methods from a class and its supertypes.
   *
   * @param clazz            the current class to process
   * @param baseType         the base type specified in {@code @DispatchFor}
   * @param handlerMethods   a map of collected handler methods
   * @param processedClasses a set of classes that have already been processed
   */
  private void collectHandlerMethodsRecursive(
      TypeElement clazz, TypeMirror baseType, Map<String, HandlerMethod> handlerMethods,
      Set<TypeElement> processedClasses, TypeMirror returnType) {

    if (processedClasses.contains(clazz)) {
      return;
    }
    processedClasses.add(clazz);

    // Collect methods from the current class
    for (Element elem : clazz.getEnclosedElements()) {
      if (elem.getKind() == ElementKind.METHOD
          && elem.getAnnotation(Handler.class) != null) {

        ExecutableElement method = (ExecutableElement) elem;
        List<? extends VariableElement> parameters = method.getParameters();

        if (parameters.size() != 1) {
          messager.printMessage(Diagnostic.Kind.ERROR,
              "@Handler methods must have exactly one parameter", method);
          continue;
        }

        VariableElement param = parameters.get(0);
        TypeMirror paramType = param.asType();

        // Check if paramType is a subtype of baseType
        if (!typeUtils.isSubtype(paramType, baseType)) {
          messager.printMessage(Diagnostic.Kind.ERROR,
              "Parameter type of @Handler method must be a subtype of " + baseType.toString(),
              method);
          continue;
        }

        // Check return type if returnType is not Void
        if (!isVoid(returnType)) {
          TypeMirror methodReturnType = method.getReturnType();
          if (!typeUtils.isSubtype(methodReturnType, returnType)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                "Return type of @Handler method must be a subtype of " + returnType,
                method);
            continue;
          }
        }

        String paramTypeStr = paramType.toString();
        handlerMethods.put(paramTypeStr,
            new HandlerMethod(method.getSimpleName().toString(), paramType));
      }
    }

    // Process interfaces
    for (TypeMirror iface : clazz.getInterfaces()) {
      TypeElement ifaceElement = (TypeElement) typeUtils.asElement(iface);
      collectHandlerMethodsRecursive(ifaceElement, baseType, handlerMethods, processedClasses,
          returnType);
    }

    // Process superclass
    TypeMirror superclass = clazz.getSuperclass();
    if (superclass != null && superclass.getKind() != TypeKind.NONE) {
      TypeElement superClassElement = (TypeElement) typeUtils.asElement(superclass);
      collectHandlerMethodsRecursive(superClassElement, baseType, handlerMethods, processedClasses,
          returnType);
    }
  }

  private boolean isVoid(TypeMirror type) {
    return type.getKind() == TypeKind.VOID
        || typeUtils.isSameType(type, elementUtils.getTypeElement("java.lang.Void").asType());
  }

  /**
   * Collects all subclasses of the specified base type within the given include packages.
   * If no includePackage was specified, we search in all available packages.
   *
   * @param baseType the base type
   * @param roundEnv the environment for this processing round
   * @return a set of type elements representing the subclasses
   */
  private Set<TypeElement> collectAllSubTypes(TypeMirror baseType, RoundEnvironment roundEnv,
                                              List<String> includePackages) {
    Set<TypeElement> subTypes = new HashSet<>();
    for (Element element : roundEnv.getRootElements()) {
      if (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE) {
        TypeElement typeElement = (TypeElement) element;
        String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
        // Check if the class is in one of the included packages
        boolean packageIncluded = includePackages.isEmpty(); // Include all if 'include' is empty
        for (String includePackage : includePackages) {
          if (packageName.startsWith(includePackage)) {
            packageIncluded = true;
            break;
          }
        }
        if (packageIncluded) {
          TypeMirror typeMirror = typeElement.asType();
          if (typeUtils.isSubtype(typeMirror, baseType)
              && !typeUtils.isSameType(typeMirror, baseType)) {
            subTypes.add(typeElement);
          }
        }
      }
    }
    return subTypes;
  }

  /**
   * Checks if all subclasses of the base type are handled by the handler methods.
   *
   * @param handlerMethods the map of handler methods
   * @param subTypes       the set of subclasses
   * @param handlerClass   the handler class
   */
  private void checkForUnhandledSubclasses(Map<String, HandlerMethod> handlerMethods,
                                           Set<TypeElement> subTypes, TypeElement handlerClass,
                                           TypeMirror returnType) {
    var missingTypes = new ArrayList<TypeMirror>();

    for (TypeElement subtype : subTypes) {
      TypeMirror subtypeMirror = subtype.asType();
      if (subtype.getModifiers().contains(Modifier.ABSTRACT)) {
        // only check non-abstract sub types
        continue;
      }

      if (!isSubtypeHandled(subtypeMirror, handlerMethods)) {
        missingTypes.add(subtypeMirror);
        messager.printMessage(Diagnostic.Kind.ERROR,
            "No handler found for subclass: " + subtype.getQualifiedName().toString(),
            handlerClass);
      }
    }

    printSuggestedMethodsToAdd(missingTypes, handlerClass, returnType);
  }

  private void printSuggestedMethodsToAdd(List<TypeMirror> missingTypes, TypeElement handlerClass,
                                          TypeMirror returnType) {
    if (missingTypes.isEmpty()) {
      return;
    }

    var returnTypeStr = isVoid(returnType) ? "void" : typeMirrorSimpleName(returnType);

    StringBuilder sb = new StringBuilder();
    sb.append("Add the following to %s: \n".formatted(handlerClass.getSimpleName()));
    for (var type : missingTypes) {
      var typeName = typeMirrorSimpleName(type);
      sb.append("@Handler\n");
      sb.append(returnTypeStr);
      sb.append(" handle(");
      sb.append(typeName);
      sb.append(" toHandle) {\n");
      sb.append(
          "\tthrow new UnsupportedOperationException(\"Type %s not yet implemented\");\n".formatted(
              typeName));
      sb.append("}\n\n");
    }

    messager.printMessage(Diagnostic.Kind.NOTE, sb.toString(), handlerClass);
  }

  /**
   * Determines if a given subtype is handled by the handler methods.
   *
   * @param subtype        the subtype to check
   * @param handlerMethods the map of handler methods
   * @return {@code true} if the subtype is handled, {@code false} otherwise
   */
  private boolean isSubtypeHandled(TypeMirror subtype, Map<String, HandlerMethod> handlerMethods) {
    // Check if there is a handler for the subtype or any of its supertypes
    for (HandlerMethod hm : handlerMethods.values()) {
      if (typeUtils.isSubtype(subtype, hm.paramType)) {
        return true;
      }
    }
    return false;
  }


  private record HandlerMethod(
      String methodName,
      TypeMirror paramType
  ) {
  }

  /**
   * Generates the dispatcher class for the handler.
   *
   * @param handlerClass   the handler class
   * @param baseType       the base type
   * @param handlerMethods the map of handler methods
   * @throws IOException if an error occurs while writing the dispatcher class
   */
  private void generateDispatcher(TypeElement handlerClass, TypeMirror baseType,
                                  Map<String, HandlerMethod> handlerMethods, TypeMirror returnType)
      throws IOException {
    String handlerClassName = handlerClass.getQualifiedName().toString();
    String packageName = elementUtils.getPackageOf(handlerClass).getQualifiedName().toString();
    String dispatcherClassName = handlerClass.getSimpleName() + "Dispatcher";

    JavaFileObject file = filer.createSourceFile(packageName + "." + dispatcherClassName);
    try (Writer writer = file.openWriter()) {
      writer.write("package " + packageName + ";\n\n");

      // Import base type
      writer.write("import " + baseType.toString() + ";\n");

      // Import handler class
      writer.write("import " + handlerClassName + ";\n");

      // Import types for the handler methods
      Set<String> importTypes = new HashSet<>();
      for (HandlerMethod hm : handlerMethods.values()) {
        importTypes.add(hm.paramType.toString());
      }
      for (String importType : importTypes) {
        writer.write("import " + importType + ";\n");
      }

      writer.write("\n");
      writer.write("public class " + dispatcherClassName + " {\n\n");

      String baseTypeName = baseType.toString().substring(baseType.toString().lastIndexOf('.') + 1);

      // Determine if returnType is void
      boolean isVoidReturnType = isVoid(returnType);

      String returnTypeName = isVoidReturnType ? "void" : returnType.toString();

      writer.write("    @SuppressWarnings(\"BadInstanceof\")\n");
      writer.write(
          "    public static " + returnTypeName + " dispatch(" + handlerClassName
              + " handler, " + baseTypeName
              + " obj) {\n");

      // Sort handler methods by specificity
      List<HandlerMethod> sortedHandlerMethods = new ArrayList<>(handlerMethods.values());
      sortedHandlerMethods.sort((hm1, hm2) -> {
        TypeMirror type1 = hm1.paramType;
        TypeMirror type2 = hm2.paramType;

        if (typeUtils.isSubtype(type1, type2) && !typeUtils.isSameType(type1, type2)) {
          return -1; // type1 is more specific
        } else if (typeUtils.isSubtype(type2, type1) && !typeUtils.isSameType(type1, type2)) {
          return 1; // type2 is more specific
        } else {
          return 0; // Unrelated types
        }
      });

      for (int i = 0; i < sortedHandlerMethods.size(); i++) {
        HandlerMethod hm = sortedHandlerMethods.get(i);
        String paramTypeStr = hm.paramType.toString();
        String simpleParamType = paramTypeStr.substring(paramTypeStr.lastIndexOf('.') + 1);
        String methodName = hm.methodName;

        if (i == 0) {
          writer.write("        if (obj instanceof " + simpleParamType + ") {\n");
        } else {
          writer.write("        else if (obj instanceof " + simpleParamType + ") {\n");
        }

        if (isVoidReturnType) {
          writer.write("            handler." + methodName + "((" + simpleParamType + ") obj);\n");
        } else {
          writer.write(
              "            return handler." + methodName + "((" + simpleParamType + ") obj);\n");
        }
        writer.write("        }\n");
      }


      // Handle the 'else' case
      writer.write("        else {\n");
      if (isVoidReturnType) {
        writer.write(
            "            "
                + "throw new IllegalArgumentException(\"Unhandled type: \" + obj.getClass());\n");
      } else {
        writer.write(
            "            "
                + "throw new IllegalArgumentException(\"Unhandled type: \" + obj.getClass());\n");
      }
      writer.write("        }\n");

      // If returnType is not void, ensure all code paths return a value
      if (!isVoidReturnType) {
        writer.write("        // This line should be unreachable\n");
      }

      writer.write("    }\n");

      writer.write("}\n");
    }
  }

  private String typeMirrorSimpleName(TypeMirror typeMirror) {
    return typeMirror.toString().substring(typeMirror.toString().lastIndexOf('.') + 1);
  }
}