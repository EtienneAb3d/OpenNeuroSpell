#!/bin/bash
#!/usr/bin/env python3.6

sudo apt install python3
sudo apt install python3-pip
sudo apt install virtualenv

pushd ~/
virtualenv --system-site-packages -p python3.6 ./venvNS
. ./venvNS/bin/activate
popd

pip3 install -r requirements.txt
#download/refresh models
python3.6 -m spacy download en_core_web_sm
python3.6 -m spacy download fr_core_news_sm
