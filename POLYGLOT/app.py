# coding: utf8
from __future__ import unicode_literals

import hug
from hug_middleware_cors import CORSMiddleware
from polyglot.text import Text

@hug.post("/tag")
def tag(
    text: str,
    lng: str,
):
    print("TEXT="+text)
    print("LNG="+lng)
    text = Text(text, hint_language_code=lng)
    return text.pos_tags

if __name__ == "__main__":
    import waitress

    app = hug.API(__name__)
    app.http.add_middleware(CORSMiddleware(app))
    waitress.serve(__hug_wsgi__, port=8082)
