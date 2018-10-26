package proj6AbulhabFengMaoSavillo;

import org.antlr.runtime.*;
import org.antlr.v4.runtime.misc.NotNull;
import org.stringtemplate.v4.compiler.Bytecode;

import java.lang.reflect.Method;
import java.util.Collection;

import java.util.ArrayList;


public class JavaListener implements Parser {

    @Override
    public Class parse(String code) {
        CharStream charStream = new ANTLRInputStream(code);
        Java8Lexer lexer = new Java8Lexer(charStream);
        TokenStream tokens = new CommonTokenStream(lexer);
        Java8Parser parser = new Java8Parser(tokens);

        ClassListener classListener = new ClassListener();
        parser.classDeclaration().enterRule(classListener);
        return classListener.getParsedClass();
    }

    class ClassListener extends Java8BaseListener {

        private Class parsedClass;

        @Override
        public void enterClassDeclaration(@NotNull Java8Parser.ClassDeclarationContext ctx) {
            String className = ctx.className().getText();
            MethodListener methodListener = new MethodListener();
            ctx.method().forEach(method -> method.enterRule(methodListener));
            Collection<Method> methods = methodListener.getMethods();
            parsedClass = new Class(className,methods);
        }

        public Class getParsedClass() {
            return parsedClass;
        }
    }

    class MethodListener extends Java8BaseListener {

        private Collection<Method> methods;

        public MethodListener() {
            methods = new ArrayList<>();
        }

        @Override
        public void enterMethod(@NotNull Java8Parser.MethodContext ctx) {
            String methodName = ctx.methodName().getText();
            InstructionListener instructionListener = new InstructionListener();
            ctx.instruction().forEach(instruction -> instruction.enterRule(instructionListener));
            Collection<Bytecode.Instruction> instructions = instructionListener.getInstructions();
            methods.add(new Method(methodName, instructions));
        }

        public Collection<Method> getMethods() {
            return methods;
        }
    }

    class InstructionListener extends Java8BaseListener {

        private Collection<Instruction> instructions;

        public InstructionListener() {
            instructions = new ArrayList<>();
        }

        @Override
        public void enterInstruction(@NotNull Java8Parser.InstructionContext ctx) {
            String instructionName = ctx.getText();
            instructions.add(new Instruction(instructionName));
        }

        public Collection<Bytecode.Instruction> getInstructions() {
            return instructions;
        }
    }
}
