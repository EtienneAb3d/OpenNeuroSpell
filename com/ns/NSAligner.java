package com.ns;

import java.util.Vector;

public class NSAligner {
	class Pair{
		NSChunkerWord w1 = null;
		NSChunkerWord w2 = null;
	}
	TaggedSent fusPos(TaggedSent aTS1,TaggedSent aTS2,TaggedSent aTS3) throws Exception {
		Vector<Pair> aP12s = align(aTS1, aTS2);
		Vector<Pair> aP13s = align(aTS1, aTS3);
		
		TaggedSent aFused = new TaggedSent();
		int aP13idx = aP13s.size()-1;
		for(int p = aP12s.size()-1;p >= 0;p--) {
			Pair aP12 = aP12s.elementAt(p);

			//Must found a P13 match
			Pair aP13 = null;
			if(aP12.w1 != null && aP13idx >= 0) {
				while(aP13 == null || aP13.w1 == null || !aP12.w1.word.equals(aP13.w1.word)){
					aP13 = aP13s.elementAt(aP13idx);
					aP13idx--;
				}
			}

			System.out.println("AL:"
					+ (aP12.w1!=null ? aP12.w1.word+"\t("+aP12.w1.pos+")":"????")
					+ "\t/\t"
					+ (aP12.w2!=null ? aP12.w2.word+"\t("+aP12.w2.pos+")":"????")
					+ "\t/\t"
					+ (aP12.w1!=null ? aP12.w1.lemma + " " + aP12.w1.tag:"????")
					+ "\t/\t"
					+ (aP13 != null && aP13.w2 != null ? "("+aP13.w2.pos+") "+ aP13.w2.lemma + " " + aP13.w2.tag:"????")
					);

			if(aP12.w1 == null) {
				//Ignore ??
				continue;
			}

			NSChunkerWord aW = aP12.w1.clone();
			aFused.words.add(aW);
			aW.posOrig = aP12.w1.pos;
					
			if(aP12.w2 == null) {
				//Ignore mismatch ??
				continue;
			}
			if(!aP12.w1.word.equals(aP12.w2.word)) {
				//Ignore mismatch ??
				continue;
			}
			if(aP12.w1.pos.equals(aP12.w2.pos)) {
				//OK, keep like this
				continue;
			}
			//Merge diff
			aW.posOrig = aP12.w1.pos + "_" + aP12.w2.pos;

			if(aP13.w2 != null && aP13.w2.pos != null && !aP13.w2.pos.trim().isEmpty()) {
				//May be validated by LT
				if(aP13.w2.pos.indexOf(aP12.w1.pos) >= 0
						&& aP13.w2.pos.indexOf(aP12.w2.pos) < 0) {
					//Only 1 is possible
					continue;
				}
				if(aP13.w2.pos.indexOf(aP12.w2.pos) >= 0
						&& aP13.w2.pos.indexOf(aP12.w1.pos) < 0) {
					//Only 2 is possible
					aW.pos = aP12.w2.pos;
					continue;
				}
				if(aP13.w2.pos.trim().indexOf(" ") < 0) {
					//Provide with a single possibility, take it !
					aW.pos = aP13.w2.pos.trim();
					continue;
				}
			}
			aW.pos = aP12.w1.pos + "_" + aP12.w2.pos;
		}
		
		StringBuffer aPosSB = new StringBuffer();
		for(int w = 0;w < aFused.words.size();w++) {
			NSChunkerWord aW = aFused.words.elementAt(w);
			aPosSB.append(" "+w+","+w+aW.pos+" ");
			System.out.println("FUSED: "+aW.word+"\t("+aW.pos+")\t"+aW.lemma+"\t"+aW.tag);
		}
		aFused.idxPos = aPosSB.toString();
		
		return aFused;
	}
	
	Vector<Pair> align(TaggedSent aTS1,TaggedSent aTS2) throws Exception {
		int[][] aChoices = new int[aTS1.words.size()+1][aTS2.words.size()+1];
		double[][] aCosts = new double[aTS1.words.size()+1][aTS2.words.size()+1];
		for(int x = 0;x<aTS1.words.size()+1;x++) {
			aChoices[x][0] = 1;//Left
			aCosts[x][0] = x;
		}
		for(int y = 0;y<aTS2.words.size()+1;y++) {
			aChoices[0][y] = 2;//Up
			aCosts[0][y] = y;
		}
		for(int x = 1;x<aTS1.words.size()+1;x++) {
			for(int y = 1;y<aTS2.words.size()+1;y++) {
				double aCost = cost(aTS1.words.elementAt(x-1),aTS2.words.elementAt(y-1));
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

		int x = aTS1.words.size();
		int y = aTS2.words.size();
		Vector<Pair> aPs = new Vector<Pair>();
		while(x > 0 || y > 0) {
			Pair aP = new Pair(); 
			//		System.out.println("X="+x+" Y="+y+" Ch="+aChoices[x][y]+" Co="+aCosts[x][y]);
			if(aChoices[x][y] == 0) {
				aP.w1 = aTS1.words.elementAt(--x);
				aP.w2 = aTS2.words.elementAt(--y);
			}
			else if(aChoices[x][y] == 1) {
				aP.w1 = aTS1.words.elementAt(--x);
			}
			else {
				aP.w2 = aTS2.words.elementAt(--y);
			}
			aPs.add(aP);
		}
		return aPs;
	}
	
	double cost(NSChunkerWord aW1,NSChunkerWord aW2) {
		if(aW1.word.equals(aW2.word)) {
			return 0;
		}
		if(aW1.word.equalsIgnoreCase(aW2.word)) {
			return 0.1;
		}
		if(aW1.pos.equalsIgnoreCase(aW2.pos)) {
			return 0.2;
		}
		return 2.0;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
