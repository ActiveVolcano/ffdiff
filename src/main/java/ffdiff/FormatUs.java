package ffdiff;

import java.io.*;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

import org.apache.commons.lang3.time.DateFormatUtils;

/** Format Utilities */
public class FormatUs {

	//------------------------------------------------------------------------
	/** Timestamp -> ISO 8601 String*/
	public static String iso8601 (long timestamp) {
		Date d = new Date (timestamp);
		return DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format (d);
	}

	//------------------------------------------------------------------------
	public static IOException formatIOException (String format, Object... args) {
		return new IOException (String.format (format, args));
	}

	//------------------------------------------------------------------------
	public static String formatFilePermissions (final Set<PosixFilePermission> permissions) {
		StringBuilder s = new StringBuilder ();
		if (permissions == null) {
			return s.toString ();
		}
		Map<PosixFilePermission, String> table = new LinkedHashMap<PosixFilePermission, String> (9 * 2);
		table.put (PosixFilePermission.OWNER_READ,      "r");
		table.put (PosixFilePermission.OWNER_WRITE,     "w");
		table.put (PosixFilePermission.OWNER_EXECUTE,   "x");
		table.put (PosixFilePermission.GROUP_READ,      "r");
		table.put (PosixFilePermission.GROUP_WRITE,     "w");
		table.put (PosixFilePermission.GROUP_EXECUTE,   "x");
		table.put (PosixFilePermission.OTHERS_READ,     "r");
		table.put (PosixFilePermission.OTHERS_WRITE,    "w");
		table.put (PosixFilePermission.OTHERS_EXECUTE,  "x");
		table.forEach ((permission1, s1) -> s.append (permissions.contains (permission1) ? s1 : "-"));
		return s.toString ();
	}

	//------------------------------------------------------------------------
	public static String formatFileAttributes (final DosFileAttributes attributes) {
		StringBuilder s = new StringBuilder ();
		if (attributes == null) {
			return s.toString ();
		}
		s.append (attributes.isReadOnly () ? "+" : "-");
		s.append ("R ");
		s.append (attributes.isArchive () ? "+" : "-");
		s.append ("A ");
		s.append (attributes.isSystem () ? "+" : "-");
		s.append ("S ");
		s.append (attributes.isHidden () ? "+" : "-");
		s.append ("H");
		return s.toString ();
	}

}
