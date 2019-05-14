package com.ns;

import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.mindprod.http.Get;
import com.mindprod.http.Post;

public class ClientSpacy {
	static final HashMap<String,String> model4lng = new HashMap<String,String>();
	static int port = 8081;

	static String getModel(String aLng) throws Exception {
		String aModel = model4lng.get(aLng);
		if(aModel != null){
			return aModel;
		}
		Get aGet = new Get();
		System.out.println("MODELS");
		String aRep = aGet.send("localhost", port, "/models", "utf-8");
		System.out.println(aRep);
		JSONParser parser = new JSONParser();
		JSONObject aJSO = (JSONObject)parser.parse(aRep);
		for(Object aO : aJSO.keySet()) {
			String aM = (String)aO;
			if(aM.startsWith(aLng+"_")) {
				if(aModel != null) {
					System.err.println("ALREADY A MODEL:"+aModel+" / "+aM+" ??");
				}
				aModel = aM;
			}
		}
		return aModel;
	}

	public static Thread getTagBatch(final TaggedSent aTS,final String aLng) throws Exception {
		System.out.println("##########TAG spaCy");
		Thread aTh = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String aModel = getModel(aLng);

					Post aPost = new Post();
					String aTxt = aTS.text.replaceAll("["+NSUtils.allapos+"]", "'");
					aPost.setPostParms("text",aTxt,
						    "model",aModel
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
					aTS.idxPos = aPosSB.toString();
				}
				catch(Throwable t) {
					t.printStackTrace(System.err);
				}
			}
			
		},"spaCy");
		aTh.start();
		return aTh;
	}

	public static String getEnt(String aTxt,String aLng) throws Exception {
		StringBuffer aSB = new StringBuffer();
		try {
			String aModel = ClientSpacy.getModel(aLng);
			
			System.out.println("SPACYMODEL="+aLng+"/"+aModel);
			
			Post aPost = new Post();
			aPost.setPostParms("text",aTxt,
				    "model",aModel
				    );
			String aRep = aPost.send("localhost",ClientSpacy.port,"/ent", "utf-8");
			System.out.println("SPACYREP="+aRep);
			JSONParser parser = new JSONParser();
			JSONArray aJSA = (JSONArray)parser.parse(aRep);
			for(Object aT : aJSA){
				System.out.println(aT);
				JSONObject aO = (JSONObject)aT;
				int aStart = ((Long)aO.get("start")).intValue();
				int aEnd = ((Long)aO.get("end")).intValue();
				String aLabel = (String)aO.get("label");
				String aEnt = aTxt.substring(aStart, aEnd);
				if(aEnt.toLowerCase().equals(aEnt)){
					//Ignore lowercased ents
					System.out.println("SPACYENT: "+aEnt+"/"+aLabel+" IGNORED LC");
					continue;
				}
				System.out.println("SPACYENT: "+aEnt+"/"+aLabel);
				aSB.append("\t["+aEnt+"]"+aLabel);
			}
			System.out.println("SPACYSTRING="+aSB.toString());
		}
		catch(Throwable t) {
			//Ignore all for now
			t.printStackTrace(System.err);
		}
		return aSB.toString();
	}

	public static Thread getEntSelectAndTokeniseBatch(final String aSegTxt, final String aLng,final StringBuffer aAnalyses) throws Exception {
		Thread aTh = new Thread(new Runnable(){
			@Override
			public void run() {
				aAnalyses.append(getEntSelectAndTokenise(aSegTxt,aLng));
			}
		},"getEntBatch");
		aTh.start();
		return aTh;
	}

	public static String getEntSelectAndTokenise(String aTxt,String aLng) {
		StringBuffer aSB = new StringBuffer();
		try {
			Post aPost = new Post();
			String aModel = getModel(aLng);
			
			System.out.println("SPACYMODEL="+aLng+"/"+aModel);
			
			aPost.setPostParms("text",aTxt,
				    "model",aModel
				    );
			String aRep = aPost.send("localhost",port,"/ent", "utf-8");
			System.out.println("SPACYREP="+aRep);
			JSONParser parser = new JSONParser();
			JSONArray aJSA = (JSONArray)parser.parse(aRep);
			for(Object aT : aJSA){
				System.out.println(aT);
				JSONObject aO = (JSONObject)aT;
				int aStart = ((Long)aO.get("start")).intValue();
				int aEnd = ((Long)aO.get("end")).intValue();
				String aLabel = (String)aO.get("label");
				String aEnt = aTxt.substring(aStart, aEnd);
				if(aEnt.toLowerCase().equals(aEnt)){
					//Ignore lowercased ents
					System.out.println("SPACYENT: "+aEnt+"/"+aLabel+" IGNORED LC");
					continue;
				}
				if(aLabel.matches("(DATE|TIME|ORDINAL|CARDINAL|PERCENT|MONEY|QUANTITY|LANGUAGE)")){
					//Ignore lowercased ents
					System.out.println("SPACYENT: "+aEnt+"/"+aLabel+" IGNORED "+aLabel);
					continue;
				}
				System.out.println("SPACYENT: "+aEnt+"/"+aLabel);
				aSB.append("\t");
				for(String aW : aEnt.split("[ -]")){
					aSB.append(" "+aW+"/"+aLabel+"#");
				}
			}
			System.out.println("SPACYSTRING="+aSB.toString());
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
