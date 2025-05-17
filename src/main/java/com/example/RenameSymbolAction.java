package com.example;

import com.example.model.RenameConfig;
import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ui.Messages;

import java.io.File;
import java.io.IOException;

public class RenameSymbolAction extends AnAction {
  public RenameSymbolAction() {
    System.out.println(">>> RenameSymbolAction loaded");
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
        .withTitle("Select Rename Configuration File")
        .withDescription("Choose a JSON file with rename operations")
        .withFileFilter(file -> file.getExtension() != null && file.getExtension().equals("json"));

    FileChooser.chooseFile(descriptor, project, null, file -> {
      try {
        processConfigFile(project, file);
      } catch (Exception ex) {
        Messages.showErrorDialog(project, "Error processing configuration: " + ex.getMessage(), "Error");
        ex.printStackTrace();
      }
    });
  }

  private void processConfigFile(Project project, VirtualFile configFile) throws IOException {
    String content = new String(configFile.contentsToByteArray());
    Gson gson = new Gson();
    RenameConfig config = gson.fromJson(content, RenameConfig.class);

    if (config.getOperations() == null || config.getOperations().isEmpty()) {
      Messages.showInfoMessage(project, "No rename operations found in configuration file", "No Operations");
      return;
    }

    for (RenameConfig.RenameOperation op : config.getOperations()) {
      renameSymbol(project, op.getFilePath(), op.getLine(), op.getColumn(), op.getNewName());
    }

    Messages.showInfoMessage(project,
        "Completed " + config.getOperations().size() + " rename operations",
        "Rename Complete");
  }

  private void renameSymbol(Project project, String filePath, int line, int column, String newName) {
    VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(new File(filePath));
    if (vf == null) {
      System.out.println("File not found: " + filePath);
      return;
    }

    PsiFile psiFile = PsiManager.getInstance(project).findFile(vf);
    if (psiFile == null) return;

    Document doc = FileDocumentManager.getInstance().getDocument(vf);
    if (doc == null) return;

    int offset = doc.getLineStartOffset(line) + column;
    PsiElement element = psiFile.findElementAt(offset);
    if (element == null) {
      System.out.println("No element at offset " + offset);
      return;
    }

    PsiNamedElement namedElement = PsiTreeUtil.getParentOfType(element, PsiNamedElement.class);
    if (namedElement == null) {
      System.out.println("No named element at that position.");
      return;
    }

    WriteCommandAction.runWriteCommandAction(project, () -> {
      RenameProcessor processor = new RenameProcessor(project, namedElement, newName, false, false);
      processor.run();
      System.out.println("Symbol renamed to " + newName);
    });
  }
}