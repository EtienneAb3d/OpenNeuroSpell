# coding: utf8
from __future__ import unicode_literals

import hug
from hug_middleware_cors import CORSMiddleware
from polyglot.text import Text
from polyglot.tag import NEChunker, POSTagger
from polyglot.mapping import Embedding
from polyglot.mapping import CaseExpander
from os.path import expanduser

@hug.post("/tagOnly")
def tagOnly(
    text: str,
    lng: str,
):
    print("TAG=====(tagOnly)")
    print("TEXT="+text)
    print("LNG="+lng)
    tagger = POSTagger(lang=lng)
    lines = text.split("\n")
    taggeds = [];
    for line in lines:
        words = line.strip().split()
        tagged = tagger.annotate(words)
        if len(taggeds) > 0: 
            taggeds.extend([["\n","NL"]]);
        taggeds.extend([[w,p] for w, p in tagged]);
    return taggeds

@hug.post("/tag")
def tag(
    text: str,
    lng: str
):
    print("TAG=====")
    print("TEXT="+text)
    print("LNG="+lng)
    sent = Text(text, hint_language_code=lng)
    return sent.pos_tags

@hug.post("/ent")
def ent(
    text: str,
    lng: str
):
    print("ENT=====")
    print("TEXT="+text)
    print("LNG="+lng)
    sents = Text(text, hint_language_code=lng)
    return [{"ent": ent, "label": ent.tag}
        for ent in sents.entities
    ]
    
EMBEDDINGS = {
}

def getEmbeddings(lng):
	if lng not in EMBEDDINGS:
		home = expanduser("~")
		embeddings = Embedding.load(home+"/polyglot_data/embeddings2/"+lng+"/embeddings_pkl.tar.bz2")
		embeddings.apply_expansion(CaseExpander)
		EMBEDDINGS[lng] = embeddings
	return EMBEDDINGS[lng]

@hug.post("/syn")
def syn(
	text: str,
	lng: str
):
    print("SYN=====")
    print("TEXT="+text)
    print("LNG="+lng)
    embeddings = getEmbeddings(lng)
    nAll = {}
    for w in text.split(" ") :
        if w in embeddings:
            nAll[w] = embeddings.nearest_neighbors(w)
            for n in nAll[w]:
                print(w+": "+n)
    return nAll

if __name__ == "__main__":
    import waitress

    app = hug.API(__name__)
    app.http.add_middleware(CORSMiddleware(app))
    waitress.serve(__hug_wsgi__, port=8081)
