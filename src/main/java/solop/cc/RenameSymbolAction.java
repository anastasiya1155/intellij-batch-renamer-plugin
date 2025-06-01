package solop.cc;

import com.intellij.openapi.project.ProjectUtil;
import solop.cc.model.RenameConfig;
import solop.cc.ui.JsonInputDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.rename.RenameProcessor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Action to rename symbols across the project using JSON configuration.
 * <p>
 * This action displays a dialog for entering JSON configuration which specifies
 * rename operations by file path, line, and column coordinates.
 * </p>
 */
public class RenameSymbolAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    // Show the JSON configuration dialog
    showJsonConfigDialog(project);
  }

  private void showJsonConfigDialog(Project project) {
    JsonInputDialog dialog = new JsonInputDialog(project);
    if (!dialog.showAndGet()) {
      return; // User cancelled
    }

    RenameConfig config = dialog.getConfig();
    if (config == null || config.getOperations() == null || config.getOperations().isEmpty()) {
      Messages.showErrorDialog(project, "No valid rename operations found", "Error");
      return;
    }

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Renaming symbols", true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        processRenameOperations(project, config, indicator);
      }
    });
  }

  private void processRenameOperations(Project project, RenameConfig config, ProgressIndicator indicator) {
    List<String> results = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    String basePath = config.getBasePath();
    VirtualFile baseDir = null;

    if (basePath != null && !basePath.isEmpty()) {
      if (new File(basePath).isAbsolute()) {
        baseDir = LocalFileSystem.getInstance().findFileByIoFile(new File(basePath));
      } else {
        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir != null) {
          baseDir = projectDir.findFileByRelativePath(basePath);
        }
      }

      if (baseDir == null || !baseDir.isDirectory()) {
        ApplicationManager.getApplication().invokeLater(() ->
            Messages.showErrorDialog(project, "Base path not found or not a directory: " + basePath, "Error"));
        return;
      }
    }

    indicator.setIndeterminate(false);
    indicator.setText("Processing rename operations...");

    int totalOps = config.getOperations().size();
    for (int i = 0; i < totalOps; i++) {
      RenameConfig.RenameOperation op = config.getOperations().get(i);
      indicator.setText2("Processing: " + op);
      indicator.setFraction((double) i / totalOps);

      try {
        String filePath = op.getFilePath();

        if (baseDir != null && !new File(filePath).isAbsolute()) {
          VirtualFile targetFile = baseDir.findFileByRelativePath(filePath);
          if (targetFile != null) {
            filePath = targetFile.getPath();
          }
        }

        boolean success = renameSymbol(project, filePath, op.getLine(), op.getColumn(), op.getNewName());
        if (success) {
          results.add("Successfully renamed at " + op.getFilePath() + ":" + op.getLine() + "," + op.getColumn() + " to '" + op.getNewName() + "'");
        } else {
          errors.add("Failed to rename at " + op.getFilePath() + ":" + op.getLine() + "," + op.getColumn());
        }
      } catch (Exception ex) {
        errors.add("Error processing operation at " + op.getFilePath() + ": " + ex.getMessage());
      }
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      StringBuilder message = new StringBuilder();
      message.append("Completed ").append(results.size()).append(" out of ").append(totalOps).append(" operations.\n\n");

      if (!errors.isEmpty()) {
        message.append("Errors (").append(errors.size()).append("):\n");
        for (String error : errors) {
          message.append("- ").append(error).append("\n");
        }
      }

      Messages.showInfoMessage(project, message.toString(), "Rename Operations Complete");
    });
  }


  /**
   * Renames a specific PsiNamedElement
   */
  private boolean renameSymbolElement(Project project, PsiNamedElement element, String newName) {
    boolean[] success = new boolean[1];
    WriteCommandAction.runWriteCommandAction(project, () -> {
      try {
        RenameProcessor processor = new RenameProcessor(project, element, newName, false, false);
        processor.run();
        success[0] = true;
      } catch (Exception e) {
        success[0] = false;
      }
    });

    return success[0];
  }

  private boolean renameSymbol(Project project, String filePath, int line, int column, String newName) {
    VirtualFile vf;

    if (new File(filePath).isAbsolute()) {
      vf = LocalFileSystem.getInstance().findFileByIoFile(new File(filePath));
    } else {
      VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
      if (projectDir == null) {
        System.out.println("Cannot resolve project directory for relative path: " + filePath);
        return false;
      }
      vf = projectDir.findFileByRelativePath(filePath);
    }

    if (vf == null) {
      System.out.println("File not found: " + filePath);
      return false;
    }

    PsiFile psiFile = PsiManager.getInstance(project).findFile(vf);
    if (psiFile == null) return false;

    Document doc = FileDocumentManager.getInstance().getDocument(vf);
    if (doc == null) return false;

    int offset = doc.getLineStartOffset(line) + column;
    PsiElement element = psiFile.findElementAt(offset);
    if (element == null) {
      System.out.println("No element at offset " + offset);
      return false;
    }

    PsiNamedElement namedElement = PsiTreeUtil.getParentOfType(element, PsiNamedElement.class);
    if (namedElement == null) {
      System.out.println("No named element at that position.");
      return false;
    }

    return renameSymbolElement(project, namedElement, newName);
  }
}