#!/bin/bash
#!/usr/bin/env python3

sudo apt install python3
sudo apt install python3-pip
sudo apt install virtualenv

pushd ~/
virtualenv --system-site-packages -p python3 ./venvNS
. ./venvNS/bin/activate
popd

pip3 install -r requirements.txt

pip3 install -U spacy[cuda92]

# Impossible to install with latest releases
#pip3 install sense2vec==1.0.0a0

# download/refresh models (similarity doesn't work with small "sm" models)
python3 -m spacy download en_core_web_sm
python3 -m spacy download fr_core_news_sm

#python3 -m spacy download en_core_web_md
#python3 -m spacy download en-vectors-web-lg
