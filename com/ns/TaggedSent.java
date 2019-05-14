package com.ns;

import java.util.Vector;

import com.ns.NSChunker.NSChunkerChunk;

public class TaggedSent {
	public String text = null;
	public String idxPos = null;
	public Vector<NSChunkerWord> words = new Vector<NSChunkerWord>();
	public Vector<NSChunkerChunk> chunks = null;
	public Vector<NSChunkerChunk> extracts = null;
}
