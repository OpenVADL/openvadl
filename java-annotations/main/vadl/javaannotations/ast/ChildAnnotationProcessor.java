// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.javaannotations.ast;

import com.google.auto.service.AutoService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * A annotation processor that provides the children of each AST Node.
 *
 * <p>The {@link Child} indicates which fields are children.
 *
 * <p>The processor generates a single file called "ChildNodeRegistry" which then provides a method
 * to get the children for the nodes.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({
    "vadl.javaannotations.ast.Child",
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SuppressWarnings("processing")
public class ChildAnnotationProcessor extends AbstractProcessor {

  private Filer filer;
  private Messager messager;
  private Types typeUtils;
  private static final String packageName = "vadl.ast";

  @Nullable
  JavaFileObject registryFile;

  private final Map<TypeElement, List<VariableElement>> annotatedFieldsByClass = new HashMap<>();

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
    typeUtils = processingEnv.getTypeUtils();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    // Generate the registryFile as soon as we know we need it to avoid a javac warning.
    // Inspiration from:
    // https://github.com/avaje/avaje-inject/issues/128#issuecomment-883721014
    if (registryFile == null && !annotations.isEmpty()) {
      try {
        registryFile = filer.createSourceFile(packageName + ".NodeChildrenRegistry");
      } catch (IOException e) {
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate node children registry: " + e.getMessage());
        return false;
      }
    }

    // Only when the processing is over generate one single file.
    if (roundEnv.processingOver()) {
      addAllInheritedFields();
      try {
        if (!annotatedFieldsByClass.isEmpty()) {
          generateNodeChildrenRegistry();
        }
      } catch (IOException e) {
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to generate node children registry: " + e.getMessage());
      }
      return false;
    }

    if (annotations.isEmpty()) {
      return false;
    }

    // Collect all fields annotated with @Child
    Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(Child.class);

    for (Element element : annotatedElements) {
      if (element.getKind() != ElementKind.FIELD) {
        messager.printMessage(Diagnostic.Kind.ERROR,
            "@Child can only be applied to fields", element);
        continue;
      }

      VariableElement field = (VariableElement) element;
      TypeElement classElement = (TypeElement) field.getEnclosingElement();

      List<VariableElement> fields =
          annotatedFieldsByClass.getOrDefault(classElement, new ArrayList<>());
      fields.add(field);
      annotatedFieldsByClass.put(classElement, fields);
    }

    return false;
  }

  private void addAllInheritedFields() {
    for (Map.Entry<TypeElement, List<VariableElement>> entry :
        annotatedFieldsByClass.entrySet()) {
      TypeMirror type = entry.getKey().getSuperclass();
      while (!(type instanceof NoType)) {
        TypeElement superElement = (TypeElement) typeUtils.asElement(type);
        List<VariableElement> superFields = annotatedFieldsByClass.get(superElement);
        if (superFields != null) {
          entry.getValue().addAll(0, superFields);
        }
        type = superElement.getSuperclass();
      }
    }
  }

  private void generateNodeChildrenRegistry() throws IOException {
    JavaFileObject registryFile = Objects.requireNonNull(this.registryFile);
    try (PrintWriter out = new PrintWriter(registryFile.openWriter())) {
      // Write package and imports
      out.println("// Generated code from %s".formatted(this.getClass().getName()));
      out.println("package " + packageName + ";");
      out.println();
      out.println("import java.util.List;");
      out.println("import java.util.ArrayList;");
      out.println("import java.util.Collections;");
      out.println("import java.util.Map;");
      out.println("import java.util.HashMap;");
      out.println("import java.util.function.Function;");
      out.println();

      // Generate registry class
      out.println("public final class NodeChildrenRegistry {");
      out.println(
          "    private static final Map<Class<?>, Function<Node, List<Node>>> COLLECTORS = "
              + "new HashMap<>();");
      out.println();

      // Static initializer to populate map
      out.println("    static {");

      // Register a collector for each node type
      for (Map.Entry<TypeElement, List<VariableElement>> entry :
          annotatedFieldsByClass.entrySet()) {
        TypeElement classElement = entry.getKey();

        String className = classElement.getQualifiedName().toString();

        out.println("        COLLECTORS.put(" + className + ".class, (node) -> {");
        out.println("            " + className + " n = (" + className + ") node;");
        out.println("            List<Node> children = new ArrayList<>();");

        // Add code to collect children for this node type
        List<VariableElement> childFields = entry.getValue();
        for (VariableElement field : childFields) {
          String fieldName = field.getSimpleName().toString();
          TypeMirror fieldType = field.asType();
          String fieldTypeString = fieldType.toString();

          // Handle field visibility
          Set<Modifier> modifiers = field.getModifiers();
          String accessPrefix = "";
          if (modifiers.contains(Modifier.PRIVATE)) {
            // Need to use getter if field is private
            String getterName =
                "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            accessPrefix = "n." + getterName + "()";
          } else {
            accessPrefix = "n." + fieldName;
          }

          // Handle different field types
          if (fieldTypeString.startsWith("java.util.List")) {
            out.println("            if (" + accessPrefix + " != null) {");
            out.println("               for (var child: " + accessPrefix + ") {");
            out.println("                   children.add((Node) child);");
            out.println("               }");
            out.println("            }");
          } else {
            out.println("            if (" + accessPrefix + " != null) {");
            out.println("                children.add((Node)" + accessPrefix + ");");
            out.println("            }");
          }
        }

        out.println("            return children;");
        out.println("        });");
      }

      out.println("    }");
      out.println();

      // Method to get children for any node
      out.println("    public static List<Node> getChildren(Node node) {");
      out.println(
          "        Function<Node, List<Node>> collector = COLLECTORS.get(node.getClass());");
      out.println("        if (collector != null) {");
      out.println("            return collector.apply(node);");
      out.println("        }");
      out.println("        return Collections.emptyList();");
      out.println("    }");
      out.println("");


      // Method to get all children but specify the exact class, only used for edgecases
      out.println("    /**");
      out.println("     * Specify the class directly as which it should be loaded.");
      out.println(
          "     * This should only be used when you know what you do, like if you want "
              + "to get the children");
      out.println("     * from your superclass.");
      out.println("     *");
      out.println("     * @param node from which the children are loaded.");
      out.println("     * @param nodeType as which the node should be interpreted.");
      out.println("     * @return the children.");
      out.println("     */");
      out.println(
          "    public static List<Node> unsafeGetChildrenDirect(Node node, "
              + "Class<? extends Node> nodeType) {");
      out.println("        Function<Node, List<Node>> collector = COLLECTORS.get(nodeType);");
      out.println("            if (collector == null) {");
      out.println(
          "                throw new IllegalArgumentException(\"Node type \" + nodeType + \" "
              + "not supported\");");
      out.println("            }");
      out.println("        return collector.apply(node);");
      out.println("    }");
      out.println("}");
    }
  }
}