package com.ns;

import java.util.Vector;

import com.ns.NSToken.NSTOKEN_KIND;

public class NSAligner {
	static final boolean _DEBUG = false;

	static double COST_INCREDIBLE = 1000000;
	
	class Pair{
		NSToken t1 = null;
		NSToken t2 = null;
	}
	
	NSTaggedSent fusPos(NSTaggedSent aTS1,NSTaggedSent aTS2,NSTaggedSent aTS3) throws Exception {
		Vector<Pair> aP12s = align(aTS1, aTS2);
		Vector<Pair> aP13s = align(aTS1, aTS3);
		
		NSTaggedSent aFused = new NSTaggedSent();
		int aP13idx = 0;
		for(int p = 0;p < aP12s.size();p++) {
			Pair aP12 = aP12s.elementAt(p);

			//Must found a P13 match
			Pair aP13 = null;
			if(aP12.t1 != null && aP13idx < aP13s.size()) {
				while(aP13 == null || aP13.t1 == null || !aP12.t1.token.equals(aP13.t1.token)){
					aP13 = aP13s.elementAt(aP13idx);
					aP13idx++;
				}
			}

			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("AL:"
					+ (aP12.t1!=null ? aP12.t1.token+"\tPOS("+aP12.t1.pos+")":"????")
					+ "\t/\t"
					+ (aP12.t2!=null ? aP12.t2.token+"\tPOS("+aP12.t2.pos+")":"????")
					+ "\t|\t"
					+ (aP12.t1!=null ? "LEM("+aP12.t1.lemma + ") TAG(" + aP12.t1.tag+")":"????")
					+ "\t/\t"
					+ (aP13 != null && aP13.t2 != null ? aP13.t2.token + " POS("+aP13.t2.pos+") LEM("+ aP13.t2.lemma + ") TAG(" + aP13.t2.tag+")":"????")
					);
			}

			if(aP12.t1 == null) {
				//Ignore if the reference isn't there
				continue;
			}

			NSToken aT = aP12.t1.clone();
			aFused.tokens.add(aT);
			aT.posOrig = aP12.t1.pos;
			
			if(aP13.t2 != null) {
				//Enrich tag
				aT.tag += " LT: " + aP13.t2.tag;
			}
					
			if(aP12.t2 == null
					&& aP13.t2 == null) {
				//Nothing to match !?
				continue;
			}
			String aT2 = aP12.t2 == null ? null : aP12.t2.token;
			String aT3 = aP13.t2 == null ? null : aP13.t2.token;
			if(!aP12.t1.token.equals(aT2)
					&& !aP12.t1.token.equals(aT3)) {
				//Nothing to match !?
				continue;
			}
			String aPos2 = aP12.t2 == null ? null : aP12.t2.pos;
			String aPos3 = aP13.t2 == null ? null : aP13.t2.pos;
			if(aP12.t1.pos.equals(aPos2)) {
				//OK, keep like this
				continue;
			}
			//Merge diff
			aT.posOrig = aP12.t1.pos
					+ "_" + (aPos2 == null ? "???" : aPos2)
					+ "_" + (aPos3 == null ? "???" : aPos3.replaceAll(" +", "-"));

			if(aPos3 != null) {
				if(aPos2 != null) {
					if((" "+aPos3+" ").indexOf(" "+aP12.t1.pos+" ") >= 0
							&& (" "+aPos3+" ").indexOf(" "+aPos2+" ") < 0) {
						//Only 1 is possible, keep it
						continue;
					}
					if((" "+aPos3+" ").indexOf(" "+aPos2+" ") >= 0
							&& (" "+aPos3+" ").indexOf(" "+aP12.t1.pos+" ") < 0) {
						//Only 2 is possible, take it
						aT.pos = aP12.t2.pos;
						continue;
					}
				}
				if(aPos3.trim().indexOf(" ") < 0) {
					//Provide with a single possibility, take it !
					aT.pos = aPos3.trim().isEmpty()? "UNK" : aP13.t2.pos.trim();
					continue;
				}
			}
				
			//Keep only 1 and 2
			aT.pos = aP12.t1.pos
					+ (aPos2 == null ? "" : "_" + aPos2);
		}
		
		StringBuffer aTxtSB = new StringBuffer();
		StringBuffer aPosSB = new StringBuffer();
		for(int t = 0;t < aFused.tokens.size();t++) {
			NSToken aW = aFused.tokens.elementAt(t);
			aTxtSB.append(" "+aW.token);
			aPosSB.append(" "+t+","+t+aW.pos+" ");
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("FUSED: "+aW.token+"\t("+aW.pos+")\t"+aW.lemma+"\t"+aW.tag);
			}
		}
		aFused.text = aTxtSB.toString().replaceAll("  +", " ").trim();
		aFused.idxPos = aPosSB.toString();
		
		return aFused;
	}
	
	NSTaggedSent syncMarks1to2(NSTaggedSent aTS1,NSTaggedSent aTS2) throws Exception {
		Vector<Pair> aP12s = align(aTS1, aTS2);
		NSTaggedSent aFused = new NSTaggedSent();
		for(int p = 0;p < aP12s.size();p++) {
			Pair aP12 = aP12s.elementAt(p);
			if(aP12.t1 != null && aP12.t1.kind == NSTOKEN_KIND.MARK) {
				aFused.tokens.add(aP12.t1);
			}
			if(aP12.t2 != null) {
				aFused.tokens.add(aP12.t2);
			}
		}
		return aFused;
	}
	
	Vector<Pair> align(NSTaggedSent aTS1,NSTaggedSent aTS2) throws Exception {
		int[][] aChoices = new int[aTS1.tokens.size()+1][aTS2.tokens.size()+1];
		double[][] aCosts = new double[aTS1.tokens.size()+1][aTS2.tokens.size()+1];
		for(int x = 0;x<aTS1.tokens.size()+1;x++) {
			aChoices[x][0] = 1;//Left
			aCosts[x][0] = x;
		}
		for(int y = 0;y<aTS2.tokens.size()+1;y++) {
			aChoices[0][y] = 2;//Up
			aCosts[0][y] = y;
		}
		for(int x = 1;x<aTS1.tokens.size()+1;x++) {
			for(int y = 1;y<aTS2.tokens.size()+1;y++) {
				double aCost = cost(aTS1.tokens.elementAt(x-1),aTS2.tokens.elementAt(y-1));
				double aCost0 = aCosts[x-1][y-1]+aCost*0.99;
				double aCost1 = aCosts[x-1][y]+1;
				double aCost2 = aCosts[x][y-1]+1;
				if(aCost0 <= aCost1 && aCost0 <= aCost2) {
					aChoices[x][y] = 0;
					aCosts[x][y] = aCost0;
				}
				else if(aCost1 < aCost2) {
					aChoices[x][y] = 1;
					aCosts[x][y] = aCost1;
				}
				else {
					aChoices[x][y] = 2;
					aCosts[x][y] = aCost2;
				}
			}
		}

		int x = aTS1.tokens.size();
		int y = aTS2.tokens.size();
		Vector<Pair> aPs = new Vector<Pair>();
		while(x > 0 || y > 0) {
			Pair aP = new Pair(); 
			//		System.out.println("X="+x+" Y="+y+" Ch="+aChoices[x][y]+" Co="+aCosts[x][y]);
			if(aChoices[x][y] == 0) {
				aP.t1 = aTS1.tokens.elementAt(--x);
				aP.t2 = aTS2.tokens.elementAt(--y);
			}
			else if(aChoices[x][y] == 1) {
				aP.t1 = aTS1.tokens.elementAt(--x);
			}
			else {
				aP.t2 = aTS2.tokens.elementAt(--y);
			}
			aPs.add(aP);
		}
		
		//Reverse order
		Vector<Pair> aPOs = new Vector<Pair>();
		for(int p = aPs.size()-1;p >= 0;p--) {
			aPOs.add(aPs.elementAt(p));
		}
		
		return aPOs;
	}
	
	double cost(NSToken aT1,NSToken aT2) {
		if(aT1.kind != aT2.kind) {
			return COST_INCREDIBLE;
		}
		if(aT1.token.equals(aT2.token)) {
			return 0;
		}
		if(aT1.token.equalsIgnoreCase(aT2.token)) {
			return 0.01;
		}
		if(aT1.token.startsWith(aT2.token) || aT1.token.endsWith(aT2.token)
				|| aT2.token.startsWith(aT1.token) || aT2.token.endsWith(aT1.token)) {//Segmentation problem ?
			return 1.0 - 2.0*Math.min(aT1.token.length(),aT2.token.length())/(double)(aT1.token.length()+aT2.token.length());
		}
		if(aT1.pos != null && aT2.pos != null
				&& aT1.pos.equalsIgnoreCase(aT2.pos)) {
			return 1.0;
		}
		return 2.0 - 2.0*Math.min(aT1.token.length(),aT2.token.length())/(double)(aT1.token.length()+aT2.token.length());
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
