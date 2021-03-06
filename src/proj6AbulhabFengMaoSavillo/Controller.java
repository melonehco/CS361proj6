/*
 * File: Controller.java
 * F18 CS361 Project 6
 * Names: Melody Mao, Zena Abulhab, Yi Feng, Evan Savillo
 * Date: 10/27/2018
 * This file contains the Main controller class, handling actions evoked by the Main window.
 */

package proj6AbulhabFengMaoSavillo;

import javafx.beans.property.*;
import javafx.event.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;

import java.io.File;
import java.util.*;

import javafx.scene.input.MouseEvent;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import javafx.scene.input.KeyCode;


/**
 * Main controller handles actions evoked by the Main window.
 *
 * @author Zena Abulhab
 * @author Yi Feng
 * @author Melody Mao
 * @author Evan Savillo
 */
public class Controller
{
    /**
     * ToolbarController handling toolbar actions
     */
    private ToolBarController toolbarController;
    /**
     * FileMenuController handling File menu actions
     */
    private FileMenuController fileMenuController;
    /**
     * EditMenuController handling Edit menu actions
     */
    private EditMenuController editMenuController;
    /**
     * treeStructure View Controller handling the current file's treeStructure View
     */
    private StructureViewController structureViewController;
    /**
     * Compile button defined in Main.fxml
     */
    @FXML
    private Button compileButton;
    /**
     * CompileRun button defined in Main.fxml
     */
    @FXML
    private Button compileRunButton;
    /**
     * Stop button defined in Main.fxml
     */
    @FXML
    private Button stopButton;
    /**
     * TabPane defined in Main.fxml
     */
    @FXML
    private TabPane tabPane;
    /**
     * the console pane defined in Main.fxml
     */
    @FXML
    private StyleClassedTextArea console;
    /**
     * Close menu item of the File menu defined in Main.fxml
     */
    @FXML
    private MenuItem closeMenuItem;
    /**
     * Save menu item of the File menu defined in Main.fxml
     */
    @FXML
    private MenuItem saveMenuItem;
    /**
     * Save As menu item of the File menu defined in Main.fxml
     */
    @FXML
    private MenuItem saveAsMenuItem;
    /**
     * Edit menu defined in Main.fxml
     */
    @FXML
    private Menu editMenu;

    @FXML
    private TreeView treeView;
    /**
     * Checkbox which currently toggles File treeStructure View
     */
    @FXML
    private CheckBox checkBox;
    /**
     * Split pane which contains File treeStructure View on left and the rest on right
     */
    @FXML
    private SplitPane horizontalSplitPane;
    /**
     * a HashMap mapping the tabs and the associated files
     */
    private Map<Tab, File> tabFileMap = new HashMap<>();


    private ToolBarController.CompileWorker compileWorker;

    private ToolBarController.CompileRunWorker compileRunWorker;

    /**
     * This function is called after the FXML fields are populated.
     * Sets up references to the sub Controllers.
     * Sets up bindings.
     * Sets focus to console.
     */
    @FXML
    public void initialize()
    {
        // set up the sub controllers
        this.setupEditMenuController();
        this.setupFileMenuController();
        this.setupToolbarController();
        this.setupStructureViewController();

        this.setButtonBinding();
        this.setupEventAwareness();

        // Sets focus to console on startup
        this.console.requestFocus();
    }

    /**
     * Sets up listening and handling of various events and whatnot
     */
    private void setupEventAwareness()
    {
        // Prevents user from moving caret in console during running
        {
            this.console.addEventFilter(MouseEvent.ANY, event ->
            {
                this.console.requestFocus();
                if (this.compileRunWorker.isRunning())
                    event.consume();
            });
        }

        // Detects presses to tab (overriding the system default that deletes the selection) and calls tabOrUntab
        {
            this.tabPane.addEventFilter(KeyEvent.KEY_PRESSED, event ->
            {
                // if tab or shift+tab pressed
                if (event.getCode() == KeyCode.TAB)
                {
                    tabOrUntab(event);
                }
            });
        }

        // Structure View various
        {
            SplitPane.Divider divider = this.horizontalSplitPane.getDividers().get(0);

            // Toggles the side panel
            this.checkBox.selectedProperty().addListener((observable, oldValue, newValue) ->
                                                         {
                                                             if (!newValue)
                                                                 divider.setPosition(0.0);
                                                             else
                                                                 divider.setPosition(0.25);
                                                         });

            // Prevents user from resizing split pane when closed
            divider.positionProperty().addListener(((observable, oldValue, newValue) ->
            {
                if (!this.checkBox.isSelected()) divider.setPosition(0.0);
            }));


            // Updates the file structure view whenever a key is typed
            this.tabPane.addEventFilter(KeyEvent.KEY_RELEASED, event ->
            {
                this.updateStructureView();

            });

            // Updates the file structure view whenever the tab selection changes
            // e.g., open tab, remove tab, select another tab
            this.tabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) ->
                                                                                {
                                                                                    this.updateStructureView();
                                                                                });
        }
    }

    /**
     * Binds the Close, Save, Save As menu items of the File menu,
     * the Edit menu, with the condition whether the tab pane is empty.
     */
    private void setButtonBinding()
    {
        ReadOnlyBooleanProperty ifCompiling = this.compileWorker.runningProperty();
        ReadOnlyBooleanProperty ifCompilingRunning = this.compileRunWorker.runningProperty();

        this.closeMenuItem.disableProperty().bind(this.fileMenuController.tablessProperty());
        this.saveMenuItem.disableProperty().bind(this.fileMenuController.tablessProperty());
        this.saveAsMenuItem.disableProperty().bind(this.fileMenuController.tablessProperty());
        this.editMenu.disableProperty().bind(this.fileMenuController.tablessProperty());

        this.stopButton.disableProperty().bind(((ifCompiling.not()).and(ifCompilingRunning.not())).or(this.fileMenuController.tablessProperty()));
        this.compileButton.disableProperty().bind(ifCompiling.or(ifCompilingRunning).or(this.fileMenuController.tablessProperty()));
        this.compileRunButton.disableProperty().bind(ifCompiling.or(ifCompilingRunning).or(this.fileMenuController.tablessProperty()));

    }

    /**
     * Returns the file object in the current tab
     *
     * @return the File object of the item selected in the tab pane
     */
    public File getCurrentFile() {
        Tab selectedTab = this.tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            return this.tabFileMap.get(selectedTab);
        } else return null;
    }

    /**
     * Depending on whether or not shift was held down with tab, tab or untab the selection
     * @param event the key event, whether that be tab or shift+tab
     */
    private void tabOrUntab(KeyEvent event) {
        JavaCodeArea currentCodeArea = this.getCurrentCodeArea();
        if (currentCodeArea != null) {
            if (event.isShiftDown()) { // Shift was held down with tab
                editMenuController.handleUnindentation(currentCodeArea);
            } else // Tab only
                editMenuController.handleIndentation(currentCodeArea);

        }
        event.consume();
    }

    /**
     * Returns the code area currently being viewed in the current tab
     * @return the JavaCodeArea object for the open tab
     */
    public JavaCodeArea getCurrentCodeArea() {
        Tab selectedTab = this.tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            return (JavaCodeArea) ((VirtualizedScrollPane) selectedTab.getContent()).getContent();
        } else
            return null;
    }

    /**
     * Creates a reference to the ToolbarController and passes in window items and other sub Controllers when necessary.
     */
    private void setupToolbarController()
    {
        this.toolbarController = new ToolBarController();
        this.toolbarController.setConsole(this.console);
        this.toolbarController.setFileMenuController(this.fileMenuController);
        this.toolbarController.initialize();
        this.compileWorker = this.toolbarController.getCompileWorker();
        this.compileRunWorker = this.toolbarController.getCompileRunWorker();
    }

    /**
     * Creates a reference to the FileMenuController and passes in window items and other sub Controllers when necessary.
     */
    private void setupFileMenuController()
    {
        this.fileMenuController = new FileMenuController();
        this.fileMenuController.setTabFileMap(this.tabFileMap);
        this.fileMenuController.setTabPane(this.tabPane);
        this.fileMenuController.setParentController(this);
    }

    /**
     * Creates a reference to the EditMenuController and passes in window items and other sub Controllers when necessary.
     */
    private void setupEditMenuController()
    {
        this.editMenuController = new EditMenuController();
        this.editMenuController.setTabPane(this.tabPane);
    }

    /**
     * Creates a reference to the StructureViewController and passes in relevant items
     */
    private void setupStructureViewController() {
        this.structureViewController = new StructureViewController();
        this.structureViewController.setTreeView(this.treeView);
    }

    /**
     * Parses and generates the structure view for the currently open code area
     */
    private void updateStructureView() {
        JavaCodeArea currentCodeArea = this.getCurrentCodeArea();
        File currentFile = this.getCurrentFile();

        // if the code area is open
        if (currentCodeArea != null) {
            // if this is not an unsaved file
            if (currentFile != null) {
                String fileName = currentFile.getName();
                // if this is a java file
                if (fileName.endsWith(".java")) {
                    // Re-generates the tree
                    this.structureViewController.generateStructureTree(currentCodeArea.getText());
                }
            } else {
                // Gets rid of open structure view
                this.resetStructureView();
            }
        }
    }

    /**
     * Calls the method that handles the Compile button action from the toolbarController.
     * @param event Event object
     */
    @FXML
    private void handleCompileButtonAction(Event event)
    {
        this.toolbarController.handleCompileButtonAction(event, this.getCurrentFile());
    }

    /**
     * Calls the method that handles the CompileRun button action from the toolbarController.
     * @param event Event object
     */
    @FXML
    private void handleCompileRunButtonAction(Event event)
    {
        this.toolbarController.handleCompileRunButtonAction(event, this.getCurrentFile());
    }

    /**
     * Calls the method that handles the Stop button action from the toolbarController.
     */
    @FXML
    private void handleStopButtonAction()
    {
        this.toolbarController.handleStopButtonAction();
    }

    /**
     * Calls the method that handles About menu item action from the fileMenuController.
     */
    @FXML
    private void handleAboutAction()
    {
        this.fileMenuController.handleAboutAction();
    }

    /**
     * Calls the method that handles the New menu item action from the fileMenuController.
     */
    @FXML
    private void handleNewAction()
    {
        this.fileMenuController.handleNewAction();
        this.updateStructureView();
    }

    /**
     * Calls the method that handles the Open menu item action from the fileMenuController.
     */
    @FXML
    private void handleOpenAction() {
        this.fileMenuController.handleOpenAction();
        this.updateStructureView();
    }

    /**
     * Clears the currently open structure view of all nodes
     */
    public void resetStructureView() {
        this.structureViewController.resetRootNode();
    }

    /**
     * Calls the method that handles the Close menu item action from the fileMenuController.
     * @param event Event object
     */
    @FXML
    private void handleCloseAction(Event event)
    {
        this.fileMenuController.handleCloseAction(event);
    }

    /**
     * Inelegant, but slight
     * Checks or does not check the box
     *
     * @param bool whether or not to check or uncheck the box
     */
    public void updateCheckbox(Boolean bool)
    {
        this.checkBox.setSelected(bool);
    }

    /**
     * Calls the method that handles the Save As menu item action from the fileMenuController.
     */
    @FXML
    private void handleSaveAsAction()
    {
        this.fileMenuController.handleSaveAsAction();
    }

    /**
     * Calls the method that handles the Save menu item action from the fileMenuController.
     */
    @FXML
    private void handleSaveAction()
    {
        this.fileMenuController.handleSaveAction();
    }

    /**
     * Calls the method that handles the Exit menu item action from the fileMenuController.
     * @param event Event object
     */
    @FXML
    public void handleExitAction(Event event)
    {
        this.fileMenuController.handleExitAction(event);
    }

    /**
     * Calls the method that handles the Edit menu action from the editMenuController.
     s     * @param event ActionEvent object
     */
    @FXML
    private void handleEditMenuAction(ActionEvent event)
    {
        this.editMenuController.handleEditMenuAction(event);
    }

    /**
     * Jump to the line where the selected class/method/field is declared.
     */
    @FXML
    private void handleTreeItemClicked()
    {
        TreeItem selectedTreeItem = (TreeItem) this.treeView.getSelectionModel().getSelectedItem();
        JavaCodeArea currentCodeArea = this.getCurrentCodeArea();
        if (selectedTreeItem != null)
        {
            int lineNum = this.structureViewController.getTreeItemLineNum(selectedTreeItem);
            if (currentCodeArea != null) currentCodeArea.showParagraphAtTop(lineNum - 1);
        }
    }
}
