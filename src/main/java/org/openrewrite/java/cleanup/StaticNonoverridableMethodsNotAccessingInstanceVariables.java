package org.openrewrite.java.cleanup;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

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
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration methodDeclaration, ExecutionContext executionContext) {

                J.MethodDeclaration md = super.visitMethodDeclaration(methodDeclaration, executionContext);

                J.ClassDeclaration parentClassDeclaration = getCursor().firstEnclosing(J.ClassDeclaration.class);
                J.Block methodBody = md.getBody();

                if (parentClassDeclaration != null && methodBody != null) {
                    final List<J.VariableDeclarations> allClassVariableDeclarations = parentClassDeclaration.getBody().getStatements().stream()
                            .filter(J.VariableDeclarations.class::isInstance)
                            .map(J.VariableDeclarations.class::cast)
                            .collect(Collectors.toList());

                    final List<J.VariableDeclarations.NamedVariable> instanceVariables = allClassVariableDeclarations.stream()
                            .filter(v -> !v.hasModifier(J.Modifier.Type.Static))
                            .flatMap(vd -> vd.getVariables().stream())
                            .collect(Collectors.toList());


                    Set<String> variableTypeSignatures = instanceVariables.stream()
                            .map(J.VariableDeclarations.NamedVariable::getVariableType)
                            .filter(Objects::nonNull)
                            .map(JavaType.Variable::toString)
                            .collect(Collectors.toSet());



/*
                    boolean isNonOverridable = (md.hasModifier(J.Modifier.Type.Private) || md.hasModifier(J.Modifier.Type.Final));
                    boolean isAccessingInstanceField = true; // TODO Determine if method is accessing fields without static modifier or not access fields at all




                    if (isNonOverridable && isAccessingInstanceField) {
                        J.Modifier staticModifier = new J.Modifier(Tree.randomId(), Space.build(" ", Collections.emptyList()), Markers.EMPTY, J.Modifier.Type.Static, Collections.emptyList());
                        md = md.withModifiers(ListUtils.concat(staticModifier, md.getModifiers()));
                    }

 */
                }

                return md;
            }

            List<>


        };
    }

}