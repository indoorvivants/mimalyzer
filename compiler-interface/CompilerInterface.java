package mimalyzer.iface;

public interface CompilerInterface {
	public CompilerInterface withClasspath(String[] cp);
	public CompilationResult compile(String fileName, String contents, String outDir);
}

