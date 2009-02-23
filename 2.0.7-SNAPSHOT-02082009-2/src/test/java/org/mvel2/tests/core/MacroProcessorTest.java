package org.mvel2.tests.core;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.Assert;
import org.mvel2.tests.core.res.Foo;
import org.mvel2.integration.Interceptor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.ast.ASTNode;
import org.mvel2.ast.WithNode;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.compiler.CompiledExpression;
import static org.mvel2.MVEL.parseMacros;
import static org.mvel2.MVEL.executeExpression;
import org.mvel2.debug.Debugger;
import org.mvel2.debug.Frame;
import org.mvel2.debug.DebugTools;
import org.mvel2.*;

public class MacroProcessorTest extends TestCase {

    private MacroProcessor macroProcessor;

    protected void setUp() throws Exception {
        super.setUp();
        Map<String, Macro> macros = new HashMap<String, Macro>();
        macros.put("insert",
                new Macro() {
                    public String doMacro() {
                        return "drools.insert";
                    }
                });
        macroProcessor = new MacroProcessor();
        macroProcessor.setMacros(macros);
    }

    public void testParseString() {
        String raw = "    l.add( \"rule 2 executed \" + str);";
        try {
            String result = macroProcessor.parse(raw);
            assertEquals(raw, result);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail("there shouldn't be any exception: " + ex.getMessage());
        }
    }

    public void testParseConsequenceWithComments() {
        String raw = "    // str is null, we are just testing we don't get a null pointer \n " +
                "    list.add( p );";
        try {
            String result = macroProcessor.parse(raw);
            assertEquals(raw, result);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail("there shouldn't be any exception: " + ex.getMessage());
        }
    }

    public void testMacroSupport() {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("foo", new Foo());

        Map<String, Interceptor> interceptors = new HashMap<String, Interceptor>();
        Map<String, Macro> macros = new HashMap<String, Macro>();

        interceptors.put("Modify", new Interceptor() {
            public int doBefore(ASTNode node, VariableResolverFactory factory) {
                ((WithNode) node).getNestedStatement().getValue(null,
                        factory);
                factory.createVariable("mod", "FOOBAR!");
                return 0;
            }

            public int doAfter(Object val, ASTNode node, VariableResolverFactory factory) {
                return 0;
            }
        });

        macros.put("modify", new Macro() {
            public String doMacro() {
                return "@Modify with";
            }
        });

        ExpressionCompiler compiler = new ExpressionCompiler(parseMacros("modify (foo) { aValue = 'poo' }; mod", macros));
        //   compiler.setDebugSymbols(true);

        ParserContext ctx = new ParserContext(null, interceptors, null);
        ctx.setSourceFile("test.mv");
        ctx.setDebugSymbols(true);

        //   CompiledExpression compiled = compiler.compile(ctx);

        assertEquals("FOOBAR!", executeExpression(compiler.compile(ctx), null, vars));
    }


    public void testMacroSupportWithStrings() {
        Map<String, Object> vars = new HashMap<String, Object>();
        Foo foo = new Foo();
        vars.put("foo", foo);

        Map<String, Macro> macros = new HashMap<String, Macro>();

        macros.put("modify", new Macro() {
            public String doMacro() {
                return "drools.modify";
            }
        });

        assertEquals("", foo.aValue);

        ExpressionCompiler compiler = new ExpressionCompiler(parseMacros("\"This is an modify()\"", macros));

        ParserContext ctx = new ParserContext(null, null, null);
        ctx.setSourceFile("test.mv");
        ctx.setDebugSymbols(true);

        assertEquals("This is an modify()", executeExpression(compiler.compile(ctx), null, vars));
    }


    public void testMacroSupportWithDebugging() {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("foo", new Foo());

        Map<String, Interceptor> interceptors = new HashMap<String, Interceptor>();
        Map<String, Macro> macros = new HashMap<String, Macro>();

        interceptors.put("Modify", new Interceptor() {
            public int doBefore(ASTNode node, VariableResolverFactory factory) {
                ((WithNode) node).getNestedStatement().getValue(null,
                        factory);

                factory.createVariable("mod", "FOOBAR!");


                return 0;
            }

            public int doAfter(Object val, ASTNode node, VariableResolverFactory factory) {
                return 0;
            }
        });

        macros.put("modify", new Macro() {
            public String doMacro() {
                return "@Modify with";
            }
        });

        ExpressionCompiler compiler = new ExpressionCompiler(
                parseMacros(
                        "System.out.println('hello');\n" +
                                "System.out.println('bye');\n" +
                                "modify (foo) { aValue = 'poo', \n" +
                                " aValue = 'poo' };\n mod", macros)
        );
        // compiler.setDebugSymbols(true);

        ParserContext ctx = new ParserContext(null, interceptors, null);
        ctx.setSourceFile("test.mv");
        ctx.setDebugSymbols(true);

        CompiledExpression compiled = compiler.compile(ctx);

        MVELRuntime.setThreadDebugger(new Debugger() {

            public int onBreak(Frame frame) {
                System.out.println(frame.getSourceName() + ":" + frame.getLineNumber());

                return Debugger.STEP;
            }
        });

        MVELRuntime.registerBreakpoint("test.mv", 3);

        System.out.println(DebugTools.decompile(compiled
        ));

        Assert.assertEquals("FOOBAR!", MVEL.executeDebugger(compiled, null, new MapVariableResolverFactory(vars)));
    }

    public void testParseStringUnmatchedChars() {
        String raw = "result.add( \"\\\"\\\' there are } [ unmatched characters in this string (\"  );";
        try {
            String result = macroProcessor.parse(raw);
            assertEquals(raw, result);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail("there shouldn't be any exception: " + ex.getMessage());
        }
    }
}