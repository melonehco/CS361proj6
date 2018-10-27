/*
File: Controller.java
CS361 Project 6
Names: Melody Mao, Zena Abulhab, Yi Feng, and Evan Savillo
Date: 10/27/2018
*/

package proj6AbulhabFengMaoSavillo;


import javafx.event.Event;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.io.FileInputStream;
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
    private Map<TreeItem, Integer> treeItemLineNumMap;
    private TreeView<String> treeView;
    /** a HashMap mapping the tabs and the associated files */
    private Map<File, TreeItem<String>> fileToCodeStructMap = new HashMap<>();
    private ParseTreeWalker walker;


    public StructureViewController()
    {
        this.walker = new ParseTreeWalker();
        this.treeItemLineNumMap = new HashMap<>();
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
        CodeStructureListener codeStructureListener = new CodeStructureListener(newRoot, this.treeItemLineNumMap);
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

    public void handleTreeItemClicked(Event event) {
        TreeItem selectedTreeItem = this.treeView.getSelectionModel().getSelectedItem();
        System.out.println(this.treeItemLineNumMap.get(selectedTreeItem));
    }


    /**
     * Private helper class that listens for code structure declarations
     * (classes, fields, methods) during a parse tree walk and builds a
     * TreeView subtree representing the code structure.
     */
    private class CodeStructureListener extends Java8BaseListener
    {
        Image classPic;
        Image methodPic;
        Image fieldPic;
        private TreeItem<String> currentNode;
        private Map<TreeItem, Integer> treeItemIntegerMap;

        /**
         * creates a new CodeStructureListener that builds a subtree
         * from the given root TreeItem
         *
         * @param root root TreeItem to build subtree from
         */
        public CodeStructureListener(TreeItem<String> root, Map<TreeItem, Integer> treeItemIntegerMap)
        {
            this.currentNode = root;
            this.treeItemIntegerMap = treeItemIntegerMap;


            try
            {
                this.classPic = new Image(new FileInputStream(System.getProperty("user.dir") + "/include/c.png"));
                this.methodPic = new Image(new FileInputStream(System.getProperty("user.dir") + "/include/m.png"));
                this.fieldPic = new Image(new FileInputStream(System.getProperty("user.dir") + "/include/f.png"));
            }
            catch (IOException e)
            {
                System.out.println("Error Loading Images");
            }
        }

        /**
         * starts a new subtree for the class declaration entered
         */
        @Override
        public void enterNormalClassDeclaration(Java8Parser.NormalClassDeclarationContext ctx)
        {
            TerminalNode node = ctx.Identifier();
            String className = node.getText();

            Token t = ctx.start;
            int lineNumber = t.getLine();

            TreeItem<String> newNode = new TreeItem<>(className);
            newNode.setGraphic(new ImageView(this.classPic));
            newNode.setExpanded(true);
            this.currentNode.getChildren().add(newNode);
            this.currentNode = newNode; //move current node into new subtree
            this.treeItemIntegerMap.put(newNode, ctx.getStart().getLine());

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

            Token t = ctx.start;
            int lineNumber = t.getLine();

            //add field to TreeView under the current class tree
            TreeItem<String> newNode = new TreeItem<>(fieldName);
            newNode.setGraphic(new ImageView(this.fieldPic));
            this.currentNode.getChildren().add(newNode);
            this.treeItemIntegerMap.put(newNode, ctx.getStart().getLine());
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

            Token t = ctx.start;
            int lineNumber = t.getLine();

            //add method to TreeView under the current class tree
            TreeItem<String> newNode = new TreeItem<>(methodName);
            newNode.setGraphic(new ImageView(this.methodPic));
            this.currentNode.getChildren().add(newNode);
            this.treeItemIntegerMap.put(newNode, ctx.getStart().getLine());

        }

        public Map<TreeItem, Integer> getTreeItemIntegerMap() {
            return this.treeItemIntegerMap;
        }
    }
}