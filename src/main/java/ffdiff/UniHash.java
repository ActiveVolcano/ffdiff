package ffdiff;

import java.util.*;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.Conversion;

/** Length + MD5 + SHA1 = Unique hash in practice */
public class UniHash implements Comparable<UniHash> {

	//------------------------------------------------------------------------
	private int length;
	private byte[] md5;
	private byte[] sha1;

	public int getLength () {
		return length;
	}
	public byte[] getMD5 () {
		return md5;
	}
	public byte[] getSHA1 () {
		return sha1;
	}

	//------------------------------------------------------------------------
	public UniHash (byte[] raw) {
		Objects.requireNonNull (raw);
		length = raw.length;
		md5  = DigestUtils.md5  (raw);
		sha1 = DigestUtils.sha1 (raw);
	}

	//------------------------------------------------------------------------
	public UniHash (byte[] raw, int from, int to) {
		this (raw != null && from == 0 && to == raw.length ?
			raw :
			Arrays.copyOfRange (raw, from, to));
	}

	//------------------------------------------------------------------------
	@Override public boolean equals (Object b) {
		if (this == b) {
			return true;
		}
		if (b == null || ! (b instanceof UniHash)) {
			return false;
		}
		UniHash b0 = (UniHash) b;
		return this.length == b0.getLength () &&
			Arrays.equals (this.md5, b0.getMD5 ()) &&
			Arrays.equals (this.sha1, b0.getSHA1 ());
	}

	//------------------------------------------------------------------------
	@Override public int hashCode () {
		return Conversion.byteArrayToInt (md5, 0, 0, 0, Integer.BYTES);
	}

	//------------------------------------------------------------------------
	@Override public String toString () {
		return String.format ("%d\t%s\t%s",
			length, Hex.encodeHexString (md5), Hex.encodeHexString (sha1));
	}

	//------------------------------------------------------------------------
	@Override public int compareTo (UniHash b) {
		int lengthb = b.getLength ();
		byte[] md5b = b.getMD5 ();
		if (length != lengthb) {
			return length - lengthb;
		} else for (int i = 0 ; i < length ; i++) {
			if (md5[i] != md5b[i]) {
				return md5[i] - md5b[i];
			}
		}
		return 0;
	}
}
