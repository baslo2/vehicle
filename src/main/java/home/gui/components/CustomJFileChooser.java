package home.gui.components;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import home.db.DbInitializer;

@SuppressWarnings("serial")
public class CustomJFileChooser extends JFileChooser {

    private static final File APPLICATION_DIR = new File(".");
    private static final String EXTENSION_DESCRIPTION = "SQLite DB (*.db, *.sqlite, *.sqlite3)";
    private static final String[] EXTENSIONS = { "db", "sqlite", "sqlite3" };

    private static final String CREATE_OR_OPEN = "Create/Open";

    private static final String CHOOSE_STORAGE = "Choose storage";
    private static final String TYPE_NAME_OR_CHOOSE_DB_FILE = "Type new file name"
            + " or choose already existed SQLite DB file.\nThis is not a joke!!!";

    private static final int MAX_REJECTIONS_COUNT = 3;
    private int selectionRejectionCounter;

    private final Component parent;

    private CustomJFileChooser(Component parent) {
        super(APPLICATION_DIR);
        this.parent = parent;
    }

    public static CustomJFileChooser create(Component parent) {
        var chooser = new CustomJFileChooser(parent);
        chooser.setFileFilter(new FileNameExtensionFilter(EXTENSION_DESCRIPTION, EXTENSIONS));
        return chooser;
    }

    public void showCreateOrOpen() throws IOException {
        if (JFileChooser.APPROVE_OPTION == showDialog(parent, CREATE_OR_OPEN)) {
            selectionRejectionCounter = 0;
            File file = getSelectedFile();
            checkDBFileExtension(file);
            DbInitializer.createDBFileIfNotExists(file);
        } else {
            JOptionPane.showMessageDialog(parent, TYPE_NAME_OR_CHOOSE_DB_FILE,
                    CHOOSE_STORAGE, JOptionPane.WARNING_MESSAGE);
            selectionRejectionCounter++;
            if (MAX_REJECTIONS_COUNT == selectionRejectionCounter) {
                System.exit(1);
            }
            showCreateOrOpen();
        }
    }

    private void checkDBFileExtension(File file) throws IOException {
        if (Arrays.stream(EXTENSIONS)
                .anyMatch(extension -> file.getName().contains('.' + extension))) {
            return;
        }
        throw new IOException("The file doesn't contain one of extensions: "
                + String.join(", ", EXTENSIONS));
    }
}