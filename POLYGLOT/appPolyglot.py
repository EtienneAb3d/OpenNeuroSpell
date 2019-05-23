# coding: utf8
from __future__ import unicode_literals

import hug
from hug_middleware_cors import CORSMiddleware
from polyglot.text import Text
from polyglot.tag import NEChunker, POSTagger

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
    lng: str,
):
    print("TAG=====")
    print("TEXT="+text)
    print("LNG="+lng)
    sent = Text(text, hint_language_code=lng)
    return sent.pos_tags

@hug.post("/ent")
def ent(
    text: str,
    lng: str,
):
    print("ENT=====")
    print("TEXT="+text)
    print("LNG="+lng)
    sents = Text(text, hint_language_code=lng)
    return [{"ent": ent, "label": ent.tag}
        for ent in sents.entities
    ]

if __name__ == "__main__":
    import waitress

    app = hug.API(__name__)
    app.http.add_middleware(CORSMiddleware(app))
    waitress.serve(__hug_wsgi__, port=8081)
