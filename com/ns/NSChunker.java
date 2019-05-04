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
	String lng = null;
	String model = null;
	
	class NSChunkerWord{
		String word = null;
		String lemma = null;
		String pos = null;
		String tag = null;
	}

	class NSChunkerChunk{
		String chunk = null;
		String pos = null;
		String posExt = null;
		boolean hasFem = false;
		boolean hasPlur = false;
	}
	
	class NSChunkerRule{
		Pattern patternPOS = null;
		Pattern patternTag = null;
		String pos = null;
	}
	
	Vector<NSChunkerRule> rules = new Vector<NSChunkerRule>();
	
	public NSChunker(String aLng) throws Exception {
		lng = aLng.toLowerCase();
		getModel();
		loadRules();
	}
	
	void getModel() throws Exception {
		Get aGet = new Get();
		System.out.println("MODELS");
		String aRep = aGet.send("localhost", 8080, "/models", "utf-8");
		System.out.println(aRep);
		JSONParser parser = new JSONParser();
		JSONObject aJSO = (JSONObject)parser.parse(aRep);
		for(Object aO : aJSO.keySet()) {
			String aM = (String)aO;
			if(aM.startsWith(lng+"_")) {
				if(model != null) {
					System.err.println("ALREADY A MODEL:"+model+" / "+aM+" ??");
				}
				model = aM;
			}
		}
	}
	
	void loadRules() throws Exception {
		BufferedReader aBR = new BufferedReader(
				new InputStreamReader(NeuroLex.class.getResourceAsStream("chunkrules/"+lng+".tsv")));
		String aLine = null;
		while((aLine = aBR.readLine()) != null){
			aLine = aLine.trim();
			if(aLine.isEmpty()){
				continue;
			}
			NSChunkerRule aR = compileRule(aLine);
			if(aR == null) {
				//Just a comment ?
				continue;
			}
			rules.add(aR);
		}
		aBR.close();
	}
	
	NSChunkerRule compileRule(String aRule) throws Exception {
		String[] aTs = aRule.split("\t");
		int aIdx = 0;
		if(aIdx >= aTs.length || aTs[aIdx].startsWith("//")) {
			return null;
		}
		NSChunkerRule aR = new NSChunkerRule();
		aR.pos = aTs[aIdx++];
		aR.patternPOS = Pattern.compile(aTs[aIdx++]
				.replaceAll("&", "( [0-9,]+")
				.replaceAll(";", ")")
				);
		if(aIdx >= aTs.length || aTs[aIdx].startsWith("//")) {
			return aR;
		}
		aR.patternTag = Pattern.compile(aTs[aIdx++]
				.replaceAll("&", "( [0-9,]+")
				.replaceAll(";", ")")
				);
		if(aIdx >= aTs.length || aTs[aIdx].startsWith("//")) {
			return aR;
		}
		return aR;
	}
	
	public void process(String aTxt) throws Exception {
		Post aPost = new Post();
		aTxt = aTxt.replaceAll("["+NSUtils.allapos+"]", "'");
		aPost.setPostParms("text",aTxt,
			    "model",model
			    );
		
		System.out.println("##########TAG");
		String aRep = aPost.send("localhost",8080,"/tag", "utf-8");
		System.out.println("REP="+aRep);
		JSONParser parser = new JSONParser();
		JSONArray aJSO = (JSONArray)parser.parse(aRep);
		Vector<NSChunkerWord> aWs = new Vector<NSChunkerWord>();
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
			aWs.add(aW);
			aPosSB.append(" "+aCountW+","+aCountW+aW.pos);
			aCountW++;
		}
		
		Vector<NSChunkerChunk> aChunks = buildChunks(aWs,applyRules(aWs,aPosSB.toString()));
		for(NSChunkerChunk aC : aChunks) {
			System.out.println(aC.chunk+" "+aC.pos+" F="+aC.hasFem+" P="+aC.hasPlur+" posExt="+aC.posExt);
		}
	}
	
	Vector<NSChunkerChunk> buildChunks(Vector<NSChunkerWord> aWs,String aPosStr)  throws Exception {
		Vector<NSChunkerChunk> aChunks = new Vector<NSChunkerChunk>();
		String[] aPosToks = aPosStr.trim().split(" ");
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
		for(int p = aS;p<=aE;p++) {
			NSChunkerWord aW = aWs.elementAt(p);
			aChunkSB.append(" "+aW.word);
			aPosSB.append(" "+aW.pos);
			if(aW.tag.indexOf("=Plur")>=0) {
				aChunk.hasPlur = true;
			}
			if(aW.tag.indexOf("=Fem")>=0) {
				aChunk.hasFem = true;
			}
		}
		aChunk.chunk = aChunkSB.toString().trim();
		aChunk.posExt = aPosSB.toString().trim();
		return aChunk;
	}
	
	String applyRules(Vector<NSChunkerWord> aWs,String aPosStr) throws Exception {
		System.out.println(aPosStr);
		for(int r = 0;r < rules.size();r++) {
			String aNew = applyRule(aWs,rules.elementAt(r),aPosStr);
			if(aNew != null) {
				//Something rewritten, need to restart
				r= 0;
				aPosStr = aNew;
				System.out.println(aPosStr);
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
			String aPosNew = " "+aS+","+aE+aR.pos;
			if(aChunk.equals(aPosNew)) {
				//No change
				continue;
			}
			if(aR.patternTag != null) {
				//Need to build a single chunk
				NSChunkerChunk aC = buildChunk(aWs, aPosNew.trim());
				if(!aR.patternTag.matcher(aC.chunk).matches()){
					//Ignore
					continue;
				}
			}
			System.out.println("ChunkP: '"+aChunk+"' => '"+aPosNew+"'");
			return aPosStr.substring(0, aM.start())
					+ aPosNew
					+ aPosStr.substring(aM.end());
		}
		return null;
	}
	
	public static void main(String[] args) {
		try {
			String aTxt = 
					"Il y a quatre mois, nous avons acheté des vignes dans l’Oregon. Nous sommes désormais un ­« véritable » vignoble.";
			new NSChunker("fr").process(aTxt);
		}
		catch(Throwable t) {
			t.printStackTrace(System.err);
		}
	}

}
