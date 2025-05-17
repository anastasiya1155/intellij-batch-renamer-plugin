package com.example.model;

import java.util.List;

public class RenameConfig {
  private String basePath;
  private List<RenameOperation> operations;

  public static class RenameOperation {
    private String filePath;
    private int line;
    private int column;
    private String newName;

    public String getFilePath() { return filePath; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public String getNewName() { return newName; }

    @Override
    public String toString() {
      return "Rename at " + filePath + ":" + line + "," + column + " to '" + newName + "'";
    }
  }

  public String getBasePath() {
    return basePath;
  }

  public List<RenameOperation> getOperations() {
    return operations;
  }

  @Override
  public String toString() {
    if (operations == null) return "No operations";
    return operations.size() + " rename operations" +
        (basePath != null ? " with base path: " + basePath : "");
  }
}