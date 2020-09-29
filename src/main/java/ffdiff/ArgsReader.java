package ffdiff;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;

import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/** Reads command-line arguments and returns options. */
class ArgsReader {

	//------------------------------------------------------------------------
	private static final PrintStream stdout = System.out;
	private static final String DEFAULT_DIFF_EXT = ".ffdiff";

	//------------------------------------------------------------------------
	class Options {
		boolean verbose = false;
		Modes mode = null;
		Path pathBase = null, pathTarget = null, pathDiff = null;
		long sizeBase = 0, sizeTarget = 0, sizeDiff = 0;
		int sizeBlock = 0;
	}

	enum Modes {
		DIFF,
		PATCH,
	}

	//------------------------------------------------------------------------
	class IllegalArgsException extends Exception {
		private static final long serialVersionUID = 1;
		IllegalArgsException (String message) {
			super (message);
		}
	}

	//------------------------------------------------------------------------
	private static final int MIN_BLOCK_SIZE = 128, MAX_BLOCK_COUNT = 16 * 1024 * 1024;

	//------------------------------------------------------------------------
	Options read (String[] args) throws IllegalArgsException {
		Options o = new Options ();
		LinkedList<String> a = new LinkedList<String> (Arrays.asList (args));
		final DefaultKeyValue<String, String> pairOptionValue = new DefaultKeyValue<String, String> ();
		Map<String, Callable<Options>> table = new HashMap<String, Callable<Options>> ();
		table.put ("help",      () -> argHelp       (o, pairOptionValue));
		table.put ("verbose",   () -> argVerbose    (o, pairOptionValue));
		table.put ("debug",     () -> argVerbose    (o, pairOptionValue));
		table.put ("mode",      () -> argMode       (o, pairOptionValue));
		table.put ("base",      () -> argBase       (o, pairOptionValue));
		table.put ("target",    () -> argTarget     (o, pairOptionValue));
		table.put ("diff",      () -> argDiff       (o, pairOptionValue));
		final String[] noValueOptions = new String[] { "help", "verbose", "debug" };

		try {
			if (ArrayUtils.isEmpty (args)) {
				table.get ("help").call ();
			}

			while (! a.isEmpty ()) {
				final String option = getOption (a);
				pairOptionValue.setKey (option);
				pairOptionValue.setValue (getValue (a, option, noValueOptions));
				if (table.get (option) != null) {
					table.get (option).call ();
				}
			}
		} catch (Exception e) {
			throw (e instanceof IllegalArgsException) ? (IllegalArgsException) e
				: new IllegalArgsException (e.getMessage ());
		}

		checkArgs (o);
		calcSize (o);
		return o;
	}

	//------------------------------------------------------------------------
	private void checkArgs (Options o) throws IllegalArgsException {
		if (o.pathBase == null && o.pathTarget == null && o.pathDiff == null) {
			argHelp (o, null);
		}
		if (o.pathBase == null || o.pathTarget == null || o.pathDiff == null) {
			throw new IllegalArgsException ("Base, target and diff file path should be all specified");
		}
		if (o.mode == Modes.DIFF && FilenameUtils.getExtension (o.pathDiff.toString ()).isEmpty ()) {
			o.pathDiff = Paths.get (o.pathDiff.toString () + DEFAULT_DIFF_EXT);
		}
	}

	//------------------------------------------------------------------------
	private void calcSize (Options o) throws IllegalArgsException {
		getFileSize (o);
		if (o.mode == Modes.DIFF) {
			calcBlockSize (o);
		}
	}

	//------------------------------------------------------------------------
	private Options argHelp (final Options o, final DefaultKeyValue<String, String> pairOptionValue) {
		stdout.println ("Usage: -mode diff|patch -base {file name} -target {file name} -diff {file name}");
		stdout.println ();
		stdout.println ("        mode diff:  Create diff from base and target. Typically diff file is much smaller.");
		stdout.println ("        mode patch: Restore target from base and diff.");
		System.exit (0);
		return o;
	}

	//------------------------------------------------------------------------
	private Options argVerbose (final Options o, final DefaultKeyValue<String, String> pairOptionValue) {
		o.verbose = true;
		return o;
	}

	//------------------------------------------------------------------------
	private Options argMode (final Options o, final DefaultKeyValue<String, String> pairOptionValue)
	throws IllegalArgsException {
		final String value = pairOptionValue.getValue ();
		try {
			o.mode = Modes.valueOf (value.toUpperCase ());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgsException ("Illegal mode: " + value);
		}
		if (o.mode == null) {
			throw new IllegalArgsException ("Mode should be specified");
		}
		return o;
	}

	//------------------------------------------------------------------------
	private Options argBase (final Options o, final DefaultKeyValue<String, String> pairOptionValue) {
		o.pathBase = Paths.get (pairOptionValue.getValue ());
		return o;
	}

	//------------------------------------------------------------------------
	private Options argTarget (final Options o, final DefaultKeyValue<String, String> pairOptionValue) {
		o.pathTarget = Paths.get (pairOptionValue.getValue ());
		return o;
	}

	//------------------------------------------------------------------------
	private Options argDiff (final Options o, final DefaultKeyValue<String, String> pairOptionValue) {
		o.pathDiff = Paths.get (pairOptionValue.getValue ());
		return o;
	}

	//------------------------------------------------------------------------
	private String getOption (final Queue<String> args) {
		if (args == null || args.isEmpty ()) {
			return null;
		}
		String option = args.poll ();
		if (option.startsWith ("--")) {
			option = option.substring (2);
		} else if (option.startsWith ("-")) {
			option = option.substring (1);
		}
		return option;
	}

	//------------------------------------------------------------------------
	private String getValue (final Deque<String> args, final String option, final String... noValueOptions)
	throws IllegalArgsException {
		if ( args == null || args.isEmpty ()) {
			return null;
		}
		String value = null;
		if (! StringUtils.equalsAny (option, noValueOptions)) {
			if (args.isEmpty ()) {
				throw new IllegalArgsException ("Option " + option + " has no value");
			}
			value = args.pop ();
		}
		return value;
	}

	//------------------------------------------------------------------------
	private void getFileSize (final Options o) throws IllegalArgsException {
		Objects.requireNonNull (o);
		File f = o.pathBase.toFile ();
		if (! f.isFile () || ! f.canRead ()) {
			throw new IllegalArgsException ("Cannot read base file " + o.pathBase);
		}
		o.sizeBase = f.length ();

		if (o.mode == Modes.DIFF) {
			f = o.pathTarget.toFile ();
			if (! f.isFile () || ! f.canRead ()) {
				throw new IllegalArgsException ("Cannot read target file " + o.pathTarget);
			}
			o.sizeTarget = f.length ();
		}

		if (o.mode == Modes.PATCH) {
			f = o.pathDiff.toFile ();
			if (! f.isFile () || ! f.canRead ()) {
				throw new IllegalArgsException ("Cannot read difference file " + o.pathDiff);
			}
			o.sizeDiff = f.length ();
		}
	}

	//------------------------------------------------------------------------
	private void calcBlockSize (final Options o) throws IllegalArgsException {
		Objects.requireNonNull (o);
		long[] size = new long [] { o.sizeBase, o.sizeTarget};
		int s = MIN_BLOCK_SIZE;
		for (boolean bigger = true ; bigger ;) {
			bigger = false;
			for (long s1 : size) {
				if (s1 / s > MAX_BLOCK_COUNT) {
					bigger = true;
					s *= 2;
					break;
				}
			}
		}
		o.sizeBlock = s;
	}

}
