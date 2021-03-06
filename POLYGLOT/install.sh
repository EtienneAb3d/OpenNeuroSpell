#!/bin/bash
#!/usr/bin/env python3

#Already done with spaCy: sudo apt install virtualenv

pushd ~/
#Already done with spaCy: virtualenv --system-site-packages -p python3 ./venvNS
. ./venvNS/bin/activate
popd

sudo apt-get install python-numpy libicu-dev
pip3 install pycld2 morfessor polyglot pyicu
polyglot download embeddings2.fr pos2.fr ner2.fr
polyglot download embeddings2.en pos2.en ner2.en
pip3 install -r requirements.txt
