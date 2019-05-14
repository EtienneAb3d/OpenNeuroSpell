# OpenNeuroSpell
OpenNeuroSpell contains parts of NeuroSpell (http://neurospell.com/en.php) released as open-source. More code will be published as soon as proprietary parts will be rewritten.

## A- NSChunker

NSChunker is combining spaCy and polyglot analysers for high quality chunking and POS re-disambiguation.

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
Le	DET	=>	DET
président	NOUN	=>	NOUN
du	DET	=>	DET
CCR	CCONJ_PROPN	=>	PROPN
souligne	VERB	=>	VERB
cependant	ADV	=>	ADV
que	SCONJ	=>	SCONJ
ce	DET	=>	DET
premier	ADJ	=>	ADJ
pas	ADV_NOUN	=>	NOUN
doit	VERB_AUX	=>	VERB
être	AUX	=>	VBX
suivi	VERB	=>	VBN
par	ADP	=>	ADP
des	DET	=>	DET
«	ADJ_PUNCT	=>	PUNCT
actions	NOUN	=>	NOUN
engagées	VERB_ADJ	=>	ADJ
,	PUNCT	=>	PUNCT
transparentes	ADJ	=>	ADJ
et	CCONJ_CONJ	=>	CONJ
soutenues	ADJ_VERB	=>	ADJ
»	ADP_PUNCT	=>	PUNCT
des	DET	=>	DET
parties	NOUN	=>	NOUN
yéménites	ADJ	=>	ADJ
pour	ADP	=>	ADP
qu'	SCONJ	=>	SCONJ
elles	PRON	=>	PRON
s'	PRON	=>	PRON
acquittent	VERB	=>	VERB
pleinement	ADV	=>	ADV
de	ADP	=>	ADP
leurs	DET	=>	DET
obligations	NOUN	=>	NOUN
.	PUNCT	=>	PUNCT
__________
Chunks:
Le président du CCR	NOUN F=false P=false posExt=DET NOUN DET PROPN
souligne	VERB F=false P=false posExt=VERB
cependant	ADV F=false P=false posExt=ADV
que	SCONJ F=false P=false posExt=SCONJ
ce premier pas	NOUN F=false P=false posExt=DET ADJ NOUN
doit être suivi	VERB F=false P=false posExt=VERB VBX VBN
par	ADP F=false P=false posExt=ADP
des	DET F=false P=true posExt=DET
«	QBEG F=true P=true posExt=PUNCT
actions engagées	NOUN F=true P=true posExt=NOUN ADJ
,	PUNCT F=false P=false posExt=PUNCT
transparentes et soutenues	ADJ F=true P=true posExt=ADJ CONJ ADJ
»	QEND F=false P=false posExt=PUNCT
des parties yéménites	NOUN F=true P=true posExt=DET NOUN ADJ
pour	ADP F=false P=false posExt=ADP
qu'	SCONJ F=false P=false posExt=SCONJ
elles	PRON F=true P=true posExt=PRON
s'	PRON F=false P=false posExt=PRON
acquittent	VERB F=false P=true posExt=VERB
pleinement de	ADP F=false P=false posExt=ADV ADP
leurs obligations	NOUN F=true P=true posExt=DET NOUN
.	PUNCT F=false P=false posExt=PUNCT
```
