package com.ns;

public class NSUtils {
	static char _BUF[]=new char[10*1000*1000];
	private final static String DIACRITICS_Str = "àáâãäåçèéêëìíîïðñòóôõöùúûüýÿ" + "ÀÁÂÃÄÅÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖÙÚÛÜÝ";
	private final static String NOACCENT_Str =   "aaaaaaceeeeiiiionooooouuuuyy" + "AAAAAACEEEEIIIIDNOOOOOUUUUY";
	private final static char[] VOWEL = new char[NOACCENT_Str.length()];
	static {
		for(int c = 0;c < NOACCENT_Str.length();c++){
			VOWEL[c] = NOACCENT_Str.charAt(c);
		}
	}

	public static String nbsp = ""+(char)160;
	public static String thinsp = ""+(char)8201;
	public static String nnbsp = ""+(char)8239;
	public static String ensp = ""+(char)8194;
	public static String emsp = ""+(char)8195;
	public static String specialsps = nbsp+thinsp+nnbsp+ensp+emsp;
	public static String allapos = "'ʼ’‘`´";
	
	public static String toPoor(String text){
		//EM 31/10/2017 : need to be sync, because BUFF is static !
		synchronized (_BUF) {
			int aLen = text.length();
			text.getChars(0,aLen,_BUF,0);
			int l,d;
			for(l=0; l<aLen; l++) 
			{
				if((d = DIACRITICS_Str.indexOf(_BUF[l])) >= 0)
				{
					_BUF[l] = VOWEL[d];
				}
			}
			return new String(_BUF,0,aLen); 
		}
	}
	
}
