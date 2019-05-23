package com.ns;

import java.util.HashMap;
import java.util.Vector;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.mindprod.http.Get;
import com.mindprod.http.Post;
import com.ns.NSChunker.NSChunkerRule;

public class ClientSpacy {
	static final boolean _DEBUG = false;

	static final HashMap<String,String> model4lng = new HashMap<String,String>();
	static {
		model4lng.put("fr", "fr_core_news_sm");
		model4lng.put("en", "en_core_web_sm");
	}
	static int port = 8091;
	static int instances = 1;
	static int currentInstance = 0;

	static String getModel(String aLng) throws Exception {
		String aModel = model4lng.get(aLng);
		if(aModel != null){
			return aModel;
		}
		Get aGet = new Get();
		if(_DEBUG || NSChunker._DEBUG_ALL) {
			System.out.println("MODELS");
		}
		String aRep = aGet.send("localhost", port, "/models", "utf-8");
		if(_DEBUG || NSChunker._DEBUG_ALL) {
			System.out.println(aRep);
		}
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
		if(aModel == null) {
			throw new Exception("can't find a '"+aLng+"' model for spaCy");
		}
		return aModel;
	}

	public static Thread getTagBatch(final Vector<NSTaggedSent> aTSs,final String aLng,Vector<Vector<NSChunkerRule>> aLtLayers) throws Exception {
		if(_DEBUG || NSChunker._DEBUG_ALL) {
			System.out.println("##########TAG spaCy");
		}
		Thread aTh = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String aModel = getModel(aLng);

					StringBuffer aSB = new StringBuffer();
					for(NSTaggedSent aTS : aTSs) {
						if(aSB.length() > 0) {
							aSB.append("\n");
						}
						aSB.append(aTS.text);
					}
					
					Post aPost = new Post();
					String aTxt = aSB.toString().replaceAll("["+NSUtils.allapos+"]", "'");
					aPost.setPostParms(
							"text",aTxt,
						    "model",aModel
						    );
					
					String aRep = aPost.send("localhost",port+(int)Math.floor(currentInstance++ % instances),"/tag", "utf-8");
					if(_DEBUG || NSChunker._DEBUG_ALL) {
						System.out.println("SPACY="+aRep);
					}
					JSONParser parser = new JSONParser();
					JSONArray aJSO = (JSONArray)parser.parse(aRep);

					StringBuffer aPosSB = new StringBuffer();
					int aIdxTS = 0;
					NSTaggedSent aTS = aTSs.elementAt(aIdxTS);
					aTS.tokens = new Vector<NSToken>();//Be sure it's fresh (possible recurrent call)
					int aCountW = 0;
					boolean aSomethingWrong = false;
					for(Object aO : aJSO) {
						if(_DEBUG || NSChunker._DEBUG_ALL) {
							System.out.println("SPW: "+aO);
						}
						JSONArray aA = (JSONArray)aO;
						NSToken aW = new NSToken();
						aW.token = (String)aA.get(0);

						if("\n".equals(aW.token)) {
							aTS.idxPos = aPosSB.toString();
							aPosSB = new StringBuffer();
							
							aIdxTS++;
							aTS = aTSs.elementAt(aIdxTS);
							aTS.tokens = new Vector<NSToken>();//Be sure it's fresh (possible recurrent call)
							aCountW = 0;
							continue;
						}
						
						if(aPosSB.length() <= 0) {
							if(!aTS.text.replaceAll("["+NSUtils.allapos+"]", "'").startsWith(aW.token)) {
								System.err.println("NOT GOOD START");
								aSomethingWrong = true;
								break;
//								System.exit(-1);
							}
						}
						
						aW.lemma = (String)aA.get(1);
						aW.pos = (String)aA.get(2);
						aW.tag = aW.lemma+"\t"+(String)aA.get(3);
						
						aTS.tokens.add(aW);
						for(Vector<NSChunkerRule> aLayer : aLtLayers) {
							for(NSChunkerRule aR : aLayer) {
								if(aR.patternPOS.matcher(aW.pos).matches()) {
									if(aR.patternText != null && !aR.patternText.matcher(aW.token).matches()) {
										//Ignore
										continue;
									}
									if(aR.patternTag != null && !aR.patternTag.matcher(aW.tag).matches()) {
										//Ignore
										continue;
									}
									aW.pos = aR.pos;
									break;
								}
							}
						}
						aPosSB.append(" "+aCountW+","+aCountW+aW.pos+" ");
						aCountW++;
					}
					aTS.idxPos = aPosSB.toString();
					if(aIdxTS != aTSs.size()-1) {
						System.err.println("BAD COUNT: "+aTSs.size()+" in / "+(aIdxTS+1)+" out");
						aSomethingWrong = true;
//						System.exit(-1);
					}
					if(aSomethingWrong && aTSs.size() > 1) {
						//?? Do one by one to avoid loosing something
						for(NSTaggedSent aTS2 : aTSs) {
							Vector<NSTaggedSent> aTSs2 = new Vector<NSTaggedSent>();
							aTSs2.add(aTS2);
							Thread aTh = getTagBatch(aTSs2, aLng, aLtLayers);
							aTh.join();
						}
					}

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
			
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("SPACYMODEL="+aLng+"/"+aModel);
			}
			
			Post aPost = new Post();
			aPost.setPostParms("text",aTxt,
				    "model",aModel
				    );
			String aRep = aPost.send("localhost",ClientSpacy.port,"/ent", "utf-8");
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("SPACYREP="+aRep);
			}
			JSONParser parser = new JSONParser();
			JSONArray aJSA = (JSONArray)parser.parse(aRep);
			for(Object aT : aJSA){
				if(_DEBUG || NSChunker._DEBUG_ALL) {
					System.out.println(aT);
				}
				JSONObject aO = (JSONObject)aT;
				int aStart = ((Long)aO.get("start")).intValue();
				int aEnd = ((Long)aO.get("end")).intValue();
				String aLabel = (String)aO.get("label");
				String aEnt = aTxt.substring(aStart, aEnd);
				if(aEnt.toLowerCase().equals(aEnt)){
					//Ignore lowercased ents
					if(_DEBUG || NSChunker._DEBUG_ALL) {
						System.out.println("SPACYENT: "+aEnt+"/"+aLabel+" IGNORED LC");
					}
					continue;
				}
				if(_DEBUG || NSChunker._DEBUG_ALL) {
					System.out.println("SPACYENT: "+aEnt+"/"+aLabel);
				}
				aSB.append("\t["+aEnt+"]"+aLabel);
			}
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("SPACYSTRING="+aSB.toString());
			}
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
			
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("SPACYMODEL="+aLng+"/"+aModel);
			}
			
			aPost.setPostParms("text",aTxt,
				    "model",aModel
				    );
			String aRep = aPost.send("localhost",port,"/ent", "utf-8");
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("SPACYREP="+aRep);
			}
			JSONParser parser = new JSONParser();
			JSONArray aJSA = (JSONArray)parser.parse(aRep);
			for(Object aT : aJSA){
				if(_DEBUG || NSChunker._DEBUG_ALL) {
					System.out.println(aT);
				}
				JSONObject aO = (JSONObject)aT;
				int aStart = ((Long)aO.get("start")).intValue();
				int aEnd = ((Long)aO.get("end")).intValue();
				String aLabel = (String)aO.get("label");
				String aEnt = aTxt.substring(aStart, aEnd);
				if(aEnt.toLowerCase().equals(aEnt)){
					//Ignore lowercased ents
					if(_DEBUG || NSChunker._DEBUG_ALL) {
						System.out.println("SPACYENT: "+aEnt+"/"+aLabel+" IGNORED LC");
					}
					continue;
				}
				if(aLabel.matches("(DATE|TIME|ORDINAL|CARDINAL|PERCENT|MONEY|QUANTITY|LANGUAGE)")){
					//Ignore lowercased ents
					if(_DEBUG || NSChunker._DEBUG_ALL) {
						System.out.println("SPACYENT: "+aEnt+"/"+aLabel+" IGNORED "+aLabel);
					}
					continue;
				}
				if(_DEBUG || NSChunker._DEBUG_ALL) {
					System.out.println("SPACYENT: "+aEnt+"/"+aLabel);
				}
				aSB.append("\t");
				for(String aW : aEnt.split("[ -]")){
					aSB.append(" "+aW+"/"+aLabel+"#");
				}
			}
			if(_DEBUG || NSChunker._DEBUG_ALL) {
				System.out.println("SPACYSTRING="+aSB.toString());
			}
		}
		catch(Throwable t) {
			//Ignore all for now
			t.printStackTrace(System.err);
		}
		return aSB.toString();
	}
	
	public static void main(String[] args) {
		try {
			Vector<NSTaggedSent> aTSs = new Vector<NSTaggedSent>();
			NSTaggedSent aTS = new NSTaggedSent();
			aTS.text = 
					"‘Libéraux contre populistes' : la médiatisation à outrance de ce clivage vise à contraindre les populations à choisir l'un de ces deux maux. ➟ (In. ‘Tous populistes !' #MDV164)";
			aTSs.add(aTS);
			NSTaggedSent aTS2 = new NSTaggedSent();
			aTS2.text = "Ceci est une deuxième phrase pour tester.";
			aTSs.add(aTS2);
			Thread aTh = ClientSpacy.getTagBatch(aTSs, "fr",new Vector<Vector<NSChunkerRule>>());
			aTh.join();
			System.out.println("POS: "+aTS.idxPos);
			System.out.println("POS: "+aTS2.idxPos);
		}
		catch(Throwable t) {
			t.printStackTrace(System.err);
		}
	}

}
