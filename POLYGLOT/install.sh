#!/bin/bash
#!/usr/bin/env python3.6

#Already done with spaCy: sudo apt install virtualenv

pushd ~/
#Already done with spaCy: virtualenv --system-site-packages -p python3.6 ./venvNS
. ./venvNS/bin/activate
popd

sudo apt-get install python-numpy libicu-dev
pip3 install polyglot
polyglot download embeddings2.fr pos2.fr
pip3 install -r requirements.txt
