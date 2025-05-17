package com.example.model;

import java.util.List;

public class RenameConfig {
  public static class RenameOperation {
    private String filePath;
    private int line;
    private int column;
    private String newName;

    // Getters and setters
    public String getFilePath() { return filePath; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public String getNewName() { return newName; }
  }

  private String basePath;
  private List<RenameOperation> operations;

  public String getBasePath() {
    return basePath;
  }

  public List<RenameOperation> getOperations() {
    return operations;
  }
}