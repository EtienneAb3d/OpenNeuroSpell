# coding: utf8
from __future__ import unicode_literals

import hug
from hug_middleware_cors import CORSMiddleware
import spacy
import sys

spacy.prefer_gpu()

#MODELS = {
#    "en_core_web_sm": spacy.load("en_core_web_sm"),
#    "en_core_web_md": spacy.load("en_core_web_md"),
#    "en_core_web_lg": spacy.load("en_core_web_lg"),
#    "de_core_news_sm": spacy.load("de_core_news_sm"),
#    "es_core_news_sm": spacy.load("es_core_news_sm"),
#    "pt_core_news_sm": spacy.load("pt_core_news_sm"),
#    "fr_core_news_sm": spacy.load("fr_core_news_sm"),
#    "fr_core_news_md": spacy.load("fr_core_news_md"),
#    "it_core_news_sm": spacy.load("it_core_news_sm"),
#    "nl_core_news_sm": spacy.load("nl_core_news_sm"),
#}

MODELS = {
}

def getModel(model_name):
    if model_name not in MODELS:
        nlp = spacy.load(model_name)
        MODELS[model_name] = nlp
    return MODELS[model_name]

def get_model_desc(nlp, model_name):
    """Get human-readable model name, language name and version."""
    lang_cls = spacy.util.get_lang_class(nlp.lang)
    lang_name = lang_cls.__name__
    model_version = nlp.meta["version"]
    return "{} - {} (v{})".format(lang_name, model_name, model_version)


@hug.get("/models")
def models():
    return {name: get_model_desc(nlp, name) for name, nlp in MODELS.items()}


@hug.post("/dep")
def dep(
    text: str,
    model: str,
    collapse_punctuation: bool = False,
    collapse_phrases: bool = False,
):
    """Get dependencies for displaCy visualizer."""
    nlp = getModel(model)
    doc = nlp(text)
    if collapse_phrases:
        for np in list(doc.noun_chunks):
            np.merge(tag=np.root.tag_, lemma=np.root.lemma_, ent_type=np.root.ent_type_)
    options = {"collapse_punct": collapse_punctuation}
    return spacy.displacy.parse_deps(doc, options)


POSMODELS = {
}

def getPOSModel(model_name):
    if model_name not in POSMODELS:
        nlp = spacy.load(model_name)
        nlp.remove_pipe("parser")
        nlp.remove_pipe("ner")
        POSMODELS[model_name] = nlp
    return POSMODELS[model_name]
    

@hug.post("/tag")
def tag(
    text: str,
    model: str,
):
    print("TAG=====")
    print("TEXT="+text)
    print("MODEL="+model)
    nlp = getPOSModel(model)
    doc = nlp(text)
    return [[token.text, token.lemma_, token.pos_, token.tag_, token.dep_,
            token.shape_, token.is_alpha, token.is_stop]
            for token in doc
    ]

@hug.post("/ent")
def ent(text: str, model: str):
    """Get entities for displaCy ENT visualizer."""
    print("ENT=====")
    print("TEXT="+text)
    print("MODEL="+model)
    nlp = getModel(model)
    doc = nlp(text)
    return [
        {"start": ent.start_char, "end": ent.end_char, "label": ent.label_}
        for ent in doc.ents
    ]

def takeSecond(elem):
    return elem[1]

def most_similar(word):
    queries = [(w, word.similarity(w)) for w in word.vocab if 
        w.is_alpha == word.is_alpha 
        and w.has_vector == word.has_vector
        and w.suffix_ == word.suffix_
        and w.shape == word.shape
        and w.is_lower == word.is_lower 
        and w.prob >= -20]
    by_similarity = sorted(queries, key= takeSecond, reverse=True)
    return by_similarity[:10]

@hug.post("/syn")
def syn(
	text: str,
	model: str
):
	print("SYN=====")
	print("TEXT="+text)
	print("MODEL="+model)
	nlp = getModel(model)
	nAll = {}
	for w in text.split(" ") :
		sims = most_similar(nlp.vocab[w])
		simsTxts = [];
		for s in sims:
			print(w+"/"+s[0].lower_+" "+ "%.3f" % s[1])
			simsTxts.append([s[0].lower_,s[1]]);
		nAll[w] = simsTxts
	return nAll

if __name__ == "__main__":
    import waitress

    app = hug.API(__name__)
    app.http.add_middleware(CORSMiddleware(app))
    if len(sys.argv) == 1 :
        waitress.serve(__hug_wsgi__, port=8091)
    else :
        waitress.serve(__hug_wsgi__, port=(8091+int(sys.argv[1])))
    
