/*
	Copyright (c) 2012 by Derek Seabrooke

	This file is part of Puffman.

	Puffman is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Puffman is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Puffman.  If not, see <http://www.gnu.org/licenses/>.

	----

	Author:  Derek Seabrooke <dseabroo@electricgenesis.com>

	The following program demonstrates the use of Huffman encoding for encryption
	See Readme.txt for details
 */

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.BufferUnderflowException;
import java.nio.BufferOverflowException;
import java.util.zip.CRC32;
import java.math.*;
import bitfuns.*;

class Depuff {

	/*
		The following function decodes the file.  The index which is passed from the main should contain
		the position of the first prefix-free code in the actual encrypted file.  This might not start on
		a byte boundary.

		ByteBuffer input - cipher file
		ByteBuffer output - plain file
		Node tree - Huffman tree to decode from
		int index - input file position
		int rembits - remaining bits passed from main method these bits have been decrypted
		int remsize - number of bits in rembits
		int targetsize - expected size of output file
		byte charwidth - word size of output file

		return crc of output file
	*/
	private static int decodeFile(ByteBuffer input, ByteBuffer output, Node tree, int index, BitSet rembits, int remsize, int targetsize, byte charwidth) {
		int outdex=0, bufsize, readsize, readpos=input.position(), bitlen[] = {remsize, 0};
		BitSet instream, outstream = new BitSet(), bitdat[] = new BitSet[2];
		byte crypt[], plain[];
		Runtime rt = Runtime.getRuntime();
		CRC32 crc = new CRC32();
		long writpos=0;

		// allocates enough memory to read entire file
		// if not enough memory is available it will allocate as much as it can
		// and process the file in chunks
		bufsize = (int)rt.freeMemory();
		readsize = bufsize;
		if (input.capacity() - readpos < bufsize){
			readsize = input.capacity()-readpos;
			// the below adds extra bytes subtracted in loop below this ensures processing to EOF
			// unfortunately it wastes a few bytes extra memory which is meaningless on modern computers
			bufsize = readsize+(int)java.lang.Math.pow(2,charwidth);
		}
		crypt = new byte[bufsize];

		// main loop
		while (input.capacity() > readpos) {
			try {
				input.get(crypt,0,Math.min(input.remaining(),readsize));
				instream = Bitfun.fromByteArray(crypt);

				// if the file is being processed in chunks it combines any outstanding bits from the last pass
				bitlen[1] = bufsize*8;
				bitdat[0] = rembits;
				bitdat[1] = instream;
				instream = Bitfun.combinebits(bitdat,bitlen);

				// loops until end of chunk or until whole file is decrypted
				while((index < (bufsize-java.lang.Math.pow(2,charwidth))*8+bitlen[0]) && (writpos/8 < targetsize)) {
					index = tree.decode(instream,outstream,index,outdex,charwidth);
					outdex += charwidth;  // bits in outstream
					writpos += charwidth;  // total bits processed in output
					// when outstream reaches a byte boundary writes to file
					if (outdex%8 == 0) {
						plain = new byte[outdex/8];
						plain = Bitfun.toByteArray(outstream,plain);
						outstream.clear();
						output.put(plain);
						crc.update(plain);
						outdex = 0;
					}
				}

				// prepares for next pass
				// remaining bits will be combined
				bitlen[0] = bufsize*8+bitlen[0]-index;
				if (instream.length()>index) 
					rembits = instream.get(index,instream.length());  // saves remaining bits for next pass
				readpos += readsize;
				index=0;
			} catch (OutOfMemoryError e) {
				// if for some reason it is unable to allocate desired memory chunk
				// it tries again this time only asking for half
				bufsize = bufsize / 2;
				readsize = bufsize;
				input.position(readpos);  // sets positions back to correct point in files
				crypt = new byte[bufsize];
				System.err.printf("Setting bufsize to %d bytes\n",bufsize);
			} catch (BufferOverflowException e) {
				// this should not happen
				System.err.println("Buffer overflow at writpos = "+writpos+"; outdex = "+outdex);
				System.exit(1);
			}
		}

		// the following code writes any extra data that remains to be written
		if (output.position() < targetsize) {
			plain = new byte[targetsize-output.position()];
			plain = Bitfun.toByteArray(outstream,plain);
			output.put(plain);
			crc.update(plain);
		}
		return (int)crc.getValue();
	}

	/*
		main method

		String[] args - command line arguments
	*/
	public static void main(String[] args)
	throws IOException {
		FileChannel input, output;
		ByteBuffer inbuf, outbuf;
		byte charwidth, dat[], kdat[], filesig[] = new byte[4], cryptwidths[];
		int outputsize, crc1, crc2, bufsize;
		BitSet bitdat, treeidbits, contentbits, contenttab[];
		String targetsig = "PUFF";
		int treeidsize;
		BigInteger treeid;
		Node tree;

		if (args.length == 3) {
			// gets key data
			kdat = Key.getKey(args[2]);

			// opens input file
			input = new FileInputStream(args[0]).getChannel();
			inbuf = input.map(FileChannel.MapMode.READ_ONLY, 0, (int)input.size());
			input.close();

			// verifies file signature
			inbuf.get(filesig);
			if (!targetsig.equals(new String(filesig))) {
				System.err.println("Invalid file signature.");
				System.exit(1);
			}

			// reads file header
			charwidth = inbuf.get();
			outputsize = inbuf.getInt();
			crc1 = inbuf.getInt();

			// gets tree and ordered content
			treeidsize = Node.maxTrees((int)java.lang.Math.pow(2,charwidth)-1).bitLength();

			// allocates at least enough to hold tree description and key
			bufsize = Math.max(kdat.length,(treeidsize+(int)java.lang.Math.pow(2,charwidth)*charwidth)/8+1);
			if (bufsize > inbuf.capacity()-inbuf.position())
				bufsize = inbuf.capacity();
			dat = new byte[bufsize];
			inbuf.get(dat);

			// this is where decryption takes place
			for (int i=0; i < kdat.length; i++)
				dat[i] ^= kdat[i];
			bitdat = Bitfun.fromByteArray(dat);
			dat = null;

			// extract tree id and ordered content
			treeidbits = bitdat.get(0,treeidsize);
			treeid = Bitfun.toBigInteger(treeidbits);
			contentbits = bitdat.get(treeidsize,treeidsize+(int)java.lang.Math.pow(2,charwidth)*charwidth);
			contenttab = new BitSet[(int)java.lang.Math.pow(2,charwidth)];
			for (int i=0; i < contenttab.length; i++) {
				contenttab[i] = contentbits.get(i*charwidth,(i+1)*charwidth);
			}

			// reconstructs tree
			tree = new Node(new BitSet(),0);
			tree.buildTree(treeid,(int)java.lang.Math.pow(2,charwidth)-1);
			tree.putContent(contenttab);

			// writes output file
			output = new FileOutputStream(args[1]).getChannel();
			outbuf = ByteBuffer.allocateDirect(outputsize);  // supports larger files

			crc2 = decodeFile(inbuf,outbuf,tree,treeidsize+(int)java.lang.Math.pow(2,charwidth)*charwidth,bitdat,bufsize*8,outputsize,charwidth);
			if (crc1 != crc2)
				System.err.println("CRC mismatch - confirm encryption key");
			else
				System.out.println("CRC match");
			outbuf.rewind();
			output.write(outbuf);
			output.close();
		} else {
			System.err.println("Usage:  java Depuff <infile> <outfile> <keyfile>");
		}
	}
}
