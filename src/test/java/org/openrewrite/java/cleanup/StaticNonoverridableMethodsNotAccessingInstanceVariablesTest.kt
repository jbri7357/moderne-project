import org.junit.jupiter.api.Test
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.cleanup.StaticNonoverridableMethodsNotAccessingInstanceVariables
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
    fun privateMethodAccessingStaticFieldInOuterClassIsMadeStatic() = rewriteRun(
        java(
            """
            class OuterUtilities {
                private static String myString = "My string";
                
                class InnerUtilities {
                    private String getMyString() {
                        return myString;
                    }
                }
            }
        """,
            """
            class OuterUtilities {
                private static String myString = "My string";
                
                class InnerUtilities {
                    private static String getMyString() {
                        return myString;
                    }
                }
            }
        """
        )
    )

    @Test
    fun privateMethodAccessingInstanceFieldInOuterClassIsNotMadeStatic() = rewriteRun(
        java(
            """
            class OuterUtilities {
                private String myString = "My string";
                
                class InnerUtilities {
                    private String getMyString() {
                        return myString;
                    }
                }
            }
        """,
            """
            class OuterUtilities {
                private String myString = "My string";
                
                class InnerUtilities {
                    private String getMyString() {
                        return myString;
                    }
                }
            }
        """
        )
    )
}