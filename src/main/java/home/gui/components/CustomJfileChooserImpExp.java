package home.gui.components;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import home.file.FileHandler;
import home.file.IExporter;
import home.file.IImporter;
import home.file.csv.CsvExporter;
import home.file.csv.CsvImporter;
import home.file.json_yaml.JsonExporter;
import home.file.json_yaml.JsonImporter;
import home.file.json_yaml.YamlExporter;
import home.file.json_yaml.YamlImporter;
import home.file.ser.BserExporter;
import home.file.ser.BserImporter;
import home.file.ser.SerExporter;
import home.file.ser.SerImporter;
import home.file.xml.XmlExporter;
import home.file.xml.XmlImporter;
import home.gui.DataActionInGui;
import home.models.AbstractVehicle;

public final class CustomJfileChooserImpExp extends JFileChooser {

    public enum DataFormat {

        XML("xml", "XML (*.xml)"),
        YAML("yaml", "YAML (*.yaml)"),
        JSON("json", "JSON (*.json)"),
        CSV("csv", "CSV (*.csv)"),
        BSER("bser", "BSER (*.bser)"),
        SER("ser", "SER (*.ser)");

        private final String extension;
        private final String extensionDescription;

        DataFormat(String extension, String extensionDescription) {
            this.extension = extension;
            this.extensionDescription = extensionDescription;
        }

        public String getExtension() {
            return extension;
        }

        public String getExtensionDescription() {
            return extensionDescription;
        }
    }

    private static final File APPLICATION_DIR = new File(".");

    private CustomJfileChooserImpExp() {
        super(APPLICATION_DIR);
    }

    public static void createAndShowChooser(Component parent, DataFormat dataFormat,
            boolean isImport) throws IOException {
        var fileChooser = new CustomJfileChooserImpExp();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                dataFormat.extensionDescription, new String[]{dataFormat.extension}));
        fileChooser.showChooser(parent, dataFormat, isImport);
    }

    private void showChooser(Component parent, DataFormat dataFormat,
            boolean isImport) throws IOException {
        String direction = (isImport ? "Import from " : "Export to ") + dataFormat.name();

        int chooserState = showDialog(parent, direction);
        if (JFileChooser.APPROVE_OPTION != chooserState) {
            return;
        }

        File file = getSelectedFile();
        file = addExtensionToFileIfNotExists(file, dataFormat.getExtension());

        if (isImport) {
            IImporter importer = switch (dataFormat) {
                case XML -> new XmlImporter();
                case YAML -> new YamlImporter();
                case JSON -> new JsonImporter();
                case CSV -> new CsvImporter();
                case BSER -> new BserImporter();
                case SER -> new SerImporter();
            };
            List<AbstractVehicle> dataObjs = importer.importDataObjsFromFile(file);
            DataActionInGui.add(dataObjs);
        } else {
            IExporter exporter = switch (dataFormat) {
                case XML -> new XmlExporter();
                case YAML -> new YamlExporter();
                case JSON -> new JsonExporter();
                case CSV -> new CsvExporter();
                case BSER -> new BserExporter();
                case SER -> new SerExporter();
            };

            if (DataFormat.SER == dataFormat) {
                ((SerExporter) exporter).exportAllDataObjsToFile(file);
            } else {
                String text = exporter.exportAllDataObjsToString();
                FileHandler.writeStringToFile(file, text);
            }
        }

        JOptionPane.showMessageDialog(parent, direction + " successfully",
                direction, JOptionPane.INFORMATION_MESSAGE);
    }

    private File addExtensionToFileIfNotExists(File file, String extension) {
        String extensionWithDot = "." + extension;
        return file.getName().endsWith(extensionWithDot) ? file
                : new File(file.getAbsolutePath() + extensionWithDot);
    }
}
