package selector;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import scissors.ScissorsSelectionModel;
import selector.SelectionModel.SelectionState;
import static selector.SelectionModel.SelectionState.*;

/**
 * A graphical application for selecting and extracting regions of images.
 */
public class SelectorApp implements PropertyChangeListener {

    
    /**
     * Progress bar to indicate the progress of a model that needs to do long calculations in a
     * PROCESSING state.
     */
    private JProgressBar processingProgress;

    /**
     * Our application window.  Disposed when application exits.
     */
    private final JFrame frame;

    /**
     * Component for displaying the current image and selection tool.
     */
    private final ImagePanel imgPanel;

    /**
     * The current state of the selection tool.  Must always match the model used by `imgPanel`.
     */
    private SelectionModel model;

    /* Components whose state must be changed during the selection process. */
    private JMenuItem saveItem;
    private JMenuItem undoItem;
    private JButton cancelButton;
    private JButton undoButton;
    private JButton resetButton;
    private JButton finishButton;
    private final JLabel statusLabel;


    /**
     * Construct a new application instance.  Initializes GUI components, so must be invoked on the
     * Swing Event Dispatch Thread.  Does not show the application window (call `start()` to do
     * that).
     */
    public SelectorApp() {
        // Initialize application window
        frame = new JFrame("Selector");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Add status bar
        statusLabel = new JLabel();
       

        frame.add(statusLabel, BorderLayout.PAGE_END);

        // Add image component with scrollbars
        imgPanel = new ImagePanel();
 
        JScrollPane pane = new JScrollPane(imgPanel);
        pane.setPreferredSize(new Dimension(700, 700));
        frame.add(pane);



        // Add menu bar
        frame.setJMenuBar(makeMenuBar());

        // Add control buttons
        
        frame.add(makeControlPanel(),BorderLayout.LINE_END);

        // Controller: Set initial selection tool and update components to reflect its state
        setSelectionModel(new PointToPointSelectionModel(true));

        processingProgress = new JProgressBar();
        frame.add(processingProgress, BorderLayout.PAGE_START);

    }

    /**
     * Create and populate a menu bar with our application's menus and items and attach listeners.
     * Should only be called from constructor, as it initializes menu item fields.
     */
    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Create and populate File menu


        JMenu fileMenu = new JMenu("File");
      
        menuBar.add(fileMenu);

        JMenuItem openItem = new JMenuItem("Open...");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_MASK));
        fileMenu.add(openItem);


        saveItem = new JMenuItem("Save...");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_S, InputEvent.ALT_MASK));
        fileMenu.add(saveItem);

        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_C, InputEvent.ALT_MASK));
        fileMenu.add(closeItem);


        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_E, InputEvent.ALT_MASK));
        fileMenu.add(exitItem);

        // Create and populate Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        undoItem = new JMenuItem("Undo");
        editMenu.add(undoItem);
        undoItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_U, InputEvent.ALT_MASK));
        editMenu.add(undoItem); //UNDO keyboard shortcut is ALT + U




        // Controller: Attach menu item listeners
        openItem.addActionListener(e -> openImage());
        closeItem.addActionListener(e -> imgPanel.setImage(null));
        saveItem.addActionListener(e -> saveSelection());
        exitItem.addActionListener(e -> frame.dispose());
        undoItem.addActionListener(e -> model.undo());

        return menuBar;
    }

    /**
     * Return a panel containing buttons for controlling image selection.  Should only be called
     * from constructor, as it initializes button fields.
     */
    private JPanel makeControlPanel() {
       
        JPanel controlPanel = new JPanel(new GridLayout(0, 1));

        undoButton = new JButton("Undo");
        undoButton.addActionListener(e-> getSelectionModel().undo());
        controlPanel.add(undoButton);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e-> getSelectionModel().cancelProcessing());
        controlPanel.add(cancelButton);

        finishButton = new JButton("Finish");
        finishButton.addActionListener(e-> getSelectionModel().finishSelection());
        controlPanel.add(finishButton);

        resetButton = new JButton("Reset");
        resetButton.addActionListener(e-> getSelectionModel().reset());
        controlPanel.add(resetButton);

       
        JComboBox<String> modelSelector = new JComboBox<>();
        modelSelector.addItem("Point-to-point");
        modelSelector.addItem("Intelligent scissors: gray");
        modelSelector.addItem("Intelligent scissors: color");

        modelSelector.addActionListener(e->{
            String selectedModel = (String) modelSelector.getSelectedItem();
            if (selectedModel.equals("Point-to-point")) {
                setSelectionModel(new PointToPointSelectionModel(model));
            } else if (selectedModel.equals("Intelligent scissors: gray")) {
                setSelectionModel(new ScissorsSelectionModel("CrossGradMono", model));
            } else if (selectedModel.equals("Intelligent scissors: color")) {
                setSelectionModel(new ScissorsSelectionModel("CrossGradColor", model));
            }
        });



        controlPanel.add(modelSelector);

        return controlPanel;



    }

    /**
     * Start the application by showing its window.
     */
    public void start() {
        // Compute ideal window size
        frame.pack();

        frame.setVisible(true);
    }

    /**
     * React to property changes in an observed model.  Supported properties include:
     * * "state": Update components to reflect the new selection state.
     */

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

       

        if ("state".equals(evt.getPropertyName())) { // given code
            reflectSelectionState(model.state());
            {
                if (evt.getNewValue().equals(PROCESSING)) {//changed == to .equals
                    processingProgress.setIndeterminate(true);
                } else {
                    processingProgress.setIndeterminate(false);
                    processingProgress.setValue(0);
                }
            }
        }
        if ("progress".equals(evt.getPropertyName())) {
            processingProgress.setIndeterminate(false);
            int progress = (int) evt.getNewValue();
            processingProgress.setValue(progress);

        }
    }

    /**
     * Update components to reflect a selection state of `state`.  Disable buttons and menu items
     * whose actions are invalid in that state, and update the status bar.
     */
    private void reflectSelectionState(SelectionState state) {
        // Update status bar to show current state
        statusLabel.setText(state.toString());


        cancelButton.setEnabled(false);
        undoButton.setEnabled(true);
        resetButton.setEnabled(true);
        finishButton.setEnabled(false);

        if (model.state() == PROCESSING){
            cancelButton.setEnabled(true);
        }

        if (model.state() == NO_SELECTION) {
            undoButton.setEnabled(false);
            resetButton.setEnabled(false);
        }

        if (model.state() == SELECTED) {
            saveItem.setEnabled(true);
        }

        if(model.state() == SELECTING){
            finishButton.setEnabled(true);
        }




    }

    /**
     * Return the model of the selection tool currently in use.
     */
    public SelectionModel getSelectionModel() {
        return model;
    }

    /**
     * Use `newModel` as the selection tool and update our view to reflect its state.  This
     * application will no longer respond to changes made to its previous selection model and will
     * instead respond to property changes from `newModel`.
     */
    public void setSelectionModel(SelectionModel newModel) {
        // Stop listening to old model
        if (model != null) {
            model.removePropertyChangeListener(this);
        }

        imgPanel.setSelectionModel(newModel);
        model = imgPanel.selection();
        model.addPropertyChangeListener("state", this);

        // Since the new model's initial state may be different from the old model's state, manually
        //  trigger an update to our state-dependent view.
        reflectSelectionState(model.state());

       
        model.addPropertyChangeListener("progress", this);
    }

    /**
     * Start displaying and selecting from `img` instead of any previous image.  Argument may be
     * null, in which case no image is displayed and the current selection is reset.
     */
    public void setImage(BufferedImage img) {
        imgPanel.setImage(img);
    }

    /**
     * Allow the user to choose a new image from an "open" dialog.  If they do, start displaying and
     * selecting from that image.  Show an error message dialog (and retain any previous image) if
     * the chosen image could not be opened.
     */
    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // Filter for file extensions supported by Java's ImageIO readers
        chooser.setFileFilter(new FileNameExtensionFilter("Image files",
                ImageIO.getReaderFileSuffixes()));

     

        //Create a file chooser
        int result = chooser.showOpenDialog(frame);


        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();


            try {
                BufferedImage image = ImageIO.read(selectedFile);
                
                if (image != null) {
                    this.setImage(image);
                }
                else {
                    JOptionPane.showMessageDialog(frame,
                            "Image cannot be read.",
                            "Image read error",
                            JOptionPane.ERROR_MESSAGE);
                    openImage();
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame,
                        "Image cannot be read.",
                        "Image read error",
                        JOptionPane.ERROR_MESSAGE);
                openImage();
            }

        }
    }

    /**
     * Save the selected region of the current image to a file selected from a "save" dialog.
     * Show an error message dialog if the image could not be saved.
     */
    private void saveSelection() {


        //EMBELLISHMENTS DONE
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // We always save in PNG format, so only show existing PNG files
        chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));


        int saveOption = chooser.showSaveDialog(null);

        if(saveOption == JFileChooser.APPROVE_OPTION) {

            File selectedFile = chooser.getSelectedFile();
            if (!selectedFile.getName().endsWith(".png") || !selectedFile.getName()
                    .endsWith(".PNG")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".png");
            }
            if (selectedFile.exists()) {
                int n = JOptionPane.showConfirmDialog(frame, "this file already exists, "
                                + "do you want to overwrite it",
                        "this file already exists!", JOptionPane.YES_NO_CANCEL_OPTION);
                if (n != JOptionPane.YES_OPTION) {
                    saveSelection();
                }
            }
            try {
                // Write an image containing the selected pixels to the file
                try (OutputStream outputStream = new FileOutputStream(selectedFile)) {
                    model.saveSelection(outputStream);
                }
            }catch(IOException e){
                JOptionPane.showMessageDialog(frame,e.getMessage(),"Error occured when saving image",
                        JOptionPane.ERROR_MESSAGE);
                saveSelection();
            }
        }
    }

    /**
     * Run an instance of SelectorApp.  No program arguments are expected.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Set Swing theme to look the same (and less old) on all operating systems.
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                /* If the Nimbus theme isn't available, just use the platform default. */
            }

            // Create and start the app
            SelectorApp app = new SelectorApp();
            app.start();
        });
    }
}