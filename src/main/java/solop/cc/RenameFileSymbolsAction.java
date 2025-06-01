package solop.cc;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.rename.RenameProcessor;
import org.jetbrains.annotations.NotNull;
import solop.cc.ui.RenameSymbolsDialog;
import solop.cc.ui.RenameSymbolsDialog.SymbolRenameInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Action to rename symbols in the current file.
 * <p>
 * This action collects all symbols from the current file
 * and presents a dialog allowing the user to rename them.
 * </p>
 */
public class RenameFileSymbolsAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // Get the file from either editor or project view
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Document document = null;
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        // If we don't have a PSI file directly, try to get it from virtual file
        if (psiFile == null) {
            com.intellij.openapi.vfs.VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
            if (virtualFile != null && !virtualFile.isDirectory() && virtualFile.isValid()) {
                psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(virtualFile);
                document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(virtualFile);

                // Open the file in the editor if it's not already open
                if (editor == null) {
                    editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openTextEditor(
                        new com.intellij.openapi.fileEditor.OpenFileDescriptor(project, virtualFile), true);
                }
            }
        } else if (editor != null) {
            // We have both editor and psiFile
            document = editor.getDocument();
        } else {
            // We have psiFile but no editor
            com.intellij.openapi.vfs.VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile != null) {
                document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(virtualFile);
                // Open the file in the editor
                editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openTextEditor(
                    new com.intellij.openapi.fileEditor.OpenFileDescriptor(project, virtualFile), true);
            }
        }

        if (psiFile == null || document == null) {
            Messages.showInfoMessage(project, "Could not open or process the selected file.", "File Error");
            return;
        }

        // Collect all symbols from the current file
        List<SymbolRenameInfo> symbols = FileSymbolsCollector.collectSymbols(psiFile, document);

        if (symbols.isEmpty()) {
            Messages.showInfoMessage(project, "No renamable symbols found in the current file.", "No Symbols Found");
            return;
        }

        // Show the rename dialog
        RenameSymbolsDialog dialog = new RenameSymbolsDialog(project, symbols);
        if (!dialog.showAndGet()) {
            return; // User cancelled
        }

        List<SymbolRenameInfo> symbolsToRename = dialog.getSymbolsToRename();

        if (symbolsToRename.isEmpty()) {
            Messages.showInfoMessage(project, "No symbols were selected for renaming.", "No Changes");
            return;
        }

        // Process the rename operations
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Renaming symbols", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                processSymbolRenames(project, symbolsToRename, indicator);
            }
        });
    }

    /**
     * Processes symbol rename operations collected from the dialog
     */
    private void processSymbolRenames(Project project, List<SymbolRenameInfo> symbolsToRename, ProgressIndicator indicator) {
        List<String> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        indicator.setIndeterminate(false);
        indicator.setText("Processing symbol renames...");

        int totalRenames = symbolsToRename.size();
        for (int i = 0; i < totalRenames; i++) {
            SymbolRenameInfo symbol = symbolsToRename.get(i);
            indicator.setText2("Renaming: " + symbol.getOriginalName() + " to " + symbol.getNewName());
            indicator.setFraction((double) i / totalRenames);

            try {
                boolean success = renameSymbolElement(project, symbol.getElement(), symbol.getNewName());
                if (success) {
                    results.add("Successfully renamed '" + symbol.getOriginalName() + "' to '" + symbol.getNewName() + "'");
                } else {
                    errors.add("Failed to rename '" + symbol.getOriginalName() + "'");
                }
            } catch (Exception ex) {
                errors.add("Error renaming '" + symbol.getOriginalName() + "': " + ex.getMessage());
            }
        }

        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            StringBuilder message = new StringBuilder();
            message.append("Completed ").append(results.size()).append(" out of ").append(totalRenames).append(" renames.\n\n");

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

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // Check if we have a file either from the editor or from the project view
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        // If we don't have a PSI file directly, check if there's a virtual file selection
        if (psiFile == null && virtualFile != null && !virtualFile.isDirectory() && virtualFile.isValid()) {
            // We have a valid file selected in project view or editor tab
            psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        }

        // If we still don't have a PSI file, try to get the current editor file
        if (psiFile == null) {
            // This helps for editor tab right-click when no explicit VirtualFile is provided
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor != null) {
                Document document = editor.getDocument();
                if (document != null) {
                    virtualFile = FileDocumentManager.getInstance().getFile(document);
                    if (virtualFile != null && virtualFile.isValid()) {
                        psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                    }
                }
            }
        }

        e.getPresentation().setEnabled(psiFile != null);
    }
}
