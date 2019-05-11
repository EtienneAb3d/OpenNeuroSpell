package com.ns;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.mindprod.http.Get;
import com.mindprod.http.Post;

public class NSChunker {
	class NSChunkerChunk{
		String chunk = null;
		String pos = null;
		String posExt = null;
		String tagExt = null;
		boolean hasFem = false;
		boolean hasPlur = false;
	}
	
	class NSChunkerRule{
		String ruleDef = null;
		boolean hardReplacePOS = false;
		Pattern patternPOS = null;
		Pattern patternTag = null;
		Pattern patternText = null;
		String pos = null;
	}
	
	String lng = null;
	String spaCyModel = null;
	
	Vector<Vector<NSChunkerRule>> ruleLayers = new Vector<Vector<NSChunkerRule>>();

	NSAligner aligner = null;
	
	public NSChunker(String aLng) throws Exception {
		lng = aLng.toLowerCase();
		findSpaCyModel();
		loadRules();
		aligner = new NSAligner();
	}
	
	void loadRules() throws Exception {
		BufferedReader aBR = new BufferedReader(
				new InputStreamReader(NSChunker.class.getResourceAsStream("chunkrules/"+lng+".tsv")));
		String aLine = null;
		Vector<NSChunkerRule> aCurrentRules = null;
		while((aLine = aBR.readLine()) != null){
			aLine = aLine.trim();
			if(aLine.isEmpty()){
				continue;
			}
			if(aLine.startsWith("LAYER")) {
				System.out.println("LAYER");
				aCurrentRules = new Vector<NSChunkerRule>();
				ruleLayers.add(aCurrentRules);
				continue;
			}
			NSChunkerRule aR = compileRule(aLine);
			if(aR == null) {
				//Just a comment ?
				continue;
			}
			aCurrentRules.add(aR);
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
		if(aR.pos.startsWith("+")) {
			aR.pos = aR.pos.substring(1);
			aR.hardReplacePOS = true;
		}
		String aRP = aTs[aIdx++];
		String aRE = aRP
				.replaceAll("&", "( [0-9,]+")
				.replaceAll(";", " )")
				.replaceAll("_", "([^_ ]*_|_[^_ ]*)?");
		aR.patternPOS = Pattern.compile(aRE);
		if(aIdx >= aTs.length || aTs[aIdx].startsWith("//")) {
			System.out.println("R="+aRP+" => RE="+aRE);
			return aR;
		}
		String aRT = aTs[aIdx++];
		aR.patternTag = Pattern.compile(aRT);
		if(aIdx >= aTs.length || aTs[aIdx].startsWith("//")) {
			System.out.println("R="+aRP+" => RE="+aRE+" / "+aRT);
			return aR;
		}
		String aRX = aTs[aIdx++];
		aR.patternText = Pattern.compile(aRX);
		if(aIdx >= aTs.length || aTs[aIdx].startsWith("//")) {
			System.out.println("R="+aRP+" => RE="+aRE+" / "+aRT+" / "+aRX);
			return aR;
		}
		return aR;
	}
	
	void findSpaCyModel() throws Exception {
		Get aGet = new Get();
		System.out.println("MODELS");
		String aRep = aGet.send("localhost", 8081, "/models", "utf-8");
		System.out.println(aRep);
		JSONParser parser = new JSONParser();
		JSONObject aJSO = (JSONObject)parser.parse(aRep);
		for(Object aO : aJSO.keySet()) {
			String aM = (String)aO;
			if(aM.startsWith(lng+"_")) {
				if(spaCyModel != null) {
					System.err.println("ALREADY A MODEL:"+spaCyModel+" / "+aM+" ??");
				}
				spaCyModel = aM;
			}
		}
	}

	Thread spaCyTag(final TaggedSent aTS) throws Exception {
		System.out.println("##########TAG spaCy");
		Thread aTh = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Post aPost = new Post();
					String aTxt = aTS.text.replaceAll("["+NSUtils.allapos+"]", "'");
					aPost.setPostParms("text",aTxt,
						    "model",spaCyModel
						    );
					
					String aRep = aPost.send("localhost",8081,"/tag", "utf-8");
					System.out.println("REP="+aRep);
					JSONParser parser = new JSONParser();
					JSONArray aJSO = (JSONArray)parser.parse(aRep);

					StringBuffer aPosSB = new StringBuffer();
					int aCountW = 0;
					for(Object aO : aJSO) {
						System.out.println("W: "+aO);
						JSONArray aA = (JSONArray)aO;
						NSChunkerWord aW = new NSChunkerWord();
						aW.word = (String)aA.get(0);
						aW.lemma = (String)aA.get(1);
						aW.pos = (String)aA.get(2);
						aW.tag = (String)aA.get(3);
						aTS.words.add(aW);
						aPosSB.append(" "+aCountW+","+aCountW+aW.pos+" ");
						aCountW++;
					}
					aTS.pos = aPosSB.toString();
				}
				catch(Throwable t) {
					t.printStackTrace(System.err);
				}
			}
			
		},"spaCy");
		aTh.start();
		return aTh;
	}
	
	Thread polyglotTag(final TaggedSent aTS) throws Exception {
		System.out.println("##########TAG polyglot");
		Thread aTh = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Post aPost = new Post();
					String aTxt = aTS.text.replaceAll("["+NSUtils.allapos+"]", "'");
					aPost.setPostParms("text",aTxt,
							"lng",lng
							);

					String aRep = aPost.send("localhost",8082,"/tag", "utf-8");
					System.out.println("REP="+aRep);
					JSONParser parser = new JSONParser();
					JSONArray aJSO = (JSONArray)parser.parse(aRep);

					StringBuffer aPosSB = new StringBuffer();
					int aCountW = 0;
					for(Object aO : aJSO) {
						System.out.println("W: "+aO);
						JSONArray aA = (JSONArray)aO;
						NSChunkerWord aW = new NSChunkerWord();
						aW.word = (String)aA.get(0);
						aW.lemma = "";
						aW.pos = (String)aA.get(1);
						aW.tag = "";
						aTS.words.add(aW);
						aPosSB.append(" "+aCountW+","+aCountW+aW.pos+" ");
						aCountW++;
					}
					aTS.pos = aPosSB.toString();
				}
				catch(Throwable t) {
					t.printStackTrace(System.err);
				}
			}

		},"polyglot");
		aTh.start();
		return aTh;
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
		NSChunkerChunk aChunk = new NSChunkerChunk();
		aChunk.pos = aPosTok.replaceAll("[0-9,]", "");
		int aS = Integer.parseInt(aPosTok.replaceAll(",.*", ""));
		int aE = Integer.parseInt(aPosTok.replaceAll("(.*,|[^0-9,]+)", ""));
		StringBuffer aChunkSB = new StringBuffer();
		StringBuffer aPosSB = new StringBuffer();
		StringBuffer aTagSB = new StringBuffer();
		for(int p = aS;p<=aE;p++) {
			NSChunkerWord aW = aWs.elementAt(p);
			aChunkSB.append(" "+aW.word);
			aPosSB.append(" "+aW.pos+" ");
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
		aChunk.tagExt = aTagSB.toString().trim();
		return aChunk;
	}
	
	String applyRules(Vector<NSChunkerWord> aWs,String aPosStr) throws Exception {
		System.out.println(aPosStr);
		for(Vector<NSChunkerRule> aRules : ruleLayers) {
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
			System.out.println("ChunkP: '"+aChunk+"' => '"+aPosNew+"'\n"
					+"=>"+aC.chunk);
			if(aR.hardReplacePOS && aS == aE) {
				//Hard rewriting of word POS
				aWs.elementAt(aS).pos = aR.pos;
			}
			return aPosStr.substring(0, aM.start())
					+ aPosNew
					+ aPosStr.substring(aM.end());
		}
		return null;
	}
	
	public void process(String aTxt) throws Exception {
		TaggedSent aSpaCyTS = new TaggedSent();
		aSpaCyTS.text = aTxt;
		Thread aSpaCyTh = spaCyTag(aSpaCyTS);
		
		TaggedSent aPolyglotTS = new TaggedSent();
		aPolyglotTS.text = aTxt;
		Thread aPolyglotTh = polyglotTag(aPolyglotTS);
		
		aSpaCyTh.join();
		aPolyglotTh.join();
		
		TaggedSent aFusedTS = aligner.fusPos(aSpaCyTS, aPolyglotTS);
		System.out.println("FUSED POS: "+aFusedTS.pos);
		
		Vector<NSChunkerChunk> aChunks = buildChunks(aFusedTS.words,applyRules(aFusedTS.words,aFusedTS.pos));
		for(NSChunkerChunk aC : aChunks) {
			System.out.println(aC.chunk+" "+aC.pos+" F="+aC.hasFem+" P="+aC.hasPlur+" posExt="+aC.posExt);
		}
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
