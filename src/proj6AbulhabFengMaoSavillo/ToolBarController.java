/*
 * File: ToolBarController.java
 * F18 CS361 Project 6
 * Names: Melody Mao, Zena Abulhab, Yi Feng, Evan Savillo
 * Date: 10/27/2018
 * This file contains the ToolBarController class, handling Toolbar related actions.
 */

package proj6AbulhabFengMaoSavillo;
import javafx.application.Platform;
import org.fxmisc.richtext.StyleClassedTextArea;
import javafx.scene.control.*;
import javafx.event.Event;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.io.*;
import javafx.concurrent.Task;
import javafx.concurrent.Service;

/**
 * ToolbarController handles Toolbar related actions.
 *
 * @author Liwei Jiang
 * @author Martin Deutsch
 * @author Tatsuya Yokota
 * @author Melody Mao
 */
public class ToolBarController {
    /**
     * a HashMap mapping the tabs and the associated files
     */
    private Map<Tab,File> tabFileMap = new HashMap<Tab,File>();
    /**
     * Console defined in Main.fxml
     */
    private StyleClassedTextArea console;
    /**
     * Process currently compiling or running a Java file
     */
    private Process curProcess;
    /**
     * Thread representing the Java program input stream
     */
    private Thread inThread;
    /**
     * Thread representing the Java program output stream
     */
    private Thread outThread;

    /**
     * Mutex lock to control input and output threads' access to console
     */
    private Semaphore mutex;
    /**
     * The consoleLength of the output on the console
     */
    private int consoleLength;
    /**
     * The FileMenuController
     */
    private FileMenuController fileMenuController;
    /**
     * A CompileWorker object compiles a Java file in a separate thread.
     */
    private CompileWorker compileWorker;
    /**
     * A CompileRunWorker object compiles and runs a Java file in a separate thread.
     */
    private CompileRunWorker compileRunWorker;

    /**
     * Initializes the ToolBarController controller.
     * Sets the Semaphore, the CompileWorker and the CompileRunWorker.
     */
    public void initialize() {
        this.mutex = new Semaphore(1);
        this.compileWorker = new CompileWorker();
        this.compileRunWorker = new CompileRunWorker();
    }

    /**
     * Sets the console pane.
     *
     * @param console StyleClassedTextArea defined in Main.fxml
     */
    public void setConsole(StyleClassedTextArea console) {
        this.console = console;
    }

    /**
     * Sets the tab file map.
     *
     * @param tabFileMap a HashMap mapping the tabs and the associated files
     */
    public void setTabFileMap(Map tabFileMap) {
        this.tabFileMap = tabFileMap;
    }

    /**
     * Sets the FileMenuController.
     *
     * @param fileMenuController FileMenuController created in main Controller.
     */
    public void setFileMenuController(FileMenuController fileMenuController) {
        this.fileMenuController = fileMenuController;
    }

    /**
     * Gets the CompileWorker.
     *
     * @return CompileWorker
     */
    public CompileWorker getCompileWorker() {
        return this.compileWorker;
    }

    /**
     * Gets the CompileRunWorker.
     *
     * @return CompileRunWorker
     */
    public CompileRunWorker getCompileRunWorker() {
        return this.compileRunWorker;
    }

    /**
     * Helper method for running Java Compiler.
     */
    private boolean compileJavaFile(File file) {
        try {
            Platform.runLater(() -> {
                this.console.clear();
                this.consoleLength = 0;
            });

            ProcessBuilder pb = new ProcessBuilder("javac", file.getAbsolutePath());
            this.curProcess = pb.start();

            this.outputToConsole();

            // true if compiled without compile-time error, else false
            return this.curProcess.waitFor() == 0;
        } catch (Throwable e) {
            Platform.runLater(() -> {
                this.fileMenuController.createErrorDialog("File Compilation", "Error compiling.\nPlease try again with another valid Java File.");
            });
            return false;
        }
    }

    /**
     * Helper method for running Java Program.
     */
    private boolean runJavaFile(File file) {
        try {
            Platform.runLater(() -> {
                this.console.clear();
                consoleLength = 0;
            });
            ProcessBuilder pb = new ProcessBuilder("java", file.getName().substring(0, file.getName().length() - 5));
            pb.directory(file.getParentFile());
            this.curProcess = pb.start();

            // Start output and input in different threads to avoid deadlock
            this.outThread = new Thread() {
                public void run() {
                    try {
                        // start output thread first
                        mutex.acquire();
                        outputToConsole();
                    } catch (Throwable e) {
                        Platform.runLater(() -> {
                            // print stop message if other thread hasn't
                            if (consoleLength == console.getLength()) {
                                console.appendText("\nProgram exited unexpectedly\n");
                                console.requestFollowCaret();
                            }
                        });
                    }
                }
            };
            outThread.start();

            inThread = new Thread() {
                public void run() {
                    try {
                        inputFromConsole();
                    } catch (Throwable e) {
                        Platform.runLater(() -> {
                            // print stop message if other thread hasn't
                            if (consoleLength == console.getLength()) {
                                console.appendText("\nProgram exited unexpectedly\n");
                                console.requestFollowCaret();
                            }
                        });
                    }
                }
            };
            inThread.start();

            // true if compiled without compile-time error, else false
            return curProcess.waitFor() == 0;
        } catch (Throwable e) {
            Platform.runLater(() -> {
                this.fileMenuController.createErrorDialog("File Running", "Error running " + file.getName() + ".");
            });
            return false;
        }
    }

    /**
     * Helper method for getting program output
     */
    private void outputToConsole() throws java.io.IOException, java.lang.InterruptedException {
        InputStream stdout = this.curProcess.getInputStream();
        InputStream stderr = this.curProcess.getErrorStream();

        BufferedReader outputReader = new BufferedReader(new InputStreamReader(stdout));
        printOutput(outputReader);

        BufferedReader errorReader = new BufferedReader(new InputStreamReader(stderr));
        printOutput(errorReader);
    }

    /**
     * Helper method for getting program input
     */
    public void inputFromConsole() throws java.io.IOException, java.lang.InterruptedException {
        OutputStream stdin = curProcess.getOutputStream();
        BufferedWriter inputWriter = new BufferedWriter(new OutputStreamWriter(stdin));

        while (curProcess.isAlive()) {
            // wait until signaled by output thread
            this.mutex.acquire();
            // write input to program
            writeInput(inputWriter);
            // signal output thread
            this.mutex.release();
            // wait for output to acquire mutex
            Thread.sleep(5);
        }
        inputWriter.close();
    }

    /**
     * Helper method for printing to console
     *
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    private void printOutput(BufferedReader reader) throws java.io.IOException, java.lang.InterruptedException {
        // if the output stream is paused, signal the input thread
        if (!reader.ready()) {
            this.mutex.release();
        }

        int intch;
        // read in program output one character at a time
        while ((intch = reader.read()) != -1) {
            this.mutex.tryAcquire();
            char ch = (char) intch;
            String out = Character.toString(ch);
            Platform.runLater(() -> {
                // add output to console
                this.console.appendText(out);
                this.console.requestFollowCaret();
            });
            // update console length tracker to include output character
            this.consoleLength++;

            // if the output stream is paused, signal the input thread
            if (!reader.ready()) {
                this.mutex.release();
            }
            // wait for input thread to acquire mutex if necessary
            Thread.sleep(5);
        }
        this.mutex.release();
        reader.close();
    }

    
    /**
     * Helper function to write user input
     */
    public void writeInput(BufferedWriter writer) throws java.io.IOException {
        // wait for user to input line of text
        while (true) {
            if (this.console.getLength() > this.consoleLength) {
                // check if user has hit enter
                if (this.console.getText().substring(this.consoleLength).contains("\n")) {
                    break;
                }
            }
        }
        // write user-entered text to program input
        writer.write(this.console.getText().substring(this.consoleLength));
        writer.flush();
        // update console length to include user input
        this.consoleLength = this.console.getLength();
    }

    /**
     * Checks whether a file embedded in the specified tab should be saved before compiling.
     * Pops up a dialog asking whether the user wants to save the file before compiling.
     * Saves the file if the user agrees so.
     *
     * @param tab Tab where is file is to be compiled
     * @return 0 if user clicked NO button; 1 if user clicked OK button;
     *         2 is user clicked Cancel button; -1 is no saving is needed
     */
    private int checkSaveBeforeCompile(Tab tab) {
        // if the file has not been saved or has been changed
        if (this.fileMenuController.tabNeedsSaving(tab, true)) {
            int buttonClicked = fileMenuController.createConfirmationDialog("Save Changes?",
                    "Do you want to save the changes before compiling?",
                    "Your recent file changes would not be compiled if not saved.");
            // if user presses Yes button
            if (buttonClicked == 1) {
                this.fileMenuController.handleSaveAction();
            }
            return buttonClicked;
        }
        return -1;
    }

    /**
     * A CompileWorker subclass handling Java program compiling in a separated thread in the background.
     * CompileWorker extends the javafx Service class.
     */
    protected class CompileWorker extends Service<Boolean> {
        /**
         * the selected tab in which the embedded file is to be compiled.
         */
        private Tab tab;
        /**
         * Sets the selected tab.
         *
         * @param tab the selected tab in which the embedded file is to be compiled.
         */
        private void setTab(Tab tab) {
            this.tab = tab;
        }

        /**
         * Overrides the createTask method in Service class.
         * Compiles the file embedded in the selected tab, if appropriate.
         *
         * @return true if the program compiles successfully;
         *         false otherwise.
         */
        @Override protected Task<Boolean> createTask() {
            return new Task<Boolean>() {
                /**
                 * Called when we execute the start() method of a CompileRunWorker object
                 * Compiles the file.
                 *
                 * @return true if the program compiles successfully;
                 *         false otherwise.
                 */
                @Override protected Boolean call() {
                    Boolean compileResult = compileJavaFile(tabFileMap.get(tab));
                    if (compileResult) {
                        Platform.runLater(() -> console.appendText("Compilation was successful!\n"));
                    }
                    return compileResult;
                }
            };
        }
    }

    /**
     * A CompileRunWorker subclass handling Java program compiling and running in a separated thread in the background.
     * CompileWorker extends the javafx Service class.
     */
    protected class CompileRunWorker extends Service<Boolean> {
        /**
         * the selected tab in which the embedded file is to be compiled.
         */
        private Tab tab;
        /**
         * Sets the selected tab.
         *
         * @param tab the selected tab in which the embedded file is to be compiled.
         */
        private void setTab(Tab tab) {
            this.tab = tab;
        }

        /**
         * Overrides the createTask method in Service class.
         * Compiles and runs the file embedded in the selected tab, if appropriate.
         *
         * @return true if the program runs successfully;
         *         false otherwise.
         */
        @Override protected Task<Boolean> createTask() {
            return new Task<Boolean>() {
                /**
                 * Called when we execute the start() method of a CompileRunWorker object.
                 * Compiles the file and runs it if compiles successfully.
                 *
                 * @return true if the program runs successfully;
                 *         false otherwise.
                 */
                @Override protected Boolean call() {
                    if (compileJavaFile(tabFileMap.get(tab))) {
                        return runJavaFile(tabFileMap.get(tab));
                    }
                    return false;
                }
            };
        }
    }

    /**
     * Handles the Compile button action.
     *
     * @param event Event object
     * @param tab the Selected tab
     */
    public void handleCompileButtonAction(Event event, Tab tab) {
        // user select cancel button
        if (this.checkSaveBeforeCompile(tab) == 2) {
            event.consume();
        }
        else {
            compileWorker.setTab(tab);
            compileWorker.restart();
        }
    }

    /**
     * Handles the CompileRun button action.
     *
     * @param event Event object
     * @param tab the Selected tab
     */
    public void handleCompileRunButtonAction(Event event, Tab tab) {
        // user select cancel button
        if (this.checkSaveBeforeCompile(tab) == 2) {
            event.consume();
        }
        else {
            compileRunWorker.setTab(tab);
            compileRunWorker.restart();
        }
    }

    /**
     * Handles the Stop button action.
     */
    public void handleStopButtonAction() {
        try {
            if (this.curProcess.isAlive()) {
                this.inThread.interrupt();
                this.outThread.interrupt();
                this.curProcess.destroy();
            }
        } catch (Throwable e) {
            this.fileMenuController.createErrorDialog("Program Stop", "Error stopping the Java program.");
        }
    }
}
