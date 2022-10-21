package slp.core.lexing.util;

import java.io.File;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;

public class SkipEmptyLexer implements Lexer {
	
	private Lexer lexer;
	
	public SkipEmptyLexer(Lexer lexer) {
		this.lexer = lexer;
	}

	@Override
	public Stream<Stream<String>> lexFile(File file) {
		Stream<Stream<String>> lexed = this.lexer.lexFile(file);
		return lexed.map(this::skipEmpty);
	}

	@Override
	public Stream<Stream<String>> lexText(String text) {
		Stream<Stream<String>> lexed = this.lexer.lexText(text);
		return lexed.map(this::skipEmpty);
	}

	@Override
	public Stream<String> lexLine(String line) {
		Stream<String> lexed = this.lexer.lexLine(line);
		return skipEmpty(lexed);
	}

	private Stream<String> skipEmpty(Stream<String> lexed) {
		return lexed.filter(w -> !w.isEmpty());
	}
}
