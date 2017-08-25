/*
 * Copyright (c) 2011-2012 by Derek Seabrooke
 * Not for commercial use
 *
 * Derek Seabrooke <dseabroo@electricgenesis.com>
 * 
 * This class is basically useless - it was implemented in the event that key file
 * would contain file structure such as a CRC validation but current key files 
 * contain no such formatting
 */

import java.util.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.io.*;

public class Key{

	// verifies and returns key
	protected static byte[] getKey(String filename) 
	throws IOException {
		FileChannel key;
		byte kdat[];
		ByteBuffer keybuf;

		try {
			key = new FileInputStream(filename).getChannel();
			keybuf = key.map(FileChannel.MapMode.READ_ONLY, 0, (int)key.size());
			key.close();
			kdat = new byte[keybuf.capacity()];
			keybuf.get(kdat);
			return kdat;
		} catch(FileNotFoundException e) {
			System.err.println(filename+" not found");
			System.exit(3);
		}
		return null;
	}
}
