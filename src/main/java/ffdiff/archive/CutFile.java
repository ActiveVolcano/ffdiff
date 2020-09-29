package ffdiff.archive;

import java.io.*;
import java.nio.file.*;

import org.apache.commons.lang3.exception.ExceptionUtils;

/** Cut a piece of file */
public class CutFile {

	//------------------------------------------------------------------------
	// Member variables
	private static final OpenOption[] WRITE = new OpenOption[]
			{ StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING },
			READ = new OpenOption[] { StandardOpenOption.READ };
	@SuppressWarnings("unused")
	private static final PrintStream stdout = System.out, stderr = System.err;

	//------------------------------------------------------------------------
	// Member constants
	private static Path source, target;
	private static long offset;
	private static int length;

	//------------------------------------------------------------------------
	public static void main (String[] args) {
		try {
			readArgs (args);
			cut ();
		} catch (Exception e) {
			stderr.println (ExceptionUtils.getMessage (e));
		}
	}

	//------------------------------------------------------------------------
	private static void readArgs (String[] args) {
		if (args.length != 4) {
			stderr.println ("Usage: {source file name} {offset} {length} {target file name}");
			System.exit (1);
		}
		source = Paths.get (args[0]);
		offset = Long.valueOf (args[1]);
		length = Integer.valueOf (args[2]);
		target = Paths.get (args[3]);
	}

	//------------------------------------------------------------------------
	private static void cut () throws IOException {
		byte[] buffer = new byte [length];
		try (DataInputStream in = new DataInputStream (Files.newInputStream (source, READ));
			OutputStream out = Files.newOutputStream (target, WRITE)) {
			in.skip (offset);
			in.readFully (buffer);
			out.write (buffer);
		}
	}

}
