/*
File: Controller.java
CS361 Project 6
Names: Melody Mao, Zena Abulhab, Yi Feng, and Evan Savillo
Date: 10/27/2018
*/

package proj6AbulhabFengMaoSavillo;


import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Controller that manages the generation and display of the structure of the
 * java code in the file currently being viewed.
 */
public class StructureViewController
{
    //TODO: add listeners to add/remove structures on tab opens/closes
    //TODO: figure out how we're actually parsing files
    /*
        - Behavior: update Tree when
            1. The view tab is set to open by user
            2. The current file has changed (is red) or only whenever saved?
    */
    private TreeView<String> treeView;
    /** a HashMap mapping the tabs and the associated files */
    private Map<File, CodeStructureTree> fileToCodeStructMap = new HashMap<>();

    /**
     * Takes in the fxml item treeView from main Controller.
     *
     * @param treeView TreeView item representing structure display
     */
    public void setTreeView(TreeView treeView)
    {
        this.treeView = treeView;

        //for testing, should be removed
        this.treeView.setRoot(new CodeStructureTree().getRoot());
        //this.treeView.setShowRoot(false);
    }

    /**
     * Sets the currently displaying File CodeStructureTree View.
     *
     * @param root root node corresponding to currently displaying file
     */
    private void setRootNode(CodeStructureTree root)
    {
        this.treeView.setRoot(root.getRoot());
    }

    /**
     * Adds a CodeStructureTree to the map, meaning the program has the relevant file open.
     *
     * @param file file which was parsed to generate CodeStructureTree
     * @param root root node which defines the CodeStructureTree
     */
    private void addStructure(File file, CodeStructureTree root)
    {
        this.fileToCodeStructMap.put(file, root);
    }

    /**
     * Removes a CodeStructureTree to the map, meaning the program has closed the relevant file.
     *
     * @param file file which was parsed to generate CodeStructureTree
     * @param root root node which defines the CodeStructureTree
     */
    private void removeStructure(File file, CodeStructureTree root)
    {
        this.fileToCodeStructMap.remove(file, root);
    }

    /**
     * Parses a file thereby storing contents as TreeItems in our special tree.
     *
     * @param file the file to be parsed
     */
    private CodeStructureTree generateStructureTree(File file)
    {
        CodeStructureTree newTree = new CodeStructureTree();

        //TODO: Do we want to hand file to the class or parsed output?
        //1. Read entire file into string
        //2. Recursively Parse String
        String methodRegex = "((public|private|protected|static|final|native|synchronized|abstract|transient)+\\s)+[\\$_\\w\\<\\>\\[\\]]*\\s+[\\$_\\w]+\\([^\\)]*\\)?\\s*\\{?[^\\}]*\\}?";
        Pattern p = Pattern.compile(methodRegex);


        return newTree;
    }


    /**
     * Private helper class used to make and store parsed java file structures.
     * Considered a tree more conceptually than in function.
     */
    private class CodeStructureTree
    {
        private TreeItem<String> root;

        CodeStructureTree()//File file)
        {
            //this.root = new TreeItem<>(file.getName());
            this.root = new TreeItem<>("Root Item");
            this.root.getChildren().addAll(this.getFileItems());
            this.root.setExpanded(true);
        }

        /**
         * Tier 1 items include names of
         * Classes, interfaces
         *
         * @return fileItems
         */
        private ArrayList<TreeItem<String>> getFileItems()
        {
            ArrayList<TreeItem<String>> fileItems = new ArrayList<>();

            TreeItem<String> fitem1 = new TreeItem<>("Class Lorem");
            fitem1.getChildren().addAll(getClassItems(fitem1.getValue()));
            fileItems.add(fitem1);
            fitem1.setExpanded(true);

            return fileItems;
        }

        /**
         * Tier 2 items listed under each Class include names of
         * Classes, class items.
         * <p>
         * Class items are of the form,
         *
         * @return
         */
        private ArrayList<TreeItem<String>> getClassItems(String fileItem)
        {
            ArrayList<TreeItem<String>> classItems = new ArrayList<>();

            classItems.add(new TreeItem<>("[Method] Ipsum(): void"));
            classItems.add(new TreeItem<>("[Method] Dolor(): void"));
            classItems.add(new TreeItem<>("[Field] Sit(): Amet"));

            return classItems;
        }

        private TreeItem<String> getRoot()
        {
            return this.root;
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
