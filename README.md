# OpenNeuroSpell
OpenNeuroSpell contains parts of NeuroSpell (http://neurospell.com/en.php) released as open-source. More code will be published as soon as proprietary parts will be rewritten.

## A- NSChunker

NSChunker is combining spaCy and polyglot analysers for high quality chunking, POS re-disambiguation, and entities extraction.

NSChunker is a rule-based iterative label rewriter / aggregator. Each rule takes into account the POS-tagging, the extended TAG, and the text of words. Rules are organised in independant layers.

The first version is available with a draft rule set for French.

### 1- Install and run spaCy and polyglot servers

NSChunker is using spaCy and polyglot POS-tagger.
```
  cd SPACY
  bash install.sh
  bash start.sh
  cd ..
  cd POLYGLOT
  bash install.sh
  bash start.sh
  cd ..
```
### 2- Test NSChunker

Add Java codes in a Java project, add <code>json-simple-1.1.1.jar</code> and <code>MindProd-http.jar</code> in the classpath.

Run <code>com.ns.NSChunker.main()</code>. You should get this final chunking:

```
__________
Disambiguations:
À	ADP	=>	ADP
Saint	PROPN_NOUN	=>	PROPN
-	PUNCT	=>	PROPN
Nazaire	NOUN_PROPN	=>	PROPN
,	PUNCT	=>	PUNCT
sept	NUM	=>	NUM
salariés	NOUN	=>	NOUN
de	ADP	=>	ADP
l'	DET	=>	DET
entreprise	NOUN	=>	NOUN
HPR	PROPN_NUM	=>	PROPN
,	PUNCT	=>	PUNCT
filiale	NOUN	=>	NOUN
française	ADJ	=>	ADJ
de	ADP	=>	ADP
la	DET	=>	DET
société	NOUN	=>	NOUN
norvégienne	VERB_ADJ	=>	ADJ
Havyard	PROPN_NUM	=>	PROPN
ne	ADV	=>	ADV
sont	AUX	=>	AUX
plus	ADV	=>	ADV
payés	VERB	=>	VBN
depuis	ADP	=>	ADP
deux	NUM	=>	NUM
mois	NOUN	=>	NOUN
.	PUNCT	=>	PUNCT
__________
Chunks:
À	ADP F=false P=false posExt=ADP
Saint - Nazaire	PROPN F=false P=false posExt=PROPN PROPN PROPN
,	PUNCT F=false P=false posExt=PUNCT
sept salariés de l' entreprise HPR	NOUN F=true P=true posExt=NUM NOUN ADP DET NOUN PROPN
,	PUNCT F=false P=false posExt=PUNCT
filiale française de la société norvégienne Havyard	NOUN F=true P=false posExt=NOUN ADJ ADP DET NOUN ADJ PROPN
ne sont plus payés	VERB F=false P=true posExt=ADV AUX ADV VBN
depuis	ADP F=false P=false posExt=ADP
deux mois	NOUN F=false P=true posExt=NUM NOUN
.	PUNCT F=false P=false posExt=PUNCT
__________
Extracts:
Saint - Nazaire	~PROPN F=false P=false posExt=PROPN PROPN PROPN
salariés de l' entreprise	~NOUN F=true P=true posExt=NOUN ADP DET NOUN
HPR	~PROPN F=false P=false posExt=PROPN
filiale française de la société norvégienne	~NOUN F=true P=false posExt=NOUN ADJ ADP DET NOUN ADJ
Havyard	~PROPN F=false P=false posExt=PROPN
mois	~NOUN F=false P=true posExt=NOUN
```
