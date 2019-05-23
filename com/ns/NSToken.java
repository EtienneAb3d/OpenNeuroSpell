package com.ns;

public class NSToken {
	public enum NSTOKEN_KIND {UNDEF,MARK};

	public NSTOKEN_KIND kind = NSTOKEN_KIND.UNDEF;
	public String token = null;
	public String lemma = null;
	public String pos = null;
	public String posOrig = null;
	public String tag = null;
	public boolean doNotPrint = false;
	
	public NSToken clone() {
		NSToken aT = new NSToken();
		aT.kind = kind;
		aT.token = token;
		aT.lemma = lemma;
		aT.pos = pos;
		aT.posOrig = posOrig;
		aT.tag = tag;
		return aT;
	}
}
