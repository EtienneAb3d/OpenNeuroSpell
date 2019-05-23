package com.ns;

import java.util.Vector;

import com.ns.NSToken.NSTOKEN_KIND;

public class NSTaggedSent {
	public String text = null;
	public String idxPos = null;
	public Vector<NSToken> tokens = new Vector<NSToken>();
	public Vector<NSChunk> chunks = null;
	public Vector<NSChunk> extracts = null;

	public String toTokenized() throws Exception {
		StringBuffer aSB = new StringBuffer();
		for(NSToken aT : tokens) {
			aSB.append(" ").append(aT.token);
		}
		return aSB.toString().replaceAll("  +", " ").trim();
	}
	
	static public NSTaggedSent fromTokenized(String aS) throws Exception {
		NSTaggedSent aTS = new NSTaggedSent();
		for(String aT : aS.replaceAll("  +", " ").trim().split(" ")) {
			NSToken aNST = new NSToken();
			aNST.token = aT;
			if(aT.matches("<[^>]*>")) {
				aNST.kind = NSTOKEN_KIND.MARK; 
			}
			aTS.tokens.add(aNST);
		}
		return aTS;
	}
}
