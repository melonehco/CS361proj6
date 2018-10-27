/*
 * File: Controller.java
 * F18 CS361 Project 6
 * Names: Melody Mao, Zena Abulhab, Yi Feng, Evan Savillo
 * Date: 10/27/2018
 * This file contains the Main controller class, handling actions evoked by the Main window.
 */

package proj6AbulhabFengMaoSavillo;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.event.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;

import java.io.File;
import java.util.*;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.Bindings;
import javafx.scene.input.MouseEvent;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
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
    private MenuItem undoMenuItem;
    @FXML
    private MenuItem redoMenuItem;
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
     * Initializes the tab file map with the default tab.
     * Sets up bindings.
     * Sets up references to the sub Controllers.
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
            //Prevents user from resizing split pane when closed
            SplitPane.Divider divider = this.horizontalSplitPane.getDividers().get(0);
            this.checkBox.selectedProperty().addListener((observable, oldValue, newValue) ->
                                                         {
                                                             if (newValue)
                                                                 divider.setPosition(0.0);
                                                             else
                                                                 divider.setPosition(0.25);
                                                         });
            divider.positionProperty().addListener(((observable, oldValue, newValue) ->
            {
                if (this.checkBox.isSelected()) divider.setPosition(0.0);
            }));

            //Updates the file structure tree whenever a key is typed
            this.tabPane.addEventFilter(KeyEvent.KEY_RELEASED, event ->
            {
                this.updateStructureViewForCurrentTab();

            });

            this.tabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
                this.updateStructureViewForCurrentTab();
            });
        }
    }


    private void updateStructureViewForCurrentTab() {

        Tab selectedTab = this.tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            CodeArea activeCodeArea = (CodeArea) ((VirtualizedScrollPane) selectedTab.getContent()).getContent();
            this.structureViewController.generateStructureTree(activeCodeArea.getText());
        }
    }
    /**
     * Depending on whether or not shift was held down with tab, tab or untab the selection
     *
     * @param event the key event, whether that be tab or shift+tab
     */
    private void tabOrUntab(KeyEvent event)
    {
        Tab selectedTab = this.tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null)
        { // if a tab is open

            CodeArea activeCodeArea = (CodeArea) ((VirtualizedScrollPane) selectedTab.getContent()).getContent();

            if (event.isShiftDown())
            { // Shift was held down with tab
                editMenuController.handleUnindentation(activeCodeArea);
            }
            else // Tab only
                editMenuController.handleIndentation(activeCodeArea);

        }
        event.consume();

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
    private void setupStructureViewController()
    {
        this.structureViewController = new StructureViewController();
        this.structureViewController.setTreeView(this.treeView);
    }

    /**
     * Binds the Close, Save, Save As menu items of the File menu,
     * the Edit menu, with the condition whether the tab pane is empty.
     */
    private void setButtonBinding()
    {
        BooleanBinding ifTabPaneEmpty = Bindings.isEmpty(tabPane.getTabs());
        ReadOnlyBooleanProperty ifCompiling = this.compileWorker.runningProperty();
        ReadOnlyBooleanProperty ifCompilingRunning = this.compileRunWorker.runningProperty();

        this.closeMenuItem.disableProperty().bind(ifTabPaneEmpty);
        this.saveMenuItem.disableProperty().bind(ifTabPaneEmpty);
        this.saveAsMenuItem.disableProperty().bind(ifTabPaneEmpty);
        this.editMenu.disableProperty().bind(ifTabPaneEmpty);

        this.stopButton.disableProperty().bind(((ifCompiling.not()).and(ifCompilingRunning.not())).or(ifTabPaneEmpty));
        this.compileButton.disableProperty().bind(ifCompiling.or(ifCompilingRunning).or(ifTabPaneEmpty));
        this.compileRunButton.disableProperty().bind(ifCompiling.or(ifCompilingRunning).or(ifTabPaneEmpty));
    }

    /**
     * Calls the method that handles the Compile button action from the toolbarController.
     *
     * @param event Event object
     */
    @FXML
    private void handleCompileButtonAction(Event event)
    {
        // get the current tab and its corresponding File object
        Tab selectedTab = this.tabPane.getSelectionModel().getSelectedItem();
        File selectedFile = this.tabFileMap.get(selectedTab);
        this.toolbarController.handleCompileButtonAction(event, selectedFile);
    }

    /**
     * Calls the method that handles the CompileRun button action from the toolbarController.
     *
     * @param event Event object
     */
    @FXML
    private void handleCompileRunButtonAction(Event event)
    {
        // get the current tab and its corresponding File object
        Tab selectedTab = this.tabPane.getSelectionModel().getSelectedItem();
        File selectedFile = this.tabFileMap.get(selectedTab);
        this.toolbarController.handleCompileRunButtonAction(event, selectedFile);
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
    }

    /**
     * Calls the method that handles the Open menu item action from the fileMenuController.
     */
    @FXML
    private void handleOpenAction()
    {
        this.fileMenuController.handleOpenAction();
    }

    /**
     * Calls the method that handles the Close menu item action from the fileMenuController.
     *
     * @param event Event object
     */
    @FXML
    private void handleCloseAction(Event event)
    {
        this.fileMenuController.handleCloseAction(event);
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
     *
     * @param event Event object
     */
    @FXML
    public void handleExitAction(Event event)
    {
        this.fileMenuController.handleExitAction(event);
    }

    /**
     * Calls the method that handles the Edit menu action from the editMenuController.
     *
     * @param event ActionEvent object
     */
    @FXML
    private void handleEditMenuAction(ActionEvent event)
    {
        this.editMenuController.handleEditMenuAction(event);
    }
}
