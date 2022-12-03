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
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.lang.reflect.Array;
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

                final Set<J.VariableDeclarations> allClassVariableDeclarations =  cd.getBody().getStatements().stream().filter(s -> s instanceof J.VariableDeclarations).map(s -> (J.VariableDeclarations) s).collect(Collectors.toSet());
                final Set<J.VariableDeclarations> instanceVariableDeclarations = allClassVariableDeclarations.stream().filter(vd -> !vd.hasModifier(J.Modifier.Type.Static)).collect(Collectors.toSet());
                Set<J.MethodDeclaration> methodsToMakeStatic = instanceVariableDeclarations.stream().map(vd -> InstanceVariablesUsage.find(vd, cd)).flatMap(Set::stream).collect(Collectors.toSet());



                return cd;
            }


            /*
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration methodDeclaration, ExecutionContext executionContext) {
                J.MethodDeclaration md = super.visitMethodDeclaration(methodDeclaration, executionContext);
                List<J.VariableDeclarations> instanceVariables = new ArrayList<>();


                J.ClassDeclaration parentClassDeclaration = getCursor().getParentOrThrow().firstEnclosing(J.ClassDeclaration.class);
                if (parentClassDeclaration != null) {
                    for (Statement statement : parentClassDeclaration.getBody().getStatements()) {
                        if (statement instanceof J.VariableDeclarations) {
                            J.VariableDeclarations vd = (J.VariableDeclarations) statement;
                            if (!vd.hasModifier(J.Modifier.Type.Static)) {
                                instanceVariables.add(vd);
                            }
                        }
                    }

                    boolean isNonOverridable = (md.hasModifier(J.Modifier.Type.Private) || md.hasModifier(J.Modifier.Type.Final));
                    boolean isAccessingInstanceField = true; // TODO Determine if method is accessing fields without static modifier or not access fields at all

                   J.Block methodBody = md.getBody();
                   if(methodBody != null) {
                       for (Statement statement : methodBody.getStatements()) {
                            System.out.println(statement.toString());
                       }
                   }


                    if (isNonOverridable && isAccessingInstanceField) {
                        J.Modifier staticModifier = new J.Modifier(Tree.randomId(), Space.build(" ", Collections.emptyList()), Markers.EMPTY, J.Modifier.Type.Static, Collections.emptyList());
                        md = md.withModifiers(ListUtils.concat(staticModifier, md.getModifiers()));
                    }
                }

                return md;
            }
            */

        };
    }

    private static class InstanceVariablesUsage {
        public static Set<J.MethodDeclaration> find(J.VariableDeclarations declarations, J.ClassDeclaration parentClass) {
            Set<J.MethodDeclaration> methodsFoundIn = new HashSet<>();
            Set<String> variableTypeSignatures = declarations.getVariables().stream().map(J.VariableDeclarations.NamedVariable::getVariableType).filter(Objects::nonNull).map(JavaType.Variable::toString).collect(Collectors.toSet());


            JavaIsoVisitor<Set<J.MethodDeclaration>> visitor =
                    new JavaIsoVisitor<Set<J.MethodDeclaration>>() {

                        @Override
                        public J.Identifier visitIdentifier(J.Identifier identifier,
                                                            Set<J.MethodDeclaration> methodsFoundIn) {
                            if (identifier.getFieldType() != null && variableTypeSignatures.contains(identifier.getFieldType().toString())) {
                                Cursor parent = getCursor().dropParentUntil(is -> is instanceof J.MethodDeclaration || is instanceof J.VariableDeclarations || is instanceof J.ClassDeclaration);
                                if(parent.getValue() instanceof J.MethodDeclaration) {
                                    methodsFoundIn.add(parent.getValue());
                                }
                            }
                            return super.visitIdentifier(identifier, methodsFoundIn);
                        }
                    };

            visitor.visit(parentClass, methodsFoundIn);
            return methodsFoundIn;
        }
    }

}