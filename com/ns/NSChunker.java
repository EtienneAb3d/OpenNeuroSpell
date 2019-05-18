package com.ns;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NSChunker {
	class NSChunkerChunk{
		public String chunk = null;
		public String pos = null;
		public String idxPos = null;
		public String posExt = null;
		public String tagExt = null;
		public boolean hasFem = false;
		public boolean hasPlur = false;
		public Vector<NSChunkerWord> words = new Vector<NSChunkerWord>();
	}
	
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
	Vector<Vector<NSChunkerRule>> ruleLayers = new Vector<Vector<NSChunkerRule>>();
	Vector<Vector<NSChunkerRule>> ruleExtracts = new Vector<Vector<NSChunkerRule>>();

	NSAligner aligner = null;

	public NSChunker(String aLng) throws Exception {
		lng = aLng.toLowerCase();
		loadRules();
		aligner = new NSAligner();
	}
	
	void loadRules() throws Exception {
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
				System.out.println("LANGUAGETOOL");
				aCurrentRules = new Vector<NSChunkerRule>();
				ltLayers.add(aCurrentRules);
				continue;
			}
			if(aLine.startsWith("LAYER")) {
				System.out.println("LAYER");
				aCurrentRules = new Vector<NSChunkerRule>();
				ruleLayers.add(aCurrentRules);
				continue;
			}
			if(aLine.startsWith("EXTRACT")) {
				System.out.println("EXTRACT");
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
			System.out.println("R="+aRP+" => RE="+aR.patternPOSDef);
			return aR;
		}
		
		String aRT = aTs[aIdx++];
		aR.patternTag = Pattern.compile(aRT);
		
		if(aIdx >= aTs.length || aTs[aIdx].startsWith("//")) {
			System.out.println("R="+aRP+" => RE="+aR.patternPOSDef+" / "+aRT);
			return aR;
		}
		
		String aRX = aTs[aIdx++];
		aR.patternText = Pattern.compile(aRX);
		
		if(aIdx >= aTs.length || aTs[aIdx].startsWith("//")) {
			System.out.println("R="+aRP+" => RE="+aR.patternPOSDef+" / "+aRT+" / "+aRX);
			return aR;
		}
		
		return aR;
	}
	
	Vector<NSChunkerChunk> buildChunks(Vector<NSChunkerWord> aWs,String aPosStr)  throws Exception {
		Vector<NSChunkerChunk> aChunks = new Vector<NSChunkerChunk>();
		String[] aPosToks = aPosStr.replaceAll(" +", " ").trim().split(" ");
		for(String aPosTok : aPosToks) {
			aChunks.add(buildChunk(aWs, aPosTok));
		}
		return aChunks;
	}
	
	NSChunkerChunk buildChunk(Vector<NSChunkerWord> aWs,String aPosTok)  throws Exception {
		return buildChunk(aWs, aPosTok, 0);
	}
	NSChunkerChunk buildChunk(Vector<NSChunkerWord> aWs,String aPosTok,int aStartIdx)  throws Exception {
		NSChunkerChunk aChunk = new NSChunkerChunk();
		aChunk.pos = aPosTok.replaceAll("[0-9,]", "");
		int aS = Integer.parseInt(aPosTok.replaceAll(",.*", ""));
		int aE = Integer.parseInt(aPosTok.replaceAll("(.*,|[^0-9,]+)", ""));
		StringBuffer aChunkSB = new StringBuffer();
		StringBuffer aPosSB = new StringBuffer();
		StringBuffer aIdxPosSB = new StringBuffer();
		StringBuffer aTagSB = new StringBuffer();
		int aIdx = aStartIdx;
		for(int p = aS;p<=aE;p++) {
			NSChunkerWord aW = aWs.elementAt(p);
			if(aW.doNotPrint) {
				continue;
			}
			aChunk.words.add(aW);
			aChunkSB.append(" "+aW.word);
			aPosSB.append(" "+aW.pos+" ");
			aIdxPosSB.append(" "+aIdx+","+aIdx+aW.pos+" ");
			aIdx++;
			aTagSB.append(" "+aW.tag);
			if(aW.tag.indexOf("=Plur")>=0) {
				aChunk.hasPlur = true;
			}
			if(aW.tag.indexOf("=Fem")>=0) {
				aChunk.hasFem = true;
			}
		}
		aChunk.chunk = aChunkSB.toString()
				.replaceAll("[(] +", "(")
				.replaceAll(" +[)]", ")")
				.trim();
		aChunk.posExt = aPosSB.toString().replaceAll(" +", " ").trim();
		aChunk.idxPos = aIdxPosSB.toString();
		aChunk.tagExt = aTagSB.toString().trim();
		return aChunk;
	}
	
	void disambiguate(Vector<NSChunkerWord> aWs,Vector<String> aDs) throws Exception {
		for(NSChunkerWord aW : aWs) {
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
	
	String applyRules(Vector<Vector<NSChunkerRule>> aRuleLayers,Vector<NSChunkerWord> aWs,String aPosStr) throws Exception {
		System.out.println(aPosStr);
		for(Vector<NSChunkerRule> aRules : aRuleLayers) {
			for(int r = 0;r < aRules.size();r++) {
				NSChunkerRule aR = aRules.elementAt(r);
				String aNew = applyRule(aWs,aR,aPosStr);
				if(aNew != null) {
					//Something rewritten, need to restart
					r = -1;
					aPosStr = aNew;
					System.out.println("R="+aR.ruleDef);
					System.out.println(aPosStr
							+"\n-----");
				}
			}
		}
		return aPosStr;
	}
	
	String applyRule(Vector<NSChunkerWord> aWs,NSChunkerRule aR,String aPosStr) throws Exception {
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
			NSChunkerChunk aC = buildChunk(aWs, aPosNew.trim());
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
				disambiguate(aC.words,aR.desambiguates);
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
			System.out.println("ChunkP: '"+aChunk+"' => '"+aPosNew+"'\n"
					+"=>"+aC.chunk);
			return aPosStr.substring(0, aM.start())
					+ aPosNew
					+ aPosStr.substring(aM.end());
		}
		return null;
	}
	
	public TaggedSent process(String aTxt) throws Exception {
		
		TaggedSent aLTTS = new TaggedSent();
		aLTTS.text = aTxt;
		ClientLT aLT = new ClientLT(lng);

		Thread aThLTC = aLT.getTagBatch(aLTTS, lng, ltLayers);
		
		TaggedSent aSpaCyTS = new TaggedSent();
		aSpaCyTS.text = aTxt;
		Thread aSpaCyTh = ClientSpacy.getTagBatch(aSpaCyTS,lng);
		
		TaggedSent aPolyglotTS = new TaggedSent();
		aPolyglotTS.text = aTxt;
		Thread aPolyglotTh = ClientPolyglot.getTagBatch(aPolyglotTS,lng);
		
		aThLTC.join();
		aSpaCyTh.join();
		aPolyglotTh.join();
		
		aLT.releaseLT();
		
//		System.out.println("LanguageTool: "+aLTC);
		
		TaggedSent aFusedTS = aligner.fusPos(aSpaCyTS, aPolyglotTS,aLTTS);
		System.out.println("FUSED POS: "+aFusedTS.idxPos);
		
		aFusedTS.chunks = buildChunks(aFusedTS.words,applyRules(ruleLayers,aFusedTS.words,aFusedTS.idxPos));

		//Rebuild disambiguated pos
		StringBuffer aPosSB = new StringBuffer();
		for(int w = 0;w < aFusedTS.words.size();w++) {
			NSChunkerWord aW = aFusedTS.words.elementAt(w);
			aPosSB.append(" "+w+","+w+aW.pos+" ");
		}
		aFusedTS.idxPos = aPosSB.toString();

		aFusedTS.extracts = new Vector<NSChunkerChunk>();
		for(NSChunkerChunk aC : aFusedTS.chunks) {
			for(NSChunkerChunk aX : buildChunks(aC.words,applyRules(ruleExtracts,aC.words,aC.idxPos))) {
				if(aX.pos.startsWith("~")) {
					aFusedTS.extracts.add(aX);
				}
			}
		}

		System.out.println("__________\n"
				+ "Disambiguations:");
		for(int w = 0;w < aFusedTS.words.size();w++) {
			NSChunkerWord aW = aFusedTS.words.elementAt(w);
			System.out.println(aW.word+"\t"+aW.posOrig+"\t=>\t"+aW.pos);
		}

		System.out.println("__________\n"
				+ "Chunks:");
		for(NSChunkerChunk aC : aFusedTS.chunks) {
			System.out.println(aC.chunk+"\t"+aC.pos+" F="+aC.hasFem+" P="+aC.hasPlur+" posExt="+aC.posExt);
		}
		
		System.out.println("__________\n"
				+ "Extracts:");
		for(NSChunkerChunk aC : aFusedTS.extracts) {
			System.out.println(aC.chunk+"\t"+aC.pos+" F="+aC.hasFem+" P="+aC.hasPlur+" posExt="+aC.posExt);
		}

		return aFusedTS;
	}

	public static void main(String[] args) {
		try {
			new NSChunker("fr").process(TestData.text);
		}
		catch(Throwable t) {
			t.printStackTrace(System.err);
		}
	}

}
