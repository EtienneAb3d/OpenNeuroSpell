# OpenNeuroSpell
OpenNeuroSpell contains parts of NeuroSpell (http://neurospell.com/en.php) released as open-source. More code will be published as soon as proprietary parts will be rewritten.

## A- NSChunker

NSChunker is a rule-based iterative label rewriter / aggregator. Each rule takes into account the POS-tagging, the extended TAG, and the text of words. Rules are organised in independant layers.

NSChunker is combining spaCy and polyglot analysers for high quality results.

The first version is available with a draft rule set for french.

### 1- Install and run spaCy and polyglot servers

NShunker is using spaCy and polyglot POS-tagger.
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
Paris NOUN F=false P=false posExt=PROPN
se PRON F=false P=false posExt=PRON
rue VERB F=false P=false posExt=VERB
sur ADP F=false P=false posExt=ADP
le sucré NOUN F=false P=false posExt=DET NOUN
, PUNCT F=false P=false posExt=PUNCT
et CONJ F=false P=false posExt=CONJ
les chefs pâtissiers NOUN F=false P=true posExt=DET NOUN NOUN_ADJ
sortent VERB F=false P=true posExt=VERB
de ADP F=false P=false posExt=ADP
l' ombre des maîtres du salés NOUN F=true P=true posExt=DET NOUN DET NOUN DET NOUN
. PUNCT F=false P=false posExt=PUNCT
```
