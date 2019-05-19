# OpenNeuroSpell
OpenNeuroSpell contains parts of NeuroSpell (http://neurospell.com/en.php) released as open-source. More code will be published as soon as proprietary parts will be rewritten.

## A- NSChunker

NSChunker is combining spaCy, polyglot, and LanguageTool analysers for POS re-disambiguation, high quality chunking, and nominal entities extraction.

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
### 2- Install LanguageTool

Get all LanguageTool code and its dependencies.

### 3- Test NSChunker

Add Java codes in a Java project. Add all LanguageTool code and its dependencies in the classpath. Add <code>json-simple-1.1.1.jar</code> and <code>MindProd-http.jar</code> in the classpath. 

Run <code>com.ns.NSChunker.main()</code>. You should get this final chunking:

```
__________
Disambiguations:
Longtemps	ADV	=>	ADV
,	PUNCT	=>	PUNCT
les	DET	=>	DET
gens	NOUN	=>	NOUN
se	PRON	=>	PRON
sont	AUX	=>	AUX
dit	VERB	=>	VBN
"	PUNCT	=>	QUOT
on	PRON	=>	PRON
leur	PRON	=>	PRON
fait	VERB	=>	VERB
confiance	NOUN	=>	NOUN
,	PUNCT	=>	PUNCT
ils	PRON	=>	PRON
vont	VERB_AUX	=>	VERB
bien	ADV	=>	ADV
trouver	VERB	=>	VB
quelque	DET	=>	DET
chose	NOUN	=>	NOUN
"	PUNCT	=>	QUOT
et	CCONJ_CONJ	=>	CONJ
à	ADP	=>	ADP
la	DET	=>	DET
COP24	PROPN_NUM	=>	PROPN
,	PUNCT	=>	PUNCT
le	DET	=>	DET
GIEC	NOUN_X	=>	PROPN
a	AUX	=>	AUX
dit	VERB	=>	VBN
qu'	SCONJ	=>	SCONJ
il	PRON	=>	PRON
fallait	VERB_AUX	=>	VERB
revenir	VERB	=>	VB
sur	ADP	=>	ADP
les	DET	=>	DET
accords	NOUN	=>	NOUN
de	ADP	=>	ADP
Paris	PROPN	=>	PROPN
et	CCONJ_CONJ	=>	CONJ
ne	ADV	=>	ADV
surtout	ADV	=>	ADV
pas	ADV	=>	ADV
dépasser	VERB	=>	VB
+1,5	DET	=>	NUM
°	NOUN	=>	NOUN
C	NOUN_NUM	=>	NOUN
de	ADP	=>	ADP
température	NOUN	=>	NOUN
.	PUNCT	=>	PUNCT
__________
Chunks:
Longtemps	ADV F=false P=false posExt=ADV
,	PUNCT F=false P=false posExt=PUNCT
les gens	NOUN F=false P=true posExt=DET NOUN
se	PRON F=false P=false posExt=PRON
sont dit	VERB F=false P=true posExt=AUX VBN
"	QUOT F=false P=false posExt=QUOT
on leur	PRON F=false P=true posExt=PRON PRON
fait	VERB F=false P=false posExt=VERB
confiance	NOUN F=true P=false posExt=NOUN
,	PUNCT F=false P=false posExt=PUNCT
ils	PRON F=false P=true posExt=PRON
vont bien trouver	VERB F=false P=true posExt=VERB ADV VB
quelque chose	NOUN F=true P=false posExt=DET NOUN
"	QUOT F=false P=false posExt=QUOT
et	CONJ F=false P=false posExt=CONJ
à	ADP F=false P=false posExt=ADP
la COP24	NOUN F=true P=false posExt=DET PROPN
,	PUNCT F=false P=false posExt=PUNCT
le GIEC	NOUN F=false P=false posExt=DET PROPN
a dit	VERB F=false P=false posExt=AUX VBN
qu'	SCONJ F=false P=false posExt=SCONJ
il	PRON F=false P=false posExt=PRON
fallait revenir	VERB F=false P=false posExt=VERB VB
sur	ADP F=false P=false posExt=ADP
les accords de Paris	NOUN F=false P=true posExt=DET NOUN ADP PROPN
et	CONJ F=false P=false posExt=CONJ
ne surtout pas	ADV F=false P=false posExt=ADV ADV ADV
dépasser	VB F=false P=false posExt=VB
+1,5 ° C de température	NOUN F=true P=false posExt=NUM NOUN NOUN ADP NOUN
.	PUNCT F=false P=false posExt=PUNCT
__________
Extracts:
gens	~NOUN F=false P=true posExt=NOUN
confiance	~NOUN F=true P=false posExt=NOUN
chose	~NOUN F=true P=false posExt=NOUN
COP24	~PROPN F=true P=false posExt=PROPN
GIEC	~PROPN F=false P=false posExt=PROPN
accords	~NOUN F=false P=true posExt=NOUN
Paris	~PROPN F=false P=false posExt=PROPN
+1,5 ° C de température	~NOUN F=true P=false posExt=NUM NOUN NOUN ADP NOUN
```
### 4- High speed POS-tagging / chunking / nominal entities extraction

- start several copies of spaCy on a GPU card using <code>startN.sh</code>
- define the number of spaCy servers launched with <code>ClientSpacy.instances = 10;</code>
- create a chunker without polyglot <code>new NSChunker("fr",false)</code>

Typical processing rate : 700K sentences / hour on a 2080Ti card

