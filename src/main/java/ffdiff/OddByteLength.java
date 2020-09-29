package ffdiff;

import java.io.IOException;
import java.io.InputStream;
import java.nio.*;
import java.util.*;

import org.apache.commons.lang3.ArrayUtils;

/** Odd byte length integer data types */
public class OddByteLength {

	//------------------------------------------------------------------------
	/** 3 bytes (24 bits) big-endian integer */
	public static byte[] int3BE (final int i) {
		ByteBuffer b = ByteBuffer.allocate (Integer.BYTES).order (ByteOrder.BIG_ENDIAN).putInt (i);
		return Arrays.copyOfRange (b.array (), 1, Integer.BYTES);
	}

	//------------------------------------------------------------------------
	/** 3 bytes (24 bits) big-endian integer */
	public static int int3BE (final byte[] b) {
		if (b == null || b.length != 3) {
			return 0;
		}
		byte[] b4 = ArrayUtils.addAll (new byte[] { 0 }, b);
		return ByteBuffer.wrap (b4).getInt ();
	}

	//------------------------------------------------------------------------
	/** Read 3 bytes (24 bits) big-endian integer from stream */
	public static int readInt3BE (final InputStream in) throws IOException {
		Objects.requireNonNull (in);
		byte[] b3 = new byte [3];
		in.read (b3);
		return int3BE (b3);
	}

	//------------------------------------------------------------------------
	/** 7 bytes (56 bits) integer in big-endian */
	public static long int7BE (final byte[] b) {
		if (b == null || b.length != 7) {
			return 0;
		}
		byte[] b8 = ArrayUtils.addAll (new byte[] { 0 }, b);
		return ByteBuffer.wrap (b8).getLong ();
	}

	//------------------------------------------------------------------------
	/** Read 7 bytes (56 bits) big-endian integer from stream */
	public static long readInt7BE (final InputStream in) throws IOException {
		Objects.requireNonNull (in);
		byte[] b7 = new byte [7];
		in.read (b7);
		return int7BE (b7);
	}

}
