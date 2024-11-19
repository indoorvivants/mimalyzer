package mimalyzer.iface;

public interface CompilationResult {
  public CompilationError[] errors();
  public String[] classpath();
}
