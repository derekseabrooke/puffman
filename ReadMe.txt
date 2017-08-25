                          Puffman Encryption Example

What is it?
-----------

Puffman is free software designed for non-commercial use.  It is 
designed only as an example of work by Electric Genesis Consulting & 
Research in the compiled Java language.  The Puffman Encryption Example 
is written by Derek Seabrooke and can process files of hundreds of 
megabytes in size depending on system resources.

The theory of information entropy is of great importance to both the 
field of data compression and encryption.  Although these goals are  
different, the entropy of the content encoded is a key consideration for 
both, as in both cases it determines the effectiveness of the process.

In the case of data compression, the entropy of the original content 
determines how compressible the content is.  The theory of data 
compression involves the removal of low entropy redundancies in the 
content by expressing the content in shorter high entropy symbols.  
The result will be a high entropy incompressible file.

In the field of cryptography, the cryptanalyst looks for redundancies 
and statistical imbalances in the encoding to try to guess what the 
original hidden content was.  A good cryptographic system will remove 
these clues by producing an output file with higher entropy - both 
types of encoding results in high entropy output files.

Huffman Encoding developed by David Huffman in the 1950's is re-purposed 
as an obfuscation method.  This is accomplished by encrypting the 
statistical model against a key file.  The goal of Puffman is 
obfuscation, not compression.  Although the output file may be smaller 
than the original, the statistical model has been biased so as to make 
it more difficult to guess.  This greatly reduces its effectiveness as a 
compression tool.

Puffman will never encrypt the same file the same way even with the same
key.  The output files will also vary in size on each attempt.

Latest Version
--------------

The latest version of this software can be obtained from 
<http://electricgenesis.com/portfolio.shtml>.

Compiling
---------

To compile, please download and install JDK 1.7 or later from 
<http://www.oracle.com/technetwork/java/javase/downloads/index.html>.

Operation
---------

To encrypt:  java Puffman <infile> <keyfile>

To decrypt:  java Depuff <outfile> <keyfile>

To generate key (on UNIX system):  head -c 7 < /dev/urandom > <keyfile>

Manifest
--------

Depuff.java - Puffman decryptor program

Puffman.java - Puffman encryptor program

Node.java - Huffman tree node data structure shared

Key.java - Key file data structure shared

example.key - Example key file

mystery.txt.puf - Example encrypted file see challege.txt

challenge.txt - Your challenge to crack mystery.txt.puf

copying.txt - license details

ReadMe.txt - This file

bitfuns/Bitfun.java


Licensing
---------

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

Cryptographic Software Notice
-----------------------------

This software has been designed for encryption.  The country in which 
you currently reside may have restrictions on the import, possession, 
use, and/or re-export to another country, of encryption software.  
BEFORE using any encryption software, please check your country's laws, 
regulations and policies concerning the import, possession, or use, 
and re-export of encryption software, to see if this is permitted.  
See <http://www.wassenaar.org/> for more information.

Contact
-------

Author Derek Seabrooke <dseabroo@electricgenesis.com>
