package solop.cc;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import solop.cc.ui.RenameSymbolsDialog.SymbolRenameInfo;

import java.util.*;

public class FileSymbolsCollector {

    /**
     * Collects all named elements (symbols) from a PSI file
     *
     * @param psiFile The PSI file to analyze
     * @param document The document for the file (used to find offsets)
     * @return List of SymbolRenameInfo objects representing each named element
     */
    public static List<SymbolRenameInfo> collectSymbols(PsiFile psiFile, Document document) {
        List<SymbolRenameInfo> result = new ArrayList<>();
        Set<PsiNamedElement> processedElements = new HashSet<>();

        // Get all named elements
        Collection<PsiNamedElement> namedElements = PsiTreeUtil.findChildrenOfType(psiFile, PsiNamedElement.class);

        // Add relevant named elements to the result
        for (PsiNamedElement element : namedElements) {
            // Skip duplicates
            if (processedElements.contains(element)) {
                continue;
            }

            // Get textual representation (for finding offset)
            PsiElement nameIdentifier = null;
            if (element instanceof PsiNameIdentifierOwner) {
                nameIdentifier = ((PsiNameIdentifierOwner) element).getNameIdentifier();
            }

            // Skip if it has no name
            String name = element.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }

            // Skip if it's not a valid symbol type to rename
            if (!isRenamableSymbol(element)) {
                continue;
            }

            // Find the element's offset in the document
            int offset = -1;
            if (nameIdentifier != null) {
                offset = nameIdentifier.getTextOffset();
            } else if (element.getTextRange() != null) {
                offset = element.getTextRange().getStartOffset();
            }

            if (offset >= 0) {
                result.add(new SymbolRenameInfo(element, offset));
                processedElements.add(element);
            }
        }

        return result;
    }

    /**
     * Determines if an element can be renamed
     */
    private static boolean isRenamableSymbol(PsiNamedElement element) {
        // Check if element is a meaningful, renamable element
        // This approach avoids direct dependencies on Java-specific PSI types
        String elementType = PsiUtilCore.getElementType(element) != null ? 
                          PsiUtilCore.getElementType(element).toString() : "";

        // Include common renamable types based on their general characteristics
        return (element instanceof PsiNameIdentifierOwner) && 
               (elementType.contains("CLASS") || 
                elementType.contains("METHOD") || 
                elementType.contains("FIELD") || 
                elementType.contains("VARIABLE") || 
                elementType.contains("PARAMETER") ||
                elementType.contains("IDENTIFIER"));
    }
}
