package com.ns;

import java.util.HashMap;
import java.util.Vector;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.mindprod.http.Post;

public class ClientPolyglot {
	static final boolean _DEBUG = false;

	static int port = 8081;

	public static Thread getTagBatch(final Vector<NSTaggedSent> aTSs,final String aLng) throws Exception {
		if(_DEBUG || NSChunker._DEBUG_ALL) {
			System.out.println("##########TAG polyglot");
		}
		Thread aTh = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					StringBuffer aSB = new StringBuffer();
					for(NSTaggedSent aTS : aTSs) {
						if(aSB.length() > 0) {
							aSB.append("\n");
						}
						aSB.append(aTS.text);
					}
					
					Post aPost = new Post();
					String aTxt = aSB.toString().replaceAll("["+NSUtils.allapos+"]", "'");
					aPost.setPostParms("text",aTxt,
							"lng",aLng
							);

//					String aRep = aPost.send("localhost",port,"/tagOnly", "utf-8");
					String aRep = aPost.send("localhost",port,"/tag", "utf-8");
					if(_DEBUG || NSChunker._DEBUG_ALL) {
						System.out.println("POLYGLOT="+aRep);
					}
					JSONParser parser = new JSONParser();
					JSONArray aJSO = (JSONArray)parser.parse(aRep);

					StringBuffer aPosSB = new StringBuffer();
					int aIdxTS = 0;
					NSTaggedSent aTS = aTSs.elementAt(aIdxTS);
					aTS.tokens = new Vector<NSToken>();//Be sure it's fresh (possible recurrent call)
					int aCountW = 0;
					for(Object aO : aJSO) {
						if(_DEBUG || NSChunker._DEBUG_ALL) {
							System.out.println("PGW: "+aO);
						}
						JSONArray aA = (JSONArray)aO;
						NSToken aW = new NSToken();
						aW.token = (String)aA.get(0);
						
						if("\n".equals(aW.token)
								|| "\\n".equals(aW.token)) {
							aTS.idxPos = aPosSB.toString();
							aPosSB = new StringBuffer();
							
							aIdxTS++;
							aTS = aTSs.elementAt(aIdxTS);
							aTS.tokens = new Vector<NSToken>();//Be sure it's fresh (possible recurrent call)
							aCountW = 0;
							continue;
						}
						
						aW.lemma = "";
						aW.pos = (String)aA.get(1);
						aW.tag = "";
						aTS.tokens.add(aW);
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
			aPost.setPostParms("text","["+aTxt+"]",
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

	static HashMap<String,String> syn(Vector<String> aTxts,String aLng) throws Exception {
		HashMap<String,String> aSyns = new HashMap<String,String>();

		StringBuffer aLst = new StringBuffer();
		for(String aTxt : aTxts) {
			if(aTxt.length() <= 1
					|| aTxt.matches(".*[0-9].*")) {
				continue;
			}
			if(aLst.length() > 0) {
				aLst.append(" ");
			}
			aLst.append(aTxt);
		}

		Post aPost = new Post();
		aPost.setReadTimeout(10*60*1000);
		aPost.setPostParms("text",aLst.toString(),
				"lng",aLng
				);

		//		String aRep = aPost.send("localhost",port,"/tagOnly", "utf-8");
		String aRep = aPost.send("localhost",port,"/syn", "utf-8");
		if(_DEBUG || NSChunker._DEBUG_ALL) {
			System.out.println("POLYGLOT="+aRep);
		}
		JSONParser parser = new JSONParser();
		JSONObject aJSO = (JSONObject)parser.parse(aRep);
		for(String aTxt : aTxts) {
			Object aSs = aJSO.get(aTxt);
			if(aSs == null) {
				continue;
			}
			StringBuffer aSSB = new StringBuffer();
			for(Object aO : (JSONArray)aSs) {
				if(aSSB.length() > 0) {
					aSSB.append(" ");
				}
				aSSB.append(aO);
			}
			if(aSSB.length() <= 0) {
				continue;
			}
			aSyns.put(aTxt, aSSB.toString());
		}
		return aSyns;
	}


	public static void main(String[] args) {
		try {
			Vector<String> aWs = new Vector<String>();
			String aLng =
//					"en";
					"fr";
			String aTxt = 
//					"I go fishing on sunday .";
					"Je vais à la pêche le dimanche .";
			for(String aW : aTxt.split(" ")) {
				aWs.add(aW);
			}
			HashMap<String,String> aSyns = ClientPolyglot.syn(aWs, aLng);
			for(String aW : aWs) {
				String aSs = aSyns.get(aW);
				System.out.println(aW+": "+(aSs == null ? "" : aSs));
			}
		}
		catch(Throwable t) {
			t.printStackTrace(System.err);
		}
	}

}
