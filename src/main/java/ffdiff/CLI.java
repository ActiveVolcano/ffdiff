package ffdiff;

import java.io.*;
import java.nio.file.Paths;
import org.apache.commons.lang3.exception.ExceptionUtils;

import ffdiff.ArgsReader.IllegalArgsException;
import ffdiff.ArgsReader.Modes;

/** Command-line Interface */
public class CLI {

	//------------------------------------------------------------------------
	// Member constants
	private static final PrintStream stdout = System.out, stderr = System.err;

	//------------------------------------------------------------------------
	public static void main (String[] args) {
		banner ();
		try {
			ArgsReader.Options options = new ArgsReader().read (args);
			showOptions (options);
			if (options.mode == Modes.DIFF) {
				new Diff (options).run ();
			} else if (options.mode == Modes.PATCH) {
				new Patch (options).run ();
			}

		} catch (IllegalArgsException e) {
			stderr.println (e.getMessage ());
		} catch (Exception e) {
			stderr.println (ExceptionUtils.getMessage (e));
		}
	}

	//------------------------------------------------------------------------
	private static void banner () {
		stdout.println ("Fast File DIFFerence version 1.0.190925");
		stdout.println ("Written by CHEN Qingcan, Mid Autumn 2019, Foshan China");
		stdout.println ("----------");
	}

	//------------------------------------------------------------------------
	private static void showOptions (final ArgsReader.Options options) {
		assert options != null;
		stdout.println ("Fast File DIFFerence version 1.0.190925");
		stdout.println ("Written by CHEN Qingcan, Mid Autumn 2019, Foshan China");
		stdout.println ("----------");
		stdout.printf ("mode:  %5s | base: %s | target: %s | diff: %s%n",
			options.mode, options.pathBase, options.pathTarget, options.pathDiff);
		if (options.mode == Modes.DIFF) {
			stdout.printf ("block: %,5d | base: %,d | target: %,d%n",
				options.sizeBlock, options.sizeBase, options.sizeTarget);
		} else if (options.mode == Modes.PATCH) {
			stdout.printf ("             | base: %,d | diff: %,d%n", options.sizeBase, options.sizeDiff);
		}
		if (options.verbose) {
			stdout.printf ("debug: %s (*.debug)%n", Paths.get("").toAbsolutePath ());
		}
		stdout.println ("----------");
	}

}
