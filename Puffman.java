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
import java.lang.Math;
import java.nio.BufferUnderflowException;
import java.util.zip.CRC32;
import java.math.*;
import bitfuns.*;

public class Puffman
{
	// change this value to decide encoded character width in bits
	// maximum 7
	private static final byte charwidth = 7;

	/*
		Encodes file according to key and symbol table

		byte kdat[] - key data
		ByteBuffer plainbuf - input file
		FileChannel cipherfile - output file
		BitSet crypt - bits already read from input file
		int cryptpos - start position in crypt
		BitSet bitdat[] - symbol table
		Byte lens[] - lengths of symbol table entries

		return CRC of input file
	*/
	private static int encodeFile(byte kdat[], ByteBuffer plainbuf, FileChannel cipherfile, BitSet crypt, int cryptpos, BitSet bitdat[], byte lens[]) throws IOException {
		int testsize, remsize, origcryptpos, bufsize, readsize, writ, kpos=0, filepos=0;
		byte cipher[], dat[], ch[];
		Runtime rt = Runtime.getRuntime();
		ByteBuffer cipherbuf = ByteBuffer.allocate(0);
		CRC32 crc = new CRC32();
		BitSet datbits;

		// allocates enough memory to read entire file
		// if not enough memory is available it will allocate as much as it can
		// and process the file in chunks
		bufsize = (int)rt.freeMemory();
		bufsize -= bufsize%charwidth;  // this ensures that no input character is split between chunks
		if (plainbuf.capacity() - filepos < bufsize)
			bufsize = plainbuf.capacity()-filepos+1;  // ensures processing to EOF
		dat = new byte[bufsize];

		// process chunks
		while (plainbuf.capacity() >= filepos) {
			plainbuf.position(filepos);
			remsize = plainbuf.remaining();
			readsize = Math.min(remsize,bufsize);
			plainbuf.get(dat,0,readsize);
			// in the final chunk one byte is added to remaining size to ensure processing to EOF
			testsize = Math.min(remsize+1,bufsize);
			origcryptpos = cryptpos;  // saves original position to facilitate rollback
			try {
				datbits = Bitfun.fromByteArray(dat);
				for (int i=0; i < testsize*8/charwidth; i++) {
					ch = Bitfun.toByteArray(datbits.get(i*charwidth,charwidth*(i+1)));
					for (int j=0; j < lens[ch[0]&0xFF]; j++) {
						if (bitdat[ch[0]&0xFF].get(j)) crypt.set(cryptpos);
						cryptpos++;
					}
				}
				crc.update(dat,0,readsize);
				cipher = new byte[cryptpos/8];
				cipher = Bitfun.toByteArray(crypt,cipher);
				// this does the encryption
				while (kpos < kdat.length) {
					cipher[kpos] ^= kdat[kpos];
					kpos++;
				}
				cipherbuf = ByteBuffer.wrap(cipher);
			} catch (OutOfMemoryError e) {
				cryptpos = origcryptpos;  // roll back cryptpos
				bufsize = bufsize / 2;
				dat = new byte[bufsize];
				System.err.printf("Setting bufsize to %d bytes\n",bufsize);
			}

			writ = cipherfile.write(cipherbuf);

			filepos += bufsize;

			// setup for next pass
			if (cryptpos%8 != 0)
				crypt = crypt.get(cryptpos-(cryptpos%8),cryptpos);  // removes written bits
			else
				crypt.clear();
			cryptpos %= 8;  // resume from bits remaining
		}
		// when it reaches end it still has to output any outstanding bits
		cipher = Bitfun.toByteArray(crypt);
		cipherbuf = ByteBuffer.wrap(cipher);
		writ = cipherfile.write(cipherbuf);
		return (int)crc.getValue();
	}

	/*
		main method

		String[] args - command line argument list
	*/
	public static void main(String[] args)
		throws IOException
	{
		Node tree;
		FileChannel input, output;
		byte dat[], kdat[], codewidths[] = new byte[(int)java.lang.Math.pow(2,charwidth)];
		BitSet bittab, treeidbits, codetab, bitstream, contenttab[], tab[] = new BitSet[2], code[] = new BitSet[(int)java.lang.Math.pow(2,charwidth)];
		ByteBuffer inbuf, outbuf;
		int inputsize, crc, headsize, bufsize = Integer.MAX_VALUE, tablen[] = new int[2];
		Runtime rt = Runtime.getRuntime();
		String filesig = "PUFF";
		BigInteger treeid;

		if (args.length == 2) {
			System.out.println("Encrypting with "+charwidth+" bit wordsize");

			// gets key data
			kdat = Key.getKey(args[1]);

			// opens input file
			input = new FileInputStream(args[0]).getChannel();
			inbuf = input.map(FileChannel.MapMode.READ_ONLY, 0, (int)input.size());
			input.close();

			// if the file is too big to read in a single chunk huffman table will be based only on first chunk
			// in theory this could fail if a symbol occurs in the file that was not in the first chunk, however
			// this seems like a remote posibility in practice.
			inputsize = inbuf.capacity();
			if (inputsize < bufsize)
				bufsize = inputsize;
			if (rt.freeMemory() < bufsize)
		  		bufsize = (int)rt.freeMemory();
			dat = new byte[bufsize];
			inbuf.get(dat);

			// convert to bitstream
			bitstream = Bitfun.fromByteArray(dat);
			dat = null;
			tree = buildTree(bitstream);

			// calculates size of header
			headsize = filesig.getBytes().length+(Integer.SIZE*2+Byte.SIZE)/8;

			// opens output file
			output = new FileOutputStream(args[0]+".puf").getChannel();

			// generates codes
			Node.genCodes(tree,code,codewidths);

			// gets content order
			contenttab = tree.getContent();
			codetab = Bitfun.combinebits(contenttab,charwidth);

			// gets tree number
			treeid = tree.getTreeNumber();
			treeidbits = Bitfun.fromBigInteger(treeid);

			// combine codewidthstab and codetab into single bitset
			tab[0] = treeidbits;
			tab[1] = codetab;
			tablen[0] = tree.maxTrees().bitLength();
			tablen[1] = (int)java.lang.Math.pow(2,charwidth)*charwidth;
			bittab = Bitfun.combinebits(tab,tablen);

			// writes code table then encrypted content
			output.position(headsize);
			crc = encodeFile(kdat, inbuf,output,bittab,tablen[0]+tablen[1],code,codewidths);
			outbuf = ByteBuffer.allocate(headsize);

			// puts file signature to header
			outbuf.put(filesig.getBytes());

			// puts character width to header
			outbuf.put(charwidth);

			// puts original file length to header
			outbuf.putInt(inputsize);

			// puts original file crc
			outbuf.putInt(crc);

			// writes file header
			outbuf.rewind();
			output.position(0);
			output.write(outbuf);
			output.close();
		} else {
			System.out.println("Usage:  java Puffman <infile> <keyfile>");
			System.out.println("In file will be encrypted in <infile>.puff");
		}
	}

	/*
		Generates Huffman tree based on bitset contents

		BitSet fileContents - contents of input file

		return root node
	*/
	private static Node buildTree(BitSet fileContents)
		throws IOException
	{
		byte ch[];
		int[] frequency = new int[(int)java.lang.Math.pow(2,charwidth)];  // frequency of character
		TreeSet<Node> trees = new TreeSet<Node>();  // ordered list of trees
		Random bias = new Random();

		// builds the frequency table for each character
		for (int i=0; i < fileContents.length()/charwidth; i++)
		{
			ch = Bitfun.toByteArray(fileContents.get(i*charwidth,i*charwidth+charwidth));
			++frequency[ch[0]&0xFF];
		}

		// Builds the initial trees
		for (byte i=0; (i&0xFF) < frequency.length; i++)
		{
			// the frequencies are biased by producing a pseudo random number up to the actual frquency value
			// in the hopes of producing less predictable tokens and token lengths
			Node n = new Node(i, bias.nextInt(frequency[i&0xFF]+1)+1);
			trees.add(n);
			// adds entire alphabet, even characters not used in message so that decrypter can calculate number
			// of nodes based soley on character width
		}

		// Huffman algoritm
		while (trees.size() > 1)
		{
			Node tree1 = (Node) trees.first();
			trees.remove(tree1);
			Node tree2 = (Node) trees.first();
			trees.remove(tree2);

			Node merged = new Node(tree1, tree2);
			trees.add(merged);
		}
		return trees.first();
	}
}
