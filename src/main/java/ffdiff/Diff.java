package ffdiff;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import ffdiff.ArgsReader.Options;

/** Difference file creation */
class Diff {

	//------------------------------------------------------------------------
	private static final Path pathDebugBase = Paths.get ("base.debug"),
		pathDebugTarget = Paths.get ("target.debug"),
		pathDebugDiff   = Paths.get ("diff.debug");
	private static final OpenOption[] WRITE = new OpenOption[]
		{ StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING };

	//------------------------------------------------------------------------
	private ArgsReader.Options options = null;
	// TODO init cap
	private Map<UniHash, Long> baseHash2Offset   = new HashMap<UniHash, Long>();
	private Map<Long, UniHash> targetOffset2Hash = new LinkedHashMap<Long, UniHash>();

	//------------------------------------------------------------------------
	Diff (final Options o) {
		Objects.requireNonNull (o);
		options = o;
	}

	//------------------------------------------------------------------------
	public void run () throws IOException, DecoderException {
		hash (options.pathBase,   baseHash2Offset, null);
		hash (options.pathTarget, null,            targetOffset2Hash);
		diff ();
		if (options.verbose) {
			writeDebug ();
		}
	}

	//------------------------------------------------------------------------
	private void hash (Path path, Map<UniHash, Long> hash2offset, Map<Long, UniHash> offset2hash)
		throws IOException {
		assert path != null;
		if (hash2offset != null) {
			hash2offset.clear ();
		}
		if (offset2hash != null) {
			offset2hash.clear ();
		}
		byte[] buffer = new byte[options.sizeBlock];
		long offset = 0;

		try (InputStream in = Files.newInputStream (path, StandardOpenOption.READ)) {
			for (int read = in.read (buffer) ; read > 0 ; read = in.read (buffer)) {
				UniHash hash = new UniHash (buffer, 0, read);
				if (hash2offset != null) {
					hash2offset.put (hash, offset);
				}
				if (offset2hash != null) {
					offset2hash.put (offset, hash);
				}
				offset += read;
			}
		}
	}

	//------------------------------------------------------------------------
	private void diff () throws IOException, DecoderException {
		try (RandomAccessFile inTarget = new RandomAccessFile (options.pathTarget.toFile (), "r");
			DataOutputStream outDiff = new DataOutputStream (Files.newOutputStream (options.pathDiff, WRITE))) {
			outDiff.write (Hex.decodeHex ("ffd1ff00"));
			outDiff.write (27);
			outDiff.writeLong (Files.size (options.pathBase));
			outDiff.writeLong (Files.size (options.pathTarget));
			outDiff.writeLong (Files.getLastModifiedTime (options.pathTarget).toMillis ());
			outDiff.write (IOUs.getFilePermissions (options.pathTarget));
			outDiff.write (IOUs.getFileAttributes (options.pathTarget));

			// TODO merge continue blocks
			for (Entry<Long, UniHash> target1 : targetOffset2Hash.entrySet ()) {
				Long baseOffset = baseHash2Offset.get (target1.getValue ());
				if (baseOffset == null) {
					diffDIFF (inTarget, outDiff, target1);
				} else {
					diffCP (baseOffset, outDiff, target1);
				}
			}
		}
	}

	//------------------------------------------------------------------------
	private void diffCP (final long baseOffset, final DataOutputStream outDiff, final Entry<Long, UniHash> target1)
		throws IOException {
		assert outDiff != null && target1 != null;
		final UniHash hash = target1.getValue ();

		outDiff.writeBytes ("CP24");
		outDiff.write (11);
		outDiff.writeInt ((int) baseOffset); // TODO long -> int
		final int length = hash.getLength ();
		outDiff.write (OddByteLength.int3BE (length));
		outDiff.write (Arrays.copyOf (hash.getMD5 (), 4));
	}

	//------------------------------------------------------------------------
	private void diffDIFF
	(final RandomAccessFile inTarget, final DataOutputStream outDiff, final Entry<Long, UniHash> target1)
	throws IOException {
		assert outDiff != null && target1 != null;
		final long offset = target1.getKey ();
		final UniHash hash = target1.getValue ();

		outDiff.writeBytes ("DIFF");
		outDiff.writeInt (hash.getLength () + 22);
		outDiff.writeBytes ("NN"); // No compression, No encryption
		outDiff.writeInt (hash.getLength ());
		outDiff.write (hash.getMD5 ());
		inTarget.seek (offset);
		byte[] buffer = new byte [hash.getLength ()];
		inTarget.readFully (buffer);
		outDiff.write (buffer);
	}

	//------------------------------------------------------------------------
	private void writeDebug () throws IOException {
		writeDebugBase ();
		writeDebugTarget ();
		writeDebugDiff ();
	}

	//------------------------------------------------------------------------
	private void writeDebugBase () throws IOException {
		try (PrintWriter debug = new PrintWriter (Files.newOutputStream (pathDebugBase))) {
			for (Entry<UniHash, Long> base1 : baseHash2Offset.entrySet ()) {
				debug.format ("%d\t%s%n", base1.getValue (), base1.getKey ());
			}
		}
	}

	//------------------------------------------------------------------------
	private void writeDebugTarget () throws IOException {
		try (PrintWriter debug = new PrintWriter (Files.newOutputStream (pathDebugTarget))) {
			for (Entry<Long, UniHash> target1 : targetOffset2Hash.entrySet ()) {
				debug.format ("%d\t%s%n", target1.getKey (), target1.getValue ());
			}
		}
	}

	//------------------------------------------------------------------------
	private void writeDebugDiff () throws IOException {
		try (PrintWriter debug = new PrintWriter (Files.newOutputStream (pathDebugDiff, WRITE))) {
			int total = 0;
			for (Entry<Long, UniHash> target1 : targetOffset2Hash.entrySet ()) {
				Long baseOffset = baseHash2Offset.get (target1.getValue ());
				debug.printf ("%d\t%s\t%d%n", target1.getKey (), target1.getValue (), baseOffset);
				if (baseOffset != null) {
					total++;
				}
			}
			debug.printf ("total: %,d%n", total);
		}
	}

}
