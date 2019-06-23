package com.ns;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NSChunker {
	static final boolean _DEBUG_ALL = true;
	static final boolean _DEBUG = false;
	
	class NSChunkerRule{
		String ruleDef = null;
		String patternPOSDef = null;
		boolean hardReplacePOS = false;
		Vector<String> desambiguates = new Vector<String>();
		boolean doNotAggregate = false;
		Pattern patternPOS = null;
		Pattern patternTag = null;
		Pattern patternText = null;
		String pos = null;
	}
	
	String lng = null;

	Vector<Vector<NSChunkerRule>> ltLayers = new Vector<Vector<NSChunkerRule>>();
	Vector<Vector<NSChunkerRule>> spaCyLayers = new Vector<Vector<NSChunkerRule>>();
	Vector<Vector<NSChunkerRule>> ruleLayers = new Vector<Vector<NSChunkerRule>>();
	Vector<Vector<NSChunkerRule>> ruleExtracts = new Vector<Vector<NSChunkerRule>>();

	NSAligner aligner = null;
	boolean usePolyglot = true;
	boolean useLT = true;

	public NSChunker(String aLng,boolean aUsePolyglot,boolean aUseLT) throws Exception {
		lng = aLng.toLowerCase();
		loadRules();
		aligner = new NSAligner();
		usePolyglot = aUsePolyglot;
		useLT = aUseLT;
	}
	
	void loadRules() throws Exception {
		System.out.println("LOADING CHUNKER RULES: "+lng+".tsv");
		BufferedReader aBR = new BufferedReader(
				new InputStreamReader(NSChunker.class.getResourceAsStream("chunkrules/"+lng+".tsv")));
		String aLine = null;
		Vector<NSChunkerRule> aCurrentRules = null;
//		boolean aIsExtractRule = false;
		while((aLine = aBR.readLine()) != null){
			aLine = aLine.trim();
			if(aLine.isEmpty()){
				continue;
			}
			if(aLine.startsWith("LANGUAGETOOL")) {
				if(_DEBUG || NSChunker._DEBUG_ALL) {
					System.out.println("LANGUAGETOOL");
				}
				aCurrentRules = new Vector<NSChunkerRule>();
				ltLayers.add(aCurrentRules);
				continue;
			}
			if(aLine.startsWith("SPACY")) {
				if(_DEBUG || NSChunker._DEBUG_ALL) {
					System.out.println("SPACY");
				}
				aCurrentRules = new Vector<NSChunkerRule>();
				spaCyLayers.add(aCurrentRules);
				continue;
			}
			if(aLine.startsWith("LAYER")) {
				if(_DEBUG || NSChunker._DEBUG_ALL) {
					System.out.println("LAYER");
				}
				aCurrentRules = new Vector<NSChunkerRule>();
				ruleLayers.add(aCurrentRules);
				continue;
			}
			if(aLine.startsWith("EXTRACT")) {
				if(_DEBUG || NSChunker._DEBUG_ALL) {
					System.out.println("EXTRACT");
				}
				aCurrentRules = new Vector<NSChunkerRule>();
				ruleExtracts.add(aCurrentRules);
//				aIsExtractRule = true;
				continue;
			}
			NSChunkerRule aR = compileRule(aLine);
			if(aR == null) {
				//Just a comment ?
				continue;
			}
			aCurrentRules.add(aR);
//			if(aIsExtractRule) {
//				aR.pos = "~" + aR.pos;
//			}
		}
		aBR.close();
	}
	
	NSChunkerRule compileRule(String aRuleDef) throws Exception {
		String[] aTs = aRuleDef.split("\t");
		int aIdx = 0;
		if(aIdx >= aTs.length || aTs[aIdx].startsWith("//")) {
			return null;
		}
		NSChunkerRule aR = new NSChunkerRule();
		aR.ruleDef = aRuleDef;
		aR.pos = aTs[aIdx++];
		int aSlash = aR.pos.indexOf('/');
		if(aSlash > 0) {
			aR.desambiguates.addAll(Arrays.asList(aR.pos.substring(aSlash+1).split(",")));
			aR.pos = aR.pos.substring(0,aSlash);
		}
		if(aR.pos.startsWith("+")) {
			aR.pos = aR.pos.substring(1);
			aR.hardReplacePOS = true;
		}
		if(aR.pos.startsWith("=")) {
			aR.pos = aR.pos.substring(1);
			aR.doNotAggregate = true;
		}
		String aRP = aTs[aIdx++];
		aR.patternPOSDef = aRP
				.replaceAll("&_;", "&[^ ]*;")//Any POS
				.replaceAll("&_", "&([^_ ]*_)?")//One side or all
				.replaceAll("_;", "(_[^_ ]*)?;")//One side or all
				//General final token form
				.replaceAll("&", "( [0-9,]+")
				.replaceAll(";", " )")
				;
		aR.patternPOS = Pattern.compile(aR.patternPOSDef);
		
		if(aIdx >= aTs.length || aTs[aIdx].startsWith("//")) {
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("R="+aRP+" => RE="+aR.patternPOSDef);
			}
			return aR;
		}
		
		String aRT = aTs[aIdx++];
		aR.patternTag = Pattern.compile(aRT);
		
		if(aIdx >= aTs.length || aTs[aIdx].startsWith("//")) {
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("R="+aRP+" => RE="+aR.patternPOSDef+" / "+aRT);
			}
			return aR;
		}
		
		String aRX = aTs[aIdx++];
		aR.patternText = Pattern.compile(aRX);
		
		if(aIdx >= aTs.length || aTs[aIdx].startsWith("//")) {
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("R="+aRP+" => RE="+aR.patternPOSDef+" / "+aRT+" / "+aRX);
			}
			return aR;
		}
		
		return aR;
	}
	
	Vector<NSChunk> buildChunks(Vector<NSToken> aWs,String aPosStr)  throws Exception {
		Vector<NSChunk> aChunks = new Vector<NSChunk>();
		String[] aPosToks = aPosStr.replaceAll(" +", " ").trim().split(" ");
		for(String aPosTok : aPosToks) {
			aChunks.add(buildChunk(aWs, aPosTok));
		}
		return aChunks;
	}
	
	NSChunk buildChunk(Vector<NSToken> aWs,String aPosTok)  throws Exception {
		return buildChunk(aWs, aPosTok, 0);
	}
	NSChunk buildChunk(Vector<NSToken> aTs,String aPosTok,int aStartIdx)  throws Exception {
		NSChunk aChunk = new NSChunk();
		aChunk.pos = aPosTok.replaceAll("[0-9,]", "");
		int aS = Integer.parseInt(aPosTok.replaceAll(",.*", ""));
		int aE = Integer.parseInt(aPosTok.replaceAll("(.*,|[^0-9,]+)", ""));
		StringBuffer aChunkSB = new StringBuffer();
		StringBuffer aPosSB = new StringBuffer();
		StringBuffer aIdxPosSB = new StringBuffer();
		StringBuffer aTagSB = new StringBuffer();
		int aIdx = aStartIdx;
		for(int p = aS;p<=aE;p++) {
			NSToken aT = aTs.elementAt(p);
			if(aT.doNotPrint) {
				continue;
			}
			aChunk.tokens.add(aT);
			aChunkSB.append(" "+aT.token);
			aPosSB.append(" "+aT.pos+" ");
			aIdxPosSB.append(" "+aIdx+","+aIdx+aT.pos+" ");
			aIdx++;
			aTagSB.append(" "+aT.tag);
		}
		aChunk.chunk = aChunkSB.toString()
				.replaceAll("[(] +", "(")
				.replaceAll(" +[)]", ")")
				.trim();
		aChunk.posExt = aPosSB.toString().replaceAll(" +", " ").trim();
		aChunk.idxPos = aIdxPosSB.toString();
		aChunk.tagExt = aTagSB.toString().trim();
		
		evalPlur(aChunk);
		evalFem(aChunk);
		
		return aChunk;
	}
	
	//Hard coded eval in this version
	void evalPlur(NSChunk aChunk) throws Exception {
		if("fr".equalsIgnoreCase(lng)) {
			boolean aLtMayBe = false;
			boolean aLtHasUnk = false;
			for(NSToken aT : aChunk.tokens) {
				if(aT.tag.matches(".*LT: ")){
					aLtHasUnk = true;
				}
				if(aT.tag.matches(".*LT: (,?[^\t,]*\t[^\\t,]* p=?)+")) {
					//LT is sure
					aChunk.hasPlur = true;
					break;
				}
				if(aT.tag.matches(".*LT: .*\t[^\\t,]* p=?(,.*)?")) {
					aLtMayBe = true;
				}
			}
			if(!aLtHasUnk && !aLtMayBe) {
				//Avoid doing something wrong
				return;
			}
			for(NSToken aT : aChunk.tokens) {
				if(aT.tag.indexOf("=Plur")>=0) {
					//spaCy is sure
					aChunk.hasPlur = true;
					break;
				}
			}
			return;
		}
		if("en".equalsIgnoreCase(lng)) {
			boolean aLtMayBe = false;
			boolean aLtHasUnk = false;
			for(NSToken aT : aChunk.tokens) {
				if(aT.tag.matches(".*LT: ")){
					aLtHasUnk = true;
				}
				if(aT.tag.matches(".*LT: (,?[^\t,]*\t(NNS|NNPS))+")) {
					//LT is sure
					aChunk.hasPlur = true;
					break;
				}
				if(aT.tag.matches(".*LT: .*\t(NNS|NNPS)(,.*)?")) {
					aLtMayBe = true;
				}
			}
			if(!aLtHasUnk && !aLtMayBe) {
				//Avoid doing something wrong
				return;
			}
			for(NSToken aT : aChunk.tokens) {
				if(aT.tag.matches(".*\\t(NNS|NNPS) LT:.*")) {
					//spaCy is sure
					aChunk.hasPlur = true;
					break;
				}
			}
			return;
		}
	}
	
	//Hard coded eval in this version
	void evalFem(NSChunk aChunk) throws Exception {
		if("fr".equalsIgnoreCase(lng)) {
			boolean aLtMayBe = false;
			boolean aLtHasUnk = false;
			for(NSToken aT : aChunk.tokens) {
				if(aT.tag.matches(".*LT: ")){
					aLtHasUnk = true;
				}
				if(aT.tag.matches(".*LT:(,?[^\t,]*\t[^\\t,]* f [a-z=]*)+")) {
					//LT is sure
					aChunk.hasFem= true;
					break;
				}
				if(aT.tag.matches(".*LT:.*\t[^\\t,]* f [a-z=]*(,.*)?")) {
					aLtMayBe = true;
				}
			}
			if(!aLtHasUnk && !aLtMayBe) {
				//Avoid doing something wrong
				return;
			}
			for(NSToken aT : aChunk.tokens) {
				if(aT.tag.indexOf("=Fem")>=0) {
					//spaCy is sure
					aChunk.hasFem = true;
					break;
				}
			}
			return;
		}
		if("en".equalsIgnoreCase(lng)) {
			//??
		}
	}

	void disambiguate(Vector<NSToken> aWs,Vector<String> aDs) throws Exception {
		for(NSToken aW : aWs) {
			for(String aD : aDs) {
				if(aD.startsWith("+")) {
					String[] aParts = aD.substring(1).split(">");
					if(aW.pos.matches(aParts[0])) {
						aW.pos = aParts[1];
						break;
					}
				}
				else if(aD.startsWith("-")) {
					String aPos = aD.substring(1);
					if(aW.pos.matches(aPos)) {
						aW.doNotPrint = true;
						break;
					}
				}
				else if(aW.pos.matches("([^_]+_"+aD+"|"+aD+"_[^_]+)")) {
					aW.pos = aD;
					break;
				}
			}
		}
	}
	
	String applyRules(Vector<Vector<NSChunkerRule>> aRuleLayers,Vector<NSToken> aWs,String aPosStr) throws Exception {
		if(_DEBUG || NSChunker._DEBUG_ALL) {
			System.out.println(aPosStr);
		}
		for(Vector<NSChunkerRule> aRules : aRuleLayers) {
			for(int r = 0;r < aRules.size();r++) {
				NSChunkerRule aR = aRules.elementAt(r);
				String aNew = applyRule(aWs,aR,aPosStr);
				if(aNew != null) {
					//Something rewritten, need to restart
					r = -1;
					aPosStr = aNew;
					if(_DEBUG || NSChunker._DEBUG_ALL) {
						System.out.println("R="+aR.ruleDef);
						System.out.println(aPosStr
								+"\n-----");
					}
				}
			}
		}
		return aPosStr;
	}
	
	String applyRule(Vector<NSToken> aWs,NSChunkerRule aR,String aPosStr) throws Exception {
		Matcher aM = aR.patternPOS.matcher(aPosStr);
		while(aM.find()) {
			String aChunk = aPosStr.substring(aM.start(),aM.end());
//			System.out.println("FOUND CHUNK: ["+aChunk+"] WITH: ["+aR.patternPOSDef+"] R: "+aR.ruleDef+" ON: ["+aPosStr+"]");
			String[] aCPs = aChunk.trim().split(" ");
			int aS = Integer.parseInt(aCPs[0].replaceAll(",.*", ""));
			int aE = Integer.parseInt(aCPs[aCPs.length-1].replaceAll("(.*,|[^0-9,]+)", ""));
			String aPosNew = " "+aS+","+aE+aR.pos+" ";
			if(aChunk.equals(aPosNew)) {
				//No change
				continue;
			}
			NSChunk aC = buildChunk(aWs, aPosNew.trim());
			if(aR.patternTag != null || aR.patternText != null) {
				if(aR.patternTag != null && !aR.patternTag.matcher(aC.tagExt).matches()){
					//Ignore
					continue;
				}
				if(aR.patternText != null && !aR.patternText.matcher(aC.chunk).matches()){
					//Ignore
					continue;
				}
			}
			if(aR.desambiguates.size() > 0) {
				disambiguate(aC.tokens,aR.desambiguates);
			}
			if(aR.hardReplacePOS && aS == aE) {
				//Hard rewriting of word POS
				aWs.elementAt(aS).pos = aR.pos;
			}
			if(aR.doNotAggregate) {
				//Need to rebuilt in order to change idxPos
				String aOldPos = aC.idxPos;
				aC = buildChunk(aWs, aPosNew.trim(),aS);
				if(aOldPos.replaceAll("[0-9]+,[0-9]+", "").equals(aC.idxPos.replaceAll("[0-9]+,[0-9]+", ""))) {
					//No change, Ignore
					continue;
				}
				aPosNew = aC.idxPos;
			}
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("ChunkP: '"+aChunk+"' => '"+aPosNew+"'\n"
					+"=>"+aC.chunk);
			}
			return aPosStr.substring(0, aM.start())
					+ aPosNew
					+ aPosStr.substring(aM.end());
		}
		return null;
	}
	
	Vector<NSTaggedSent> split2TSs(String aTxt) throws Exception {
		Vector<NSTaggedSent> aTSs = new Vector<NSTaggedSent>();
		String[] aSents = aTxt.split("\n");
		for(String aSent : aSents) {
			NSTaggedSent aTS = new NSTaggedSent();
			aTS.text = aSent;
			aTSs.add(aTS);
		}
		return aTSs;
	}
	
	public Vector<NSTaggedSent> process(String aTxt) throws Exception {
		
		Vector<NSTaggedSent> aSpaCyTSs = split2TSs(aTxt);
		Thread aSpaCyTh = ClientSpacy.getTagBatch(aSpaCyTSs,lng, spaCyLayers);
		
		Vector<NSTaggedSent> aLTTSs = split2TSs(aTxt);
		Thread aThLTC = null;
		if(useLT) {
			aThLTC = ClientLT.getTagBatch(aLTTSs, lng, ltLayers);
		}
		
		Vector<NSTaggedSent> aPolyglotTSs = split2TSs(aTxt);
		Thread aPolyglotTh = null;
		if(usePolyglot) {
			aPolyglotTh = ClientPolyglot.getTagBatch(aPolyglotTSs,lng);
		}
		
		aSpaCyTh.join();
		if(useLT) {
			aThLTC.join();
		}
		if(usePolyglot) {
			aPolyglotTh.join();
		}

		Vector<NSTaggedSent> aFusedTSs = new Vector<NSTaggedSent>();
		for(int s = 0;s < aSpaCyTSs.size();s++) {
			NSTaggedSent aSpaCyTS = aSpaCyTSs.elementAt(s);
			NSTaggedSent aPolyglotTS = aPolyglotTSs.elementAt(s);
			NSTaggedSent aLTTS = aLTTSs.elementAt(s);
			
			NSTaggedSent aFusedTS = aligner.fusPos(aSpaCyTS, aPolyglotTS,aLTTS);
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("FUSED POS: "+aFusedTS.idxPos);
			}

			try {
				aFusedTS.chunks = buildChunks(aFusedTS.tokens,applyRules(ruleLayers,aFusedTS.tokens,aFusedTS.idxPos));
			}
			catch(Throwable t) {
				t.printStackTrace(System.err);
				System.err.println("s= "+s);
				System.err.println("FUSTXT: ["+aFusedTS.text+"]");
				System.err.println("FUSPOS: ["+aFusedTS.idxPos+"]");
				System.err.println("SPCYTXT: ["+aSpaCyTS.text+"]");
				System.err.println("SPCYPOS: ["+aSpaCyTS.idxPos+"]");
				System.err.println("ORIG: ["+aTxt+"]");
				//??
				System.exit(-1);
			}

			//Rebuild disambiguated pos
			StringBuffer aPosSB = new StringBuffer();
			for(int w = 0;w < aFusedTS.tokens.size();w++) {
				NSToken aW = aFusedTS.tokens.elementAt(w);
				aPosSB.append(" "+w+","+w+aW.pos+" ");
			}
			aFusedTS.idxPos = aPosSB.toString();

			aFusedTS.extracts = new Vector<NSChunk>();
			for(NSChunk aC : aFusedTS.chunks) {
				for(NSChunk aX : buildChunks(aC.tokens,applyRules(ruleExtracts,aC.tokens,aC.idxPos))) {
					if(aX.pos.startsWith("~")) {
						aFusedTS.extracts.add(aX);
					}
				}
			}
			
			aFusedTSs.add(aFusedTS);
		}
		
		return aFusedTSs;
	}

	public static void main(String[] args) {
		try {
			Vector<NSTaggedSent> aTSs = new NSChunker(TestData.lng,true,true).process(TestData.text);

			for(NSTaggedSent aTS : aTSs) {
				System.out.println("__________\n"
						+ "Disambiguations:");
				for(int w = 0;w < aTS.tokens.size();w++) {
					NSToken aW = aTS.tokens.elementAt(w);
					System.out.println(aW.token+"\t"+aW.posOrig+"\t=>\t"+aW.pos);
				}

				System.out.println("__________\n"
						+ "Chunks:");
				for(NSChunk aC : aTS.chunks) {
					System.out.println(aC.chunk+"\t"+aC.pos+" F="+aC.hasFem+" P="+aC.hasPlur+" posExt="+aC.posExt);
				}

				System.out.println("__________\n"
						+ "Extracts:");
				for(NSChunk aC : aTS.extracts) {
					System.out.println(aC.chunk+"\t"+aC.pos+" F="+aC.hasFem+" P="+aC.hasPlur+" posExt="+aC.posExt);
				}
			}
		}
		catch(Throwable t) {
			t.printStackTrace(System.err);
		}
	}

}
