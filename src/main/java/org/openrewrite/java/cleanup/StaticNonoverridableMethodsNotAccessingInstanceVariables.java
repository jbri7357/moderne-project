package org.openrewrite.java.cleanup;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.stream.Collectors;

public class StaticNonoverridableMethodsNotAccessingInstanceVariables extends Recipe {

    @Override
    public String getDisplayName() {
        return "Non-overridable methods not accessing instance variables should be static";
    }

    @Override
    public String getDescription() {
        return "Change private and final (non-overridable) methods not accessing instance variables to static.";
    }

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {


            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDeclaration, ExecutionContext executionContext) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDeclaration, executionContext);

                final Set<String> instanceVariablesSignatures = classDeclaration.getBody().getStatements().stream()
                        .filter(J.VariableDeclarations.class::isInstance)
                        .map(J.VariableDeclarations.class::cast)
                        .filter(v -> !v.hasModifier(J.Modifier.Type.Static))
                        .flatMap(vd -> vd.getVariables().stream())
                        .map(J.VariableDeclarations.NamedVariable::getVariableType)
                        .filter(Objects::nonNull)
                        .map(JavaType.Variable::toString)
                        .collect(Collectors.toSet());

                Set<String> methodsUsingInstanceVariables = FindMethodsUsingInstanceVariables.find(instanceVariablesSignatures, cd);

                doAfterVisit(new SetNonoverridableMethodsToStaticVisitor(methodsUsingInstanceVariables));

                return cd;
            }
        };
    }

    private static class FindMethodsUsingInstanceVariables {
        public static Set<String> find(Set<String> instanceVariableSignatures, J.ClassDeclaration parentClass) {
            Set<String> methodsFoundIn = new HashSet<>();

            new JavaIsoVisitor<Set<String>>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> methodsFoundIn) {
                    if (identifier.getFieldType() != null && instanceVariableSignatures.contains(identifier.getFieldType().toString())) {
                        Cursor parent = getCursor().dropParentUntil(is -> is instanceof J.MethodDeclaration || is instanceof J.VariableDeclarations || is instanceof J.ClassDeclaration);
                        if (parent.getValue() instanceof J.MethodDeclaration) {
                            methodsFoundIn.add(parent.getValue().toString());
                        }
                    }
                    return super.visitIdentifier(identifier, methodsFoundIn);
                }
            }.visit(parentClass, methodsFoundIn);
            return methodsFoundIn;
        }
    }


    private static class SetNonoverridableMethodsToStaticVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final Set<String> methodsUsingInstanceVariables;

        SetNonoverridableMethodsToStaticVisitor(Set<String> methodsUsingInstanceVariables) {
            this.methodsUsingInstanceVariables = methodsUsingInstanceVariables;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration methodDeclaration, ExecutionContext executionContext) {
            J.MethodDeclaration md = super.visitMethodDeclaration(methodDeclaration, executionContext);

            boolean isNonOverridable = (md.hasModifier(J.Modifier.Type.Private) || md.hasModifier(J.Modifier.Type.Final));

            if (!methodsUsingInstanceVariables.contains(md.toString()) && isNonOverridable && !md.hasModifier((J.Modifier.Type.Static))) {
                J.Modifier staticModifier = new J.Modifier(Tree.randomId(), Space.build(" ", Collections.emptyList()), Markers.EMPTY, J.Modifier.Type.Static, Collections.emptyList());
                md = md.withModifiers(ModifierOrder.sortModifiers(ListUtils.concat(staticModifier, md.getModifiers())));
                if (getCursor().getParent() != null) {
                    md = autoFormat(md, md, executionContext, getCursor().getParent());
                }
            }
            return md;
        }
    }


}