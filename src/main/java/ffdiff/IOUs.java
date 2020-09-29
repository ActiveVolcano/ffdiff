package ffdiff;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

/** Input & Output Utilities */
public class IOUs {

	//------------------------------------------------------------------------
	// Member constants
	private static final int sizeBulkCopyBuffer = 1024 * 1024;

	//------------------------------------------------------------------------
	public static byte[] readBytea (final DataInputStream in, final int length) throws IOException {
		assert length >= 0;
		byte[] bytea = new byte[length];
		in.readFully (bytea);
		return bytea;
	}

	//------------------------------------------------------------------------
	public static String readUTF8String (final DataInputStream in, final int length) throws IOException {
		if (in == null || length <= 0) {
			return "";
		}
		byte[] b = new byte [length];
		in.readFully (b);
		return new String (b, StandardCharsets.UTF_8);
	}

	//------------------------------------------------------------------------
	public static void copy (final RandomAccessFile in, final OutputStream out, final long offset, final int length)
	throws IOException {
		byte[] buffer = new byte [sizeBulkCopyBuffer];
		in.seek (offset);
		for (int sizeBulk = 0, sizeRemain = length ; sizeRemain > 0 ; sizeRemain -= sizeBulk) {
			sizeBulk = Integer.min (sizeBulkCopyBuffer, sizeRemain);
			in.readFully (buffer, 0, sizeBulk);
			out.write (buffer, 0, sizeBulk);
		}
	}

	//------------------------------------------------------------------------
	/** File POSIX Permissions: (LSB) - user group others | (MSB) - read write execute */
	public static byte[] getFilePermissions (final Path p) throws IOException {
		BitSet bits = new BitSet (16);
		if (p == null) {
			return bits.toByteArray ();
		}
		Set<PosixFilePermission> permissions = Files.getPosixFilePermissions (p);
		bits.set (6 , permissions.contains (PosixFilePermission.OWNER_READ));
		bits.set (5 , permissions.contains (PosixFilePermission.OWNER_WRITE));
		bits.set (4 , permissions.contains (PosixFilePermission.OWNER_EXECUTE));
		bits.set (10, permissions.contains (PosixFilePermission.GROUP_READ));
		bits.set (9 , permissions.contains (PosixFilePermission.GROUP_WRITE));
		bits.set (8 , permissions.contains (PosixFilePermission.GROUP_EXECUTE));
		bits.set (14, permissions.contains (PosixFilePermission.OTHERS_READ));
		bits.set (13, permissions.contains (PosixFilePermission.OTHERS_WRITE));
		bits.set (12, permissions.contains (PosixFilePermission.OTHERS_EXECUTE));
		return bits.toByteArray ();
	}

	//------------------------------------------------------------------------
	/** File POSIX Permissions: (LSB) - user group others | (MSB) - read write execute */
	public static Set<PosixFilePermission> parseFilePermissions (final byte[] b) throws IOException {
		Set<PosixFilePermission> permissions = new HashSet<PosixFilePermission> (9 * 2);
		if (b == null || b.length != 2) {
			return permissions;
		}
		BitSet bits = BitSet.valueOf (b);
		Map<Integer, PosixFilePermission> table = new HashMap<Integer, PosixFilePermission> (9 * 2);
		table.put (6 , PosixFilePermission.OWNER_READ);
		table.put (5 , PosixFilePermission.OWNER_WRITE);
		table.put (4 , PosixFilePermission.OWNER_EXECUTE);
		table.put (10, PosixFilePermission.GROUP_READ);
		table.put (9 , PosixFilePermission.GROUP_WRITE);
		table.put (8 , PosixFilePermission.GROUP_EXECUTE);
		table.put (14, PosixFilePermission.OTHERS_READ);
		table.put (13, PosixFilePermission.OTHERS_WRITE);
		table.put (12, PosixFilePermission.OTHERS_EXECUTE);
		table.forEach ((index, permission1) -> {
			if (bits.get (index)) {
				permissions.add (permission1);
			}
		});
		return permissions;
	}

	//------------------------------------------------------------------------
	/** File Windows Attributes: (LSB) Read-only Archive System Hidden */
	public static byte[] getFileAttributes (final Path p) throws IOException {
		BitSet bits = new BitSet (8);
		if (p == null) {
			return bits.toByteArray ();
		}
		DosFileAttributes attrs = Files.readAttributes(p, DosFileAttributes.class);
		bits.set (0, attrs.isReadOnly ());
		bits.set (1, attrs.isArchive ());
		bits.set (2, attrs.isSystem ());
		bits.set (3, attrs.isHidden ());
		return bits.toByteArray ();
	}

	//------------------------------------------------------------------------
	public static void setFileAttributes (final Path p, final DosFileAttributes attributes) throws IOException {
		if (p == null || attributes == null) {
			return;
		}
		DosFileAttributeView view = Files.getFileAttributeView (p, DosFileAttributeView.class);
		view.setReadOnly (attributes.isReadOnly ());
		view.setArchive  (attributes.isArchive ());
		view.setSystem   (attributes.isSystem ());
		view.setHidden   (attributes.isHidden ());
	}

	//------------------------------------------------------------------------
	/** File Windows Attributes: (LSB) Read-only Archive System Hidden */
	public static class ParsedFileAttributes implements DosFileAttributes {

		final boolean isReadOnly, isArchive, isSystem, isHidden;
		public ParsedFileAttributes (final byte b) {
			BitSet bits = BitSet.valueOf (new byte[] { b });
			isReadOnly  = bits.get (0);
			isArchive   = bits.get (1);
			isSystem    = bits.get (2);
			isHidden    = bits.get (3);
		}

		@Override public boolean isReadOnly () {
			return isReadOnly;
		}
		@Override public boolean isArchive () {
			return isArchive;
		}
		@Override public boolean isHidden () {
			return isHidden;
		}
		@Override public boolean isSystem () {
			return isSystem;
		}
		@Override public FileTime creationTime () {
			return null;
		}
		@Override public Object fileKey () {
			return null;
		}
		@Override public boolean isDirectory () {
			return false;
		}
		@Override public boolean isOther () {
			return false;
		}
		@Override public boolean isRegularFile () {
			return true;
		}
		@Override public boolean isSymbolicLink () {
			return false;
		}
		@Override public FileTime lastAccessTime () {
			return null;
		}
		@Override public FileTime lastModifiedTime () {
			return null;
		}
		@Override public long size () {
			return 0;
		}

	}

}
