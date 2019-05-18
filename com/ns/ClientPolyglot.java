package com.ns;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.mindprod.http.Post;

public class ClientPolyglot {
	static final boolean _DEBUG = false;

	static int port = 8081;

	public static Thread getTagBatch(final TaggedSent aTS,final String aLng) throws Exception {
		if(_DEBUG || NSChunker._DEBUG_ALL) {
			System.out.println("##########TAG polyglot");
		}
		Thread aTh = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Post aPost = new Post();
					String aTxt = aTS.text.replaceAll("["+NSUtils.allapos+"]", "'");
					aPost.setPostParms("text",aTxt,
							"lng",aLng
							);

					String aRep = aPost.send("localhost",port,"/tag", "utf-8");
					if(_DEBUG || NSChunker._DEBUG_ALL) {
						System.out.println("REP="+aRep);
					}
					JSONParser parser = new JSONParser();
					JSONArray aJSO = (JSONArray)parser.parse(aRep);

					StringBuffer aPosSB = new StringBuffer();
					int aCountW = 0;
					for(Object aO : aJSO) {
						if(_DEBUG || NSChunker._DEBUG_ALL) {
							System.out.println("PGW: "+aO);
						}
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
					aTS.idxPos = aPosSB.toString();
				}
				catch(Throwable t) {
					t.printStackTrace(System.err);
				}
			}

		},"polyglot");
		aTh.start();
		return aTh;
	}

	public static String getEnt(String aTxt,String aLng) throws Exception {
		StringBuffer aSB = new StringBuffer();
		try {
			Post aPost = new Post();
			aPost.setPostParms("text",aTxt,
				    "lng",aLng
				    );
			String aRep = aPost.send("localhost",ClientPolyglot.port,"/ent", "utf-8");
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("POLYGLOTREP="+aRep);
			}
			JSONParser parser = new JSONParser();
			JSONArray aJSA = (JSONArray)parser.parse(aRep);
			for(Object aT : aJSA){
				if(_DEBUG || NSChunker._DEBUG_ALL) {
					System.out.println(aT);
				}
				JSONObject aO = (JSONObject)aT;
//				for(Object aK : aO.keySet()){
//					System.out.println(aK+":"+aO.get(aK));
//				}
				JSONArray aWords = (JSONArray)aO.get("ent");
				StringBuffer aEntSB = new StringBuffer();
				for(Object aW : aWords) {
					aEntSB.append(" ").append((String)aW);
				}
				String aLabel = (String)aO.get("label");
				String aEnt = aEntSB.toString().trim();
				if(aEnt.toLowerCase().equals(aEnt)){
					//Ignore lowercased ents
					if(_DEBUG || NSChunker._DEBUG_ALL) {
						System.out.println("POLYGLOTENT: "+aEnt+"/"+aLabel+" IGNORED LC");
					}
					continue;
				}
//				if(aLabel.matches("(DATE|TIME|ORDINAL|CARDINAL|PERCENT|MONEY|QUANTITY|LANGUAGE)")){
//					//Ignore lowercased ents
//					System.out.println("SPACYENT: "+aEnt+"/"+aLabel+" IGNORED "+aLabel);
//					continue;
//				}
				if(_DEBUG || NSChunker._DEBUG_ALL) {
					System.out.println("POLYGLOTENT: "+aEnt+"/"+aLabel);
				}
				aSB.append("\t["+aEnt+"]"+aLabel);
			}
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("POLYGLOTSTRING="+aSB.toString());
			}
		}
		catch(Throwable t) {
			//Ignore all for now
			t.printStackTrace(System.err);
		}
		return aSB.toString();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
