package mimalyzer.iface;

public interface CompilationError {
  public int line();
  public int column();
  public String msg();
}
