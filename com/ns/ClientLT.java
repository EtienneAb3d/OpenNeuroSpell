package com.ns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.MultiThreadedJLanguageTool;

import com.ns.NSChunker.NSChunkerRule;

public class ClientLT {
	static final boolean _DEBUG = false;

	static HashMap<String,Vector<JLanguageTool>> langToolPools = new HashMap<String,Vector<JLanguageTool>>();
	
	String lng = null;

	public static JLanguageTool getLT(String aLng) throws Exception {
		Vector<JLanguageTool> aLangToolPool = null;
		synchronized(langToolPools){
			aLangToolPool = langToolPools.get(aLng);
			if(aLangToolPool == null) {
				aLangToolPool = new Vector<JLanguageTool>();
				langToolPools.put(aLng,aLangToolPool);
			}
		}
		JLanguageTool aLangTool = null;
		synchronized(aLangToolPool){
			if(aLangToolPool.size() > 0) {
				aLangTool = aLangToolPool.remove(0);
			}
			else {
				aLangTool = new MultiThreadedJLanguageTool(Languages.getLanguageForShortCode(aLng),4);
			}
		}
		return aLangTool;
	}
	
	public static void releaseLT(String aLng,JLanguageTool aLangTool) throws Exception {
		Vector<JLanguageTool> aLangToolPool = null;
		synchronized(langToolPools){
			aLangToolPool = langToolPools.get(aLng);
			if(aLangToolPool == null) {
				//??
				aLangToolPool = new Vector<JLanguageTool>();
				langToolPools.put(aLng,aLangToolPool);
			}
		}
		synchronized(aLangToolPool) {
			aLangToolPool.add(aLangTool);
			aLangTool = null;
		}
	}
	
	public static Thread getTagBatch(final TaggedSent aTS,final String aLng,Vector<Vector<NSChunkerRule>> aLtLayers) throws Exception {
		if(_DEBUG || NSChunker._DEBUG_ALL) {
			System.out.println("##########TAG polyglot");
		}

		Thread aTh = new Thread(new Runnable(){
			@Override
			public void run() {
				try{
					JLanguageTool aLangTool = ClientLT.getLT(aLng);
					ArrayList<String> aSents = new ArrayList<String>();
					String aTxt = aTS.text.replaceAll("["+NSUtils.allapos+"]", "'");
					aSents.add(aTxt);
					List<AnalyzedSentence> aASents = aLangTool.analyzeSentences(aSents);
					
					ClientLT.releaseLT(aLng,aLangTool);
					
					int aCountW = 0;
					StringBuffer aPosSB = new StringBuffer();
					for(AnalyzedSentence aAS : aASents) {
						for(AnalyzedTokenReadings aATR : aAS.getTokens()) {
								NSChunkerWord aW = new NSChunkerWord();
								aW.word = aATR.getToken();
								if(aW.word == null || aW.word.trim().isEmpty()) {
									//Beg/Start/Spc ?
									continue;
								}
								if(_DEBUG || NSChunker._DEBUG_ALL) {
									System.out.println("LTW: "+aATR.toString());
								}
								StringBuffer aPOSSB = new StringBuffer();
								StringBuffer aTagSB = new StringBuffer();
								StringBuffer aLemmaSB = new StringBuffer();
								for(AnalyzedToken aAT : aATR.getReadings()) {
									String aPOSTag = aAT.getPOSTag();
									if(aPOSTag == null || aPOSTag.matches("(SENT_END|PARA_END|<[^>]*>)")) {
										//Ignore this POS
										continue;
									}
									String aLemma = aAT.getLemma();
									if(aPOSTag != null && aPOSTag.length() > 0) {
										String aPOS = aPOSTag;
										if(aPOSTag.indexOf(" ") > 0) {
											aPOS = aPOSTag.substring(0, aPOSTag.indexOf(" "));
										}
										aPOSTag = aLemma+"\t"+aPOSTag;
										aTagSB.append((aTagSB.length()>0?",":"")+aPOSTag);
										for(Vector<NSChunkerRule> aLayer : aLtLayers) {
											for(NSChunkerRule aR : aLayer) {
												if(aR.patternPOS.matcher(aPOS).matches()) {
													if(aR.patternText != null && !aR.patternText.matcher(aW.word).matches()) {
														//Ignore
														continue;
													}
													if(aR.patternTag != null && !aR.patternTag.matcher(aPOSTag).matches()) {
														//Ignore
														continue;
													}
													aPOS = aR.pos;
													break;
												}
											}
										}
										if(aPOSSB.indexOf(" "+aPOS+" ") < 0) {
											aPOSSB.append(" "+aPOS+" ");
										}
									}
									aLemmaSB.append((aLemmaSB.length() > 0 ? ",":"")+aLemma);
								}
								aW.lemma = aLemmaSB.toString().trim();
								aW.pos = aPOSSB.toString().replaceAll(" +", " ").trim();
								aW.tag = aTagSB.toString().trim();
								aTS.words.add(aW);
								aPosSB.append(" "+aCountW+","+aCountW+aW.pos+" ");
								aCountW++;
						}
					}
					aTS.idxPos = aPosSB.toString();
				}
				catch(Throwable t) {
					t.printStackTrace(System.err);
				}
			}
		},"LanguageTool");
		aTh.start();
		return aTh;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
