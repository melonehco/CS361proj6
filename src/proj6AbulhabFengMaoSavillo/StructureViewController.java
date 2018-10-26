/*
File: Controller.java
CS361 Project 6
Names: Melody Mao, Zena Abulhab, Yi Feng, and Evan Savillo
Date: 10/27/2018
*/

package proj6AbulhabFengMaoSavillo;


import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import proj6AbulhabFengMaoSavillo.Java8Parser.ResultContext;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.LexerInterpreter;
import org.antlr.v4.runtime.ParserInterpreter;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.tool.Grammar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Controller that manages the generation and display of the structure of the
 * java code in the file currently being viewed.
 */
public class StructureViewController
{
    //private Map<File, Map<TreeItem, Integer>> //thinking about how to store line numbers
    private TreeView<String> treeView;
    /** a HashMap mapping the tabs and the associated files */
    private Map<File, TreeItem<String>> fileToCodeStructMap = new HashMap<>();
    private ParseTreeWalker walker;

    public StructureViewController()
    {
        this.walker = new ParseTreeWalker();
    }

    /**
     * Takes in the fxml item treeView from main Controller.
     *
     * @param treeView TreeView item representing structure display
     */
    public void setTreeView(TreeView treeView)
    {
        this.treeView = treeView;
    }

    /**
     * Adds a TreeItem<String> to the map, meaning the program has the relevant file open.
     *
     * @param file file which was parsed to generate TreeItem<String>
     * @param root root node which defines the TreeItem<String>
     */
    private void addStructure(File file, TreeItem<String> root)
    {
        this.fileToCodeStructMap.put(file, root);
    }

    /**
     * Removes a TreeItem<String> to the map, meaning the program has closed the relevant file.
     *
     * @param file file which was parsed to generate TreeItem<String>
     * @param root root node which defines the TreeItem<String>
     */
    private void removeStructure(File file, TreeItem<String> root)
    {
        this.fileToCodeStructMap.remove(file, root);
    }

    /**
     * Parses a file thereby storing contents as TreeItems in our special tree.
     *
     * @param //file the file to be parsed
     */
    public void generateStructureTree(String fileContents)
    {
        TreeItem<String> newRoot = new TreeItem<>(fileContents);

        //build lexer, parser, and parse tree for the given file
        Java8Lexer lexer = new Java8Lexer(CharStreams.fromString(fileContents));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Java8Parser parser = new Java8Parser(tokens);
        ParseTree tree = parser.compilationUnit();

        //walk through parse tree with listening for code structure elements
        CodeStructureListener codeStructureListener = new CodeStructureListener(newRoot);
        this.walker.walk(codeStructureListener, tree);

        this.setRootNode(newRoot);
    }

    /**
     * Sets the currently displaying File TreeItem<String> View.
     *
     * @param root root node corresponding to currently displaying file
     */
    private void setRootNode(TreeItem<String> root)
    {
        this.treeView.setRoot(root);
        this.treeView.setShowRoot(false);
    }


    /**
     * Private helper class that listens for code structure declarations
     * (classes, fields, methods) during a parse tree walk and builds a
     * TreeView subtree representing the code structure.
     */
    private static class CodeStructureListener extends Java8BaseListener
    {
        private TreeItem<String> currentNode;

        /**
         * creates a new CodeStructureListener that builds a subtree
         * from the given root TreeItem
         *
         * @param root root TreeItem to build subtree from
         */
        public CodeStructureListener(TreeItem<String> root)
        {
            this.currentNode = root;
        }

        /**
         * starts a new subtree for the class declaration entered
         */
        @Override
        public void enterNormalClassDeclaration(Java8Parser.NormalClassDeclarationContext ctx)
        {
            TerminalNode node = ctx.Identifier();
            String className = node.getText();

            TreeItem<String> newNode = new TreeItem<String>("[class] " + className);
            this.currentNode.getChildren().add(newNode);
            this.currentNode = newNode; //move current node into new subtree
        }

        /**
         * ends the new subtree for the class declaration exited,
         * returns traversal to parent node
         */
        @Override
        public void exitNormalClassDeclaration(Java8Parser.NormalClassDeclarationContext ctx)
        {

            this.currentNode = this.currentNode.getParent(); //move current node back to parent
        }

        /**
         * adds a child node for the field entered under the TreeItem for the current class
         */
        @Override
        public void enterFieldDeclaration(Java8Parser.FieldDeclarationContext ctx)
        {
            //get field name
            TerminalNode node = ctx.variableDeclaratorList().variableDeclarator(0).variableDeclaratorId().Identifier();
            String fieldName = node.getText();

            //add field to TreeView under the current class tree
            TreeItem<String> newNode = new TreeItem<String>("[field] " + fieldName);
            this.currentNode.getChildren().add(newNode);
        }

        /**
         * adds a child node for the method entered under the TreeItem for the current class
         */
        @Override
        public void enterMethodHeader(Java8Parser.MethodHeaderContext ctx)
        {
            //get method name
            TerminalNode nameNode = ctx.methodDeclarator().Identifier();
            String methodName = nameNode.getText();

            //add method to TreeView under the current class tree
            TreeItem<String> newNode = new TreeItem<String>("[method] " + methodName);
            this.currentNode.getChildren().add(newNode);
        }
    }
}

/**
 * 1. pass over file:
 * -get top level declarations
 * -get all top-level bodies
 * 2. pass over all top-level bodies
 * -get all top level declarations
 * -get all top level bodies
 * 3. pass over all top-level bodies
 * ...etc
 * -get all methods and fields
 * <p>
 * <p>
 * getConsituents(body)
 * <p>
 * return [[methods/fields], getConstituents(top-level entity bodies)]
 */


