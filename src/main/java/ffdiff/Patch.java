package ffdiff;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.Callable;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import ffdiff.ArgsReader.Options;

/** Target file creation */
class Patch {

	//------------------------------------------------------------------------
	// Member constants
	private static final PrintStream stdout = System.out;
	private static final OpenOption[] WRITE = new OpenOption[]
		{ StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING },
		READ = new OpenOption[] { StandardOpenOption.READ };
	private static final Path pathDebugPatch = Paths.get ("patch.debug");

	//------------------------------------------------------------------------
	// Member variables
	private PrintStream outDebug = null;
	private ArgsReader.Options options = null;
	private long targetTimestamp = 0;
	private Set<PosixFilePermission> targetPermissions = null;
	private DosFileAttributes targetAttributes = null;
	private long processedDIFF = 0;

	//------------------------------------------------------------------------
	Patch (final Options o) {
		Objects.requireNonNull (o);
		options = o;
	}

	//------------------------------------------------------------------------
	public void run () throws Exception {
		try (RandomAccessFile inBase = new RandomAccessFile (options.pathBase.toFile (), "r");
		DataInputStream inDiff = new DataInputStream (Files.newInputStream (options.pathDiff, READ));
		DataOutputStream outTarget = new DataOutputStream (Files.newOutputStream (options.pathTarget, WRITE));
		PrintStream outDebug = options.verbose ? new PrintStream (Files.newOutputStream (pathDebugPatch, WRITE)):null) {
			this.outDebug = outDebug;

			processedDIFF = patchHeader (inBase, inDiff, outTarget);
			final int sizeSectionName = 4;
			Map<String, Callable<Long>> table = new HashMap<String, Callable<Long>> ();
			table.put ("CP24", () -> patchCP24 (inBase, inDiff, outTarget));
			table.put ("CP32", () -> patchCP32 (inBase, inDiff, outTarget));
			table.put ("DIFF", () -> patchDIFF (inBase, inDiff, outTarget));

			while (inDiff.available () >= sizeSectionName) {
				final String section = IOUs.readUTF8String (inDiff, sizeSectionName);
				processedDIFF += sizeSectionName;
				if (table.get (section) != null) {
					processedDIFF += table.get (section).call ();
				} else {
					throw FormatUs.formatIOException ("Unknown section name: %s (offset: %d)", section, processedDIFF);
				}
			}
		}

		stdout.printf ("set target timestamp: %d (%s)%n", targetTimestamp, FormatUs.iso8601 (targetTimestamp));
		Files.setLastModifiedTime (options.pathTarget, FileTime.fromMillis (targetTimestamp));
		stdout.printf ("set target POSIX style permissions: %s%n", FormatUs.formatFilePermissions (targetPermissions));
		Files.setPosixFilePermissions (options.pathTarget, targetPermissions);
		stdout.printf ("set target Windows style attributes: %s%n", FormatUs.formatFileAttributes (targetAttributes));
		IOUs.setFileAttributes (options.pathTarget, targetAttributes);
	}

	//------------------------------------------------------------------------
	/** @return Processed DIFF data length */
	private long patchHeader
	(final RandomAccessFile inBase, final DataInputStream inDiff, final DataOutputStream outTarget)
	throws IOException {
		assert inBase != null && inDiff != null && outTarget != null;
		int magic = inDiff.readInt ();
		if (magic != 0xffd1ff00) {
			throw FormatUs.formatIOException ("Diff file format and version check failed. Expected: ffd1ff00 Got: %x", magic);
		}

		int sizeContent = inDiff.read ();
		long sizeBase = inDiff.readLong (), sizeTarget = inDiff.readLong ();
		targetTimestamp = inDiff.readLong ();
		targetPermissions = IOUs.parseFilePermissions (IOUs.readBytea (inDiff, 2));
		targetAttributes = new IOUs.ParsedFileAttributes ((byte) inDiff.read ());

		if (sizeBase != options.sizeBase) {
			throw FormatUs.formatIOException ("Base file size (%,d) not equal to the value saved in diff file (%,d)",
				options.sizeBase, sizeBase);
		}
		stdout.printf ("target size: %,d | timestamp: %d (%s)%n",
			sizeTarget, targetTimestamp, FormatUs.iso8601 (targetTimestamp));

		int sizeOptional = sizeContent - Long.BYTES * 3 - Byte.BYTES * 3;
		inDiff.skip (sizeOptional);
		return Integer.BYTES + sizeContent;
	}

	//------------------------------------------------------------------------
	/** @return Processed DIFF data length */
	private long patchCP24
	(final RandomAccessFile inBase, final DataInputStream inDiff, final DataOutputStream outTarget)
	throws IOException {
		assert inBase != null && inDiff != null && outTarget != null;
		log ("%d: CP24%n", processedDIFF);

		final int sizeContent = 11;
		if (inDiff.read () != sizeContent) { // Section Content Size
			throw FormatUs.formatIOException ("Wrong CP24 Section Content Size field value (offset: %d)", processedDIFF);
		}
		int offset = inDiff.readInt ();
		int length = OddByteLength.readInt3BE (inDiff);
		byte[] checksum = new byte [4];
		inDiff.read (checksum);

		log ("Section Content Size: %d | Copy Offset: %s | Copy Length: %s | Checksum: %s%n",
			sizeContent, offset, length, Hex.encodeHexString (checksum));

		IOUs.copy (inBase, outTarget, offset, length);
		// TODO checksum
		return Byte.BYTES + sizeContent;
	}

	//------------------------------------------------------------------------
	/** @return Processed DIFF data length */
	private long patchCP32
	(final RandomAccessFile inBase, final DataInputStream inDiff, final DataOutputStream outTarget)
	throws IOException {
		assert inBase != null && inDiff != null && outTarget != null;
		log ("%d: CP32%n", processedDIFF);

		final int sizeContent = 27;
		if (inDiff.read () != sizeContent) { // Section Content Size
			throw FormatUs.formatIOException ("Wrong CP32 Section Content Size field value (offset: %d)", processedDIFF);
		}
		long offset = OddByteLength.readInt7BE (inDiff);
		int length = inDiff.readInt ();
		byte[] checksum = new byte [16];
		inDiff.read (checksum);

		log ("Section Content Size: %d | Copy Offset: %s | Copy Length: %s | Checksum: %s%n",
			sizeContent, offset, length, Hex.encodeHexString (checksum));

		IOUs.copy (inBase, outTarget, offset, length);
		// TODO checksum
		return Byte.BYTES + sizeContent;
	}

	//------------------------------------------------------------------------
	/** @return Processed DIFF data length */
	private long patchDIFF
	(final RandomAccessFile inBase, final DataInputStream inDiff, final DataOutputStream outTarget)
	throws IOException {
		assert inBase != null && inDiff != null && outTarget != null;
		log ("%d: DIFF%n", processedDIFF);

		int sizeContent = inDiff.readInt ();
		String compression = IOUs.readUTF8String (inDiff, 1);
		if (! compression.equals ("N")) {
			throw FormatUs.formatIOException ("Unsupported Compression Algorithm: %s (offset: %d)",
				compression, processedDIFF);
		}
		// TODO Compression Algorithm
		String encryption = IOUs.readUTF8String (inDiff, 1);
		if (! encryption.equals ("N")) {
			throw FormatUs.formatIOException ("Unsupported Encryption Algorithm: %s", encryption);
		}
		// TODO Encryption Algorithm
		int sizeOriginalData = inDiff.readInt ();
		byte[] checksum = new byte [16];
		inDiff.read (checksum);

		log ("Section Content Size: %d | Compression Algorithm: %s | Encryption Algorithm: %s | " +
			"Original Data Size: %d | Checksum: %s%n",
			sizeContent, compression, encryption, sizeOriginalData, Hex.encodeHexString (checksum));

		IOUtils.copyLarge (inDiff, outTarget, 0, sizeOriginalData);
		// TODO checksum

		return Integer.BYTES + sizeContent;
	}

	//------------------------------------------------------------------------
	private void log (String format, Object... args) {
		if (outDebug != null) {
			outDebug.printf (format, args);
		}
	}

}
