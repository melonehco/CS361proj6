/*
 * File: EditMenuController.java
 * F18 CS361 Project 6
 * Names: Melody Mao, Zena Abulhab, Yi Feng, Evan Savillo
 * Date: 10/27/2018
 * This file contains the EditMenuController class, handling Edit menu related actions.
 */

package proj6AbulhabFengMaoSavillo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tab;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.Selection;

import org.fxmisc.richtext.TextEditingArea;
//import javafx.beans.value.ObservableValue;

/**
 * Main controller handles Edit menu related actions.
 *
 * @author Zena Abulhab
 * @author Yi Feng
 * @author Melody Mao
 * @author Evan Savillo
 */
public class EditMenuController {
    /**
     * TabPane defined in Main.fxml
     */
    @FXML
    private TabPane tabPane;

    /**
     * Sets the tab pane.
     *
     * @param tabPane TabPane defined in Main.fxml
     */
    public void setTabPane(TabPane tabPane) {
        this.tabPane = tabPane;
    }

    /**
     * Handles the Edit menu action.
     *
     * @param event ActionEvent object
     */
    public void handleEditMenuAction(ActionEvent event) {
        // get the code area embedded in the selected tab window
        Tab selectedTab = this.tabPane.getSelectionModel().getSelectedItem();
        CodeArea activeCodeArea = (CodeArea) ((VirtualizedScrollPane) selectedTab.getContent()).getContent();
        MenuItem clickedItem = (MenuItem) event.getTarget();
        switch (clickedItem.getId()) {
            case "undoMenuItem":
                activeCodeArea.undo();
                break;
            case "redoMenuItem":
                activeCodeArea.redo();
                break;
            case "cutMenuItem":
                activeCodeArea.cut();
                break;
            case "copyMenuItem":
                activeCodeArea.copy();
                break;
            case "pasteMenuItem":
                activeCodeArea.paste();
                break;
            case "selectMenuItem":
                activeCodeArea.selectAll();
                break;
            case "tabMenuItem":
                this.handleIndentation(activeCodeArea);
                break;
            case "untabMenuItem":
                this.handleUnindentation(activeCodeArea);
                break;
            case "commentMenuItem":
                this.handleToggleCommenting(activeCodeArea);
            default:
        }
    }

    /**
     * Handles the indentation of the selected text in the code area.
     * Called from the accelerator, the tab key, or the menu item
     *
     * @param selectedCodeArea
     */
    public void handleIndentation(CodeArea selectedCodeArea) {
        Selection<?, ?, ?> selection = selectedCodeArea.getCaretSelectionBind();
        int startIdx = selection.getStartParagraphIndex();
        int endIdx = selection.getEndParagraphIndex();
        for (int lineNum = startIdx; lineNum <= endIdx; lineNum++) {
            selectedCodeArea.insertText(lineNum, 0, "\t");
        }
    }

    /**
     * Handles unindentation of the selected text by removing white space from the start.
     * Works one full tab at a time, or for any extra space(s)
     *
     * @param selectedCodeArea
     */
    public void handleUnindentation(CodeArea selectedCodeArea) {
        Selection<?, ?, ?> selection = selectedCodeArea.getCaretSelectionBind();
        int startIdx = selection.getStartParagraphIndex();
        int endIdx = selection.getEndParagraphIndex();
        for (int lineNum = startIdx; lineNum <= endIdx; lineNum++) {
            // full tab(s) present at the start of the line
            if (selectedCodeArea.getParagraph(lineNum).getText().startsWith("\t")) {
                selectedCodeArea.deleteText(lineNum, 0, lineNum, 1);
            }
            // space(s) present at the start of the line, but not a full tab
            else if (selectedCodeArea.getParagraph(lineNum).getText().startsWith(" ")) {
                while (selectedCodeArea.getParagraph(lineNum).getText().startsWith(" ")) {
                    selectedCodeArea.deleteText(lineNum, 0, lineNum, 1);
                }
            }

        }
    }

    /**
     * Handles commenting of the selected text in the code area
     *
     * @param selectedCodeArea
     */
    public void handleToggleCommenting(CodeArea selectedCodeArea)
    {

        // get the start paragraph and the end paragraph of the selection
        Selection<?, ?, ?> selection = selectedCodeArea.getCaretSelectionBind();
        int startIdx = selection.getStartParagraphIndex();
        int endIdx = selection.getEndParagraphIndex();

        // If there is one line that is not commented in the selected paragraphs,
        // comment all selected paragraphs.
        boolean shouldComment = false;
        for (int lineNum = startIdx; lineNum <= endIdx; lineNum++)
        {
            if (!(selectedCodeArea.getParagraph(lineNum).getText().startsWith("//")))
            {
                shouldComment = true;
            }
        }

        // If we should comment all paragraphs, comment all paragraphs.
        // If all selected the paragraphs are commented,
        // uncomment the selected paragraphs.
        if (shouldComment)
        {
            for (int lineNum = startIdx; lineNum <= endIdx; lineNum++)
            {
                selectedCodeArea.insertText(lineNum, 0, "//");
            }
        }
        else
        {
            for (int lineNum = startIdx; lineNum <= endIdx; lineNum++)
            {
                selectedCodeArea.deleteText(lineNum, 0, lineNum, 2);
            }
        }
    }


}