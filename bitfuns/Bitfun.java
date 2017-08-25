/*
	Copyright (c) 2012 by Derek Seabrooke

	This package is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.

	----

	Author:  Derek Seabrooke <dseabroo@electricgenesis.com>

	Functions for conversion between BitSets and other data types
 */

package bitfuns;

import java.util.*;
import java.math.*;

public class Bitfun {

	// combine bits
	public static BitSet combinebits(BitSet bitdat[], int lens)
	// all bits the same size
	 {
		int index = 0;
		BitSet bitstream = new BitSet();

		for (int i=0; i < bitdat.length; i++)
			for (int j=0; j < lens; j++) {
				if (bitdat[i].get(j)) bitstream.set(index);
				index++;
			}

		return bitstream;
	}

	public static BitSet combinebits(BitSet bitdat[], int lens[]) 
	// passes array of lengths
	{
		int index = 0;
		BitSet bitstream = new BitSet();

		for (int i=0; i < bitdat.length; i++)
			for (int j=0; j < lens[i]; j++) {
				if (bitdat[i].get(j)) bitstream.set(index);
				index++;
			}

		return bitstream;
	}

	public static BitSet combinebits(BitSet bitdat[], byte lens[])
	// lengths of of type byte
	 {
		int intLens[] = new int[lens.length];

		for (int i=0; i < lens.length; i++)
			intLens[i] = lens[i];

		return combinebits(bitdat, intLens);
	}


	public static BitSet combinebits(BitSet bitdat[], Byte[] lens) {
		byte[] byteArray = new byte[lens.length];

		for (int i=0; i < lens.length; i++)
			byteArray[i] = lens[i].byteValue();

		return combinebits(bitdat, byteArray);
	}

	// little endian
	public static byte[] toByteArray(BitSet bits) {
		return toByteArray(bits,8); // default to 8-bits
	}

	public static byte[] toByteArray(BitSet bits, byte[] bytes) {
		return toByteArray(bits,8,bytes); // default to 8-bits
	}

	// if no output array specified it allocates one just big enough
	public static byte[] toByteArray(BitSet bits, int charwidth) {
	    byte[] bytes = new byte[(bits.length()-1)/charwidth+1];

	    return toByteArray(bits, charwidth, bytes);
	}

	public static byte[] toByteArray(BitSet bits, int charwidth, byte[] bytes) {
	    for (int i=0; i < bytes.length*8; i++) {
		if (bits.get(i)) {
		    bytes[i/charwidth] |= 1<<(i%charwidth);
		}
	    }
	    return bytes;
	}

	public static BigInteger toBigInteger(BitSet bits) {
		BigInteger number = BigInteger.ZERO;

		for (int i=0; i < bits.size(); i++)
			if (bits.get(i))
				number = number.setBit(i);

		return number;
	}

	public static BitSet fromBigInteger(BigInteger number) {
		BitSet bits = new BitSet();

		for (int i=0; i < number.bitLength(); i++)
			if (number.testBit(i))
				bits.set(i);

		return bits;
	}

	// little endian
	public static BitSet fromByteArray(byte[] bytes) {
		return fromByteArray(bytes,8); // default to 8-bits
	}

	public static BitSet fromByteArray(byte[] bytes, int charwidth) {
	    BitSet bits = new BitSet();
	    for (int i=(bytes.length)*charwidth-1; i >= 0; i--) {
		if ((bytes[i/charwidth]&(1<<(i%charwidth))) > 0) {
		    //System.out.println("setting bit "+i);
		    bits.set(i);
		}
	    }
	    return bits;
	}

	public static BitSet fromByteArray(Byte[] bytes) {
		return fromByteArray(bytes, 8); // default to 8-bits
	}

	public static BitSet fromByteArray(Byte[] bytes, int charwidth) {
		byte[] byteArray = new byte[bytes.length];

		for (int i=0; i < bytes.length; i++)
			byteArray[i] = bytes[i].byteValue();

		return fromByteArray(byteArray, charwidth);
	}
}
