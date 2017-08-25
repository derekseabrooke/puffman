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
	along with Foobar.  If not, see <http://www.gnu.org/licenses/>.

	----

	Author:  Derek Seabrooke <dseabroo@electricgenesis.com>

	The following program demonstrates the use of Huffman encoding for encryption
	See Readme.txt for details
 */

import java.io.*;
import java.util.*;
import java.math.*;
import bitfuns.*;

class Node implements Comparable
{
	private BitSet 	content;  // symbol node represents
	private int	value;  // used in statistical model
	private Node 	left;
	private Node 	right;

	/* Constructors */

	protected Node(BitSet content, int value)
	{
		this.content = content;
		this.value = value;
	}

	protected Node(byte content, int value) {
		byte cont[] = {content};

		this.content = Bitfun.fromByteArray(cont);
		this.value = value;
	}

	protected Node(BigInteger content, int value) {
		this(Bitfun.fromBigInteger(content),value);
	}

	protected Node(Node left, Node right)
	{
		// Assumes that the left tree is lowest
		this.content  	= (Bitfun.toByteArray(left.content)[0] < Bitfun.toByteArray(right.content)[0]) ? left.content : right.content;
		this.value    	= left.value + right.value;
		this.left	= left;
		this.right    	= right;
	}

	/* Compare To function */

	public int compareTo(Object arg)
	{
		Node other = (Node) arg;

		// Content value has priority and then the lowest character
		if (this.value == other.value)
			return Bitfun.toByteArray(this.content)[0]-Bitfun.toByteArray(other.content)[0];
		else
			return this.value-other.value;
	}

	/* get prefix-free lengths & codes themselves */

	private void genCode(int len, BitSet code, BitSet codes[], byte[] codewidths)
	{
		BitSet newCode;
		byte cont = Bitfun.toByteArray(content)[0];

		if ((left==null) && (right==null)) {
			codes[cont] = code;
			codewidths[cont] = (byte)len;
		}

		if (left != null) {
			newCode = (BitSet)code.clone();
			left.genCode(len+1, newCode, codes, codewidths);
		}
		if (right != null) {
			newCode = (BitSet)code.clone();
			newCode.set(len);
			right.genCode(len+1, newCode, codes, codewidths);
		}
	}

	protected static void genCodes(Node tree, BitSet codes[], byte[] codewidths)
	{
		tree.genCode(0, new BitSet(), codes, codewidths);
	}

	/* Tree enumeration functions */

	private static BigInteger factorial(int x) {
		BigInteger n = BigInteger.ONE;
	        for (int i=1; i<=x; i++) {
        		n = n.multiply(BigInteger.valueOf(i));
	        }

		return n;
	}

	private static BigInteger catalan(int x) {
		return factorial(2*x).divide(factorial(x+1).multiply(factorial(x)));
	}

	protected int nodeCount() {
		int nodes=0;

		if ((left!=null) || (right!=null))
			nodes = 1;

                if (left != null)
                        nodes += left.nodeCount();
                if (right != null)
                        nodes += right.nodeCount();

		return nodes;
	}

	protected BigInteger maxTrees() {
		int nodes;

		nodes = this.nodeCount();
		return catalan(nodes);
	}

	protected static BigInteger maxTrees(int nodes) {
		return catalan(nodes);
	}

	protected BigInteger getTreeNumber() {
		BigInteger treeid = BigInteger.ZERO, leftid, rightid;
		int leftnodes, rightnodes, totalnodes;

		// recurses through branches to pinpoint minor part of tree number
		if (left != null) {
			leftnodes = left.nodeCount();
			leftid = left.getTreeNumber();
		}
		else {
			leftnodes = 0;
			leftid = BigInteger.ZERO;
		}
		if (right != null) {
			rightnodes = right.nodeCount();
			rightid = right.getTreeNumber();
		}
		else {
			rightnodes = 0;
			rightid = BigInteger.ZERO;
		}
		totalnodes = leftnodes + rightnodes;

		// this is the calculation for the Tree ID
		treeid = leftid.multiply(catalan(rightnodes)).add(rightid);

		// calculates the major part of the tree number based on the known number of nodes on the left and right
		for (int i=0; i < leftnodes; i++) {
			treeid = treeid.add(catalan(i).multiply(catalan(totalnodes-i)));
		}

		return treeid;
	}

	protected void buildTree(BigInteger treeid, int totalnodes) {
		BigInteger childid[], number=BigInteger.ZERO, newnumber=BigInteger.ZERO;
		int leftnodes=0, rightnodes;

		// based on tree number passed, pinpoints number of nodes on left and right
		for (int i=0; newnumber.compareTo(treeid) <= 0; i++) {
			number = newnumber;
			newnumber = number.add(catalan(i).multiply(catalan(totalnodes-i-1)));
			leftnodes = i;
		}
		rightnodes = totalnodes - leftnodes -1;

		// this breaks it down by subtracting the number derived above then finds 
		// the left and right id which are in index 0 and 1 respectively
		childid = treeid.subtract(number).divideAndRemainder(catalan(rightnodes));

		if (leftnodes > 0) {
			left = new Node(new BitSet(),0);
			left.buildTree(childid[0],leftnodes);
		} else
			left = new Node(new BitSet(),0);

		if (rightnodes > 0) {
			right = new Node(new BitSet(),0);
			right.buildTree(childid[1],rightnodes);
		} else
			right = new Node(new BitSet(),0);
	}

	/* Traverse tree to get order of content - traversal order doesn't matter as long as it consistent */

	private int getContent(BitSet orderedlist[], int index)
	{
		if ((left==null) && (right==null)) {
			orderedlist[index] = content;
			return index+1;
		}

		if (left != null)
			index = left.getContent(orderedlist,index);
		if (right != null)
			index = right.getContent(orderedlist,index);

		return index;
	}

	protected BitSet[] getContent() {
		BitSet orderedlist[] = new BitSet[this.nodeCount()+1];

		this.getContent(orderedlist,0);
		return orderedlist;
	}

	private int putContent(BitSet orderedlist[], int index)
	{
		if ((left==null) && (right==null)) {
			content = orderedlist[index];
			return index+1;
		}

		if (left != null)
			index = left.putContent(orderedlist,index);
		if (right != null)
			index = right.putContent(orderedlist,index);

		return index;
	}

	protected void putContent(BitSet orderedlist[]) {
		this.putContent(orderedlist,0);
	}

	/* decode */

	protected int decode(BitSet inbitstream, BitSet outbitstream, int index, int outdex, byte charwidth) {
		Node dex = this;

		while((dex.left!=null) && (dex.right!=null)) {
			if (inbitstream.get(index))
				dex = dex.right;
			else
				dex = dex.left;
			index++;
		}

		for (int i=0; i < charwidth; i++)
			if (dex.content.get(i))
				outbitstream.set(i+outdex);

		return index;
	}
}
