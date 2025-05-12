package com.example;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.openapi.command.WriteCommandAction;

import java.io.File;

public class RenameSymbolAction extends AnAction {
  public RenameSymbolAction() {
    System.out.println(">>> RenameSymbolAction loaded");
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    // Hardcoded values
    String filePath = "/Users/user/project/MyClass.java";
    int line = 3;     // line number (0-based)
    int column = 17;    // column number (0-based)
    String newName = "renamedSymbol2";

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