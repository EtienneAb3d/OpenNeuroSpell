# OpenNeuroSpell
OpenNeuroSpell contains parts of NeuroSpell (http://neurospell.com/en.php) released as open-source. More code will be published as soon as proprietary parts will be rewritten.

## A- NSChunker

NSChunker is combining spaCy, polyglot, and LanguageTool analysers for POS re-disambiguation, high quality chunking, and nominal entities extraction.

NSChunker is a rule-based iterative label rewriter / aggregator. Each rule takes into account the POS-tagging, the extended TAG, and the text of words. Rules are organised in independant layers.

This version is available with rule sets for French and English.

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
According	VBG_VERB_ADJ-VBG	=>	VBG
to	ADP	=>	ADP
research	NOUN	=>	NOUN
by	ADP	=>	ADP
Music	PROPN	=>	PROPN
Week	PROPN	=>	PROPN
,	PUNCT	=>	PUNCT
it	PRON	=>	PRON
took	VERB	=>	VERB
an	DET	=>	DET
average	NOUN	=>	NOUN
of	ADP	=>	ADP
5.34	NUM	=>	NUM
people	NOUN	=>	NOUN
to	PART	=>	PART
write	VB_VERB_VB-VERB	=>	VB
last	ADJ	=>	ADJ
year	NOUN_NUM_NOUN	=>	NOUN
's	POS	=>	POS
Top	PROPN_NOUN_ADJ-NOUN-VB-VERB	=>	PROPN
100	NUM	=>	NUM
biggest	ADJ	=>	ADJ
singles	NOUN	=>	NOUN
.	PUNCT	=>	PUNCT
__________
Chunks:
According to research by Music Week	ADJ F=false P=false posExt=VBG ADP NOUN ADP PROPN PROPN
,	PUNCT F=false P=false posExt=PUNCT
it	PRON F=false P=false posExt=PRON
took	VERB F=false P=false posExt=VERB
an average of 5.34 people	NOUN F=false P=false posExt=DET NOUN ADP NUM NOUN
to write	VB F=false P=false posExt=PART VB
last year 's Top 100 biggest singles	NOUN F=false P=false posExt=ADJ NOUN POS PROPN NUM ADJ NOUN
.	PUNCT F=false P=false posExt=PUNCT
__________
Extracts:
Music Week	~PROPN F=false P=false posExt=PROPN PROPN
average of 5.34 people	~NOUN F=false P=false posExt=NOUN ADP NUM NOUN
last year	~NOUN F=false P=false posExt=ADJ NOUN
Top 100	~PROPN F=false P=false posExt=PROPN NUM
biggest singles	~NOUN F=false P=false posExt=ADJ NOUN
```
### 4- High speed POS-tagging / chunking / nominal entities extraction

- start several copies of spaCy using <code>startN.sh</code> (adapt it according to your number of processors)
- define the number of spaCy servers launched with <code>ClientSpacy.instances = 32;</code>
- create a chunker without polyglot <code>new NSChunker("fr",false)</code>


