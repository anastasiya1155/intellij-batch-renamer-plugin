package solop.cc.ui;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import solop.cc.model.RenameConfig;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTabbedPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class JsonInputDialog extends DialogWrapper {
  private final Project project;
  private JTextArea jsonTextArea;
  private VirtualFile selectedFile;
  private JBTabbedPane tabbedPane;
  private JLabel filePathLabel;
  private JBLabel validationMessageLabel;

  public JsonInputDialog(Project project) {
    super(project);
    this.project = project;
    init();
    setTitle("Rename Configuration");
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());

    validationMessageLabel = new JBLabel();
    validationMessageLabel.setForeground(JBColor.RED);

    tabbedPane = new JBTabbedPane();
    tabbedPane.addChangeListener(e -> validateInput());

    JPanel jsonInputPanel = new JPanel(new BorderLayout());

    jsonTextArea = new JTextArea();
    jsonTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
    jsonTextArea.setLineWrap(false);
    jsonTextArea.setWrapStyleWord(false);
    jsonTextArea.setTabSize(2);

    jsonTextArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
      @Override
      public void insertUpdate(javax.swing.event.DocumentEvent e) {
        validateInput();
      }

      @Override
      public void removeUpdate(javax.swing.event.DocumentEvent e) {
        validateInput();
      }

      @Override
      public void changedUpdate(javax.swing.event.DocumentEvent e) {
        validateInput();
      }
    });

    KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    jsonTextArea.getInputMap().put(enterKey, "insert-break");
    jsonTextArea.getActionMap().put("insert-break", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        jsonTextArea.replaceSelection("\n");
      }
    });

    JScrollPane scrollPane = new JBScrollPane(jsonTextArea);
    scrollPane.setPreferredSize(new Dimension(400, 300));

    jsonInputPanel.add(new JLabel("Paste your JSON configuration:"), BorderLayout.NORTH);
    jsonInputPanel.add(scrollPane, BorderLayout.CENTER);

    JPanel fileSelectPanel = new JPanel(new BorderLayout());
    filePathLabel = new JLabel("No file selected");
    JButton browseButton = new JButton("Browse...");
    browseButton.addActionListener(this::browseForFile);

    JPanel fileSelectionControls = new JPanel(new BorderLayout());
    fileSelectionControls.add(browseButton, BorderLayout.EAST);
    fileSelectionControls.add(filePathLabel, BorderLayout.CENTER);

    fileSelectPanel.add(fileSelectionControls, BorderLayout.NORTH);
    fileSelectPanel.add(new JLabel("""
        <html><br>Select a JSON file with the following structure:<br><pre>\
        {
          "operations": [
            {
              "filePath": "src/main/java/example/MyClass.java",
              "line": 3,
              "column": 17,
              "newName": "renamedSymbol"
            }
          ]
        }</pre></html>"""), BorderLayout.CENTER);

    tabbedPane.addTab("Paste JSON", jsonInputPanel);
    tabbedPane.addTab("Select File", fileSelectPanel);

    panel.add(tabbedPane, BorderLayout.CENTER);
    panel.add(validationMessageLabel, BorderLayout.SOUTH);

    return panel;
  }

  private void browseForFile(ActionEvent e) {
    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
        .withTitle("Select Rename Configuration File")
        .withDescription("Choose a JSON file with rename operations")
        .withFileFilter(file -> "json".equals(file.getExtension()));

    VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
    if (file != null) {
      selectedFile = file;
      filePathLabel.setText(file.getPath());
      validateInput();
    }
  }

  private void validateInput() {
    ValidationInfo validationInfo = doValidate();
    if (validationInfo != null) {
      validationMessageLabel.setText(validationInfo.message);
      setOKActionEnabled(false);
    } else {
      validationMessageLabel.setText("");
      setOKActionEnabled(true);
    }
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    int selectedTab = tabbedPane.getSelectedIndex();

    if (selectedTab == 0) {
      String jsonText = jsonTextArea.getText();
      if (jsonText.trim().isEmpty()) {
        return new ValidationInfo("Please enter JSON configuration");
      }

      try {
        validateJsonContent(jsonText);
        return null;
      } catch (Exception e) {
        return new ValidationInfo("Invalid JSON: " + e.getMessage());
      }
    } else {
      if (selectedFile == null) {
        return new ValidationInfo("Please select a JSON file");
      }

      try {
        String content = new String(selectedFile.contentsToByteArray());
        validateJsonContent(content);
        return null;
      } catch (IOException e) {
        return new ValidationInfo("Error reading file: " + e.getMessage());
      } catch (Exception e) {
        return new ValidationInfo("Invalid JSON in file: " + e.getMessage());
      }
    }
  }

  private void validateJsonContent(String jsonContent) {
    Gson gson = new Gson();
    try {
      RenameConfig config = gson.fromJson(jsonContent, RenameConfig.class);

      if (config == null) {
        throw new IllegalArgumentException("JSON could not be parsed to configuration");
      }

      if (config.getOperations() == null || config.getOperations().isEmpty()) {
        throw new IllegalArgumentException("No rename operations found in configuration");
      }

      for (int i = 0; i < config.getOperations().size(); i++) {
        RenameConfig.RenameOperation op = getRenameOperation(config, i);
        if (op.getColumn() < 0) {
          throw new IllegalArgumentException("Operation #" + (i+1) + ": column must be non-negative");
        }
      }
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException("JSON syntax error: " + e.getMessage());
    }
  }

  private static RenameConfig.@NotNull RenameOperation getRenameOperation(RenameConfig config, int i) {
    RenameConfig.RenameOperation op = config.getOperations().get(i);
    if (op.getFilePath() == null || op.getFilePath().trim().isEmpty()) {
      throw new IllegalArgumentException("Operation #" + (i +1) + ": filePath is required");
    }
    if (op.getNewName() == null || op.getNewName().trim().isEmpty()) {
      throw new IllegalArgumentException("Operation #" + (i +1) + ": newName is required");
    }
    if (op.getLine() < 0) {
      throw new IllegalArgumentException("Operation #" + (i +1) + ": line must be non-negative");
    }
    return op;
  }

  public RenameConfig getConfig() {
    int selectedTab = tabbedPane.getSelectedIndex();
    String jsonContent;

    try {
      if (selectedTab == 0) {
        jsonContent = jsonTextArea.getText();
      } else {
        jsonContent = new String(selectedFile.contentsToByteArray());
      }

      Gson gson = new Gson();
      return gson.fromJson(jsonContent, RenameConfig.class);
    } catch (Exception e) {
      Messages.showErrorDialog("Error parsing JSON: " + e.getMessage(), "Error");
      return null;
    }
  }

  @Override
  protected void init() {
    super.init();
    setOKActionEnabled(false);
  }

  /**
   * Override this method to give focus to the text field by default
   */
  @Override
  public JComponent getPreferredFocusedComponent() {
    return jsonTextArea;
  }
}