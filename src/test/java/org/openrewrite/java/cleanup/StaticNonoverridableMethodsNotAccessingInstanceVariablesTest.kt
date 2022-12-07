package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.java.Assertions.java
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class StaticNonoverridableMethodsNotAccessingInstanceVariablesTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(StaticNonoverridableMethodsNotAccessingInstanceVariables())
    }

    @Test
    fun privateMethodAccessingStaticFieldIsMadeStatic() = rewriteRun(
        java(
            """
            class Utilities {
                private static String myString = "My string";
            
                private String getMyString() {
                    return myString;
                }
            }
        """,
            """
            class Utilities {
                private static String myString = "My string";
            
                private static String getMyString() {
                    return myString;
                }
            }
        """
        )
    )

    @Test
    fun privateMethodAccessingNoFieldsIsMadeStatic() = rewriteRun(
        java(
            """
            class Utilities {
                private String getMyString() {
                    return "My string";
                }
            }
        """,
            """
            class Utilities {
                private static String getMyString() {
                    return "My string";
                }
            }
        """
        )
    )

    @Test
    fun privateMethodAccessingInstanceFieldIsNotMadeStatic() = rewriteRun(
        java(
            """
            class Utilities {
                private String myString = "My string";
                
                private String getMyString() {
                    return myString;
                }
            }
        """)
    )

    // It's debatable whether we should transform the method into "public static final" or simply "public static" since static methods are final by nature
    @Test
    fun finalMethodAccessingStaticFieldIsMadeStatic() = rewriteRun(
        java(
            """
            class Utilities {
                private static String myString = "My string";
            
                final public String getMyString() {
                    return myString;
                }
            }
        """,
            """
            class Utilities {
                private static String myString = "My string";
            
                public static final String getMyString() {
                    return myString;
                }
            }
        """
        )
    )
    // It's debatable whether we should transform the method into "public static final" or simply "public static" since static methods are final by nature
    @Test
    fun finalMethodAccessingNoFieldsIsMadeStatic() = rewriteRun(
        java(
            """
            class Utilities {
                public final String getMyString() {
                    return "My string";
                }
            }
        """,
            """
            class Utilities {
                public static final String getMyString() {
                    return "My string";
                }
            }
        """
        )
    )

    @Test
    fun finalMethodAccessingInstanceFieldIsNotMadeStatic() = rewriteRun(
        java(
            """
            class Utilities {
                private String myString = "My string";
                
                final public String getMyString() {
                    return myString;
                }
            }
        """
        )
    )

    @Test
    fun overridableMethodAccessingStaticFieldIsNotMadeStatic() = rewriteRun(
        java(
            """
            class Utilities {
                private static String myString = "My string";
                
                public String getMyString() {
                    return myString;
                }
            }
        """
        )
    )

    @Test
    fun overridableMethodAccessingNoFieldIsNotMadeStatic() = rewriteRun(
        java(
            """
            class Utilities {
                public String getMyString() {
                    return "My string";
                }
            }
        """
        )
    )

    @Test
    fun overridableMethodAccessingInstanceFieldIsNotMadeStatic() = rewriteRun(
        java(
            """
            class Utilities {
                private String myString = "My string";
                
                public String getMyString() {
                    return myString;
                }
            }
        """
        )
    )

    @Test
    fun privateMethodAccessingStaticFieldInLambdaIsMadeStatic() = rewriteRun(
        java(
            """
            class Utilities {
                private static String myString = "My string";
            
                interface MyInterface {
                    String grabMyString();
                }
            
                private String getMyString() {
                    MyInterface msg = () -> myString;
                    return msg.grabMyString();
                }
            }
        """,
            """
            class Utilities {
                private static String myString = "My string";
            
                interface MyInterface {
                    String grabMyString();
                }
            
                private static String getMyString() {
                    MyInterface msg = () -> myString;
                    return msg.grabMyString();
                }
            }
        """
        )
    )

    @Test
    fun privateMethodAccessingInstanceFieldUsingThisKeywordIsNotMadeStatic() = rewriteRun(
        java(
            """
            class Utilities {
                private String myString = "My string";
            
                private String getMyString() {
                    return this.myString;
                }
            }
        """
        )
    )

    @Test
    fun privateOverloadedMethodAccessingStaticFieldIsMadeStatic() = rewriteRun(
        java(
            """
            class Utilities {
                private static String myString = "My string";
                private String myOtherString = "My  other string";
            
                private String getMyString(String stringPrefix) {
                    return stringPrefix + myString;
                }
                
                private String getMyString(Integer integerPrefix) {
                    return integerPrefix + myOtherString;
                }
            }
        """,
            """
            class Utilities {
                private static String myString = "My string";
                private String myOtherString = "My  other string";
            
                private static String getMyString(String stringPrefix) {
                    return stringPrefix + myString;
                }
                
                private String getMyString(Integer integerPrefix) {
                    return integerPrefix + myOtherString;
                }
            }
        """
        )
    )

    // Abstract methods cannot be private or final, so we won't be making them static
    @Test
    fun abstractMethodIsNotMadeStatic() = rewriteRun(
        java(
            """
            class Utilities {
                abstract public String getMyString();
                abstract protected String getMyOtherString();
            }
        """)
    )

    // We can have static and default methods in interfaces since Java 8+.
    // Interface methods can have static, default, or abstract modifiers.
    // They also can't be final or private, so static will not be applied
    // based on how our code currently works.
    @Test
    fun interfaceMethodsAreNotMadeStatic() = rewriteRun(
        java(
            """
            interface Utilities {
                String getMyString();
                
                static String getMyOtherString() {
                    return "other string";
                }
                
                default String getMyOtherOtherString() {
                    return "other other string";
                }
            }
        """)
    )

    //Static declarations in inner classes are not supported until Java 16+, so we will skip applying static
    @Test
    fun methodInInnerClassIsNotMadeStatic() = rewriteRun(
        java(
            """
            class OuterUtilities {
                class InnerUtilities {
                    private static String myString = "My string";
                    
                    private String getMyString() {
                        return myString;
                    }
                }
            }
        """
        )
    )

    @Test
    fun multipleClassesWithPrivateMethodAccessingMixedFields() = rewriteRun(
        java(
            """
            class Utilities1 {
                private String myString = "My string";
            
                private String getMyString() {
                    return myString;
                }
            }
            
            class Utilities2 {
                private static String myString = "My string";
            
                private String getMyString() {
                    return myString;
                }
            }
        """,
            """
            class Utilities1 {
                private String myString = "My string";
            
                private String getMyString() {
                    return myString;
                }
            }
            
            class Utilities2 {
                private static String myString = "My string";
            
                private static String getMyString() {
                    return myString;
                }
            }
        """
        )
    )

    @Test
    fun multipleClassesWithMethodsAccessingStaticFieldsAcrossClasses() = rewriteRun(
        java(
            """
            class Utilities1 {
                private static String myString = "My string";
            
                private String getMyString() {
                    return Utilities2.myString;
                }
            }
            
            class Utilities2 {
                private static String myString = "My string";
            
                private String getMyString() {
                    return Utilities1.myString;
                }
            }
        """,
            """
            class Utilities1 {
                private static String myString = "My string";
            
                private static String getMyString() {
                    return Utilities2.myString;
                }
            }
            
            class Utilities2 {
                private static String myString = "My string";
            
                private static String getMyString() {
                    return Utilities1.myString;
                }
            }
        """
        )
    )
}