package com.ns;

import java.util.Vector;

public class NSChunk {
	public String chunk = null;
	public String pos = null;
	public String idxPos = null;
	public String posExt = null;
	public String tagExt = null;
	public boolean hasFem = false;
	public boolean hasPlur = false;
	public Vector<NSToken> tokens = new Vector<NSToken>();
	
	String toTokenized() throws Exception {
		StringBuffer aSB = new StringBuffer();
		for(NSToken aT : tokens) {
			aSB.append(aT.token+" ");
		}
		return aSB.toString().replaceAll("  +", " ").trim();
	}
}
