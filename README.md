# OpenNeuroSpell
OpenNeuroSpell contains parts of NeuroSpell (http://neurospell.com/en.php) released as open-source. More code will be published as soon as proprietary parts will be rewritten.
## A- Chunker

The first version is available with a draft rule set for french.

### 1- Install and run spaCy server

The chunker is using the spaCy POS-tagger.
```
  cd SPACY
  sh install.sh
  sh start.sh
```
### 2- Test chunker

Add <code>com.ns.NSChunker</code> in a Java projet, add <code>json-simple-1.1.1.jar</code> and <code>MindProd-http.jar</code> in the classpath.

Run <code>com.ns.NSChunker.main()</code>. You should get this final chunking:

```
Il y a quatre mois NOUN F=false P=true posExt=PRON PRON VERB NUM NOUN
, PUNCT F=false P=false posExt=PUNCT
nous NOUN F=false P=true posExt=PRON
avons acheté VERB F=false P=true posExt=AUX VERB
des vignes dans l' Oregon NOUN F=true P=true posExt=DET NOUN ADP DET PROPN
. PUNCT F=false P=false posExt=PUNCT
Nous NOUN F=false P=true posExt=PRON
sommes VERB F=false P=true posExt=AUX
désormais ADV F=false P=false posExt=ADV
un ­ « véritable » vignoble NOUN F=false P=false posExt=DET NOUN PUNCT ADJ PUNCT ADJ
. PUNCT F=false P=false posExt=PUNCT
```
