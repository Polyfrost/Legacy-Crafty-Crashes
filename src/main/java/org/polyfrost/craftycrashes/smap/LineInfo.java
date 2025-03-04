package org.polyfrost.craftycrashes.smap;

public class LineInfo {
	public final FileInfo file;
	public final int line;

	public LineInfo(FileInfo file, int line) {
		this.file = file;
		this.line = line;
	}

	@Override
	public String toString() {
		return "Line[" + line + ']';
	}
}