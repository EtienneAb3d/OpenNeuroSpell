#!/bin/bash
#!/usr/bin/env python3.6

pushd ~/
. ./venvNS/bin/activate
popd

python3.6 appSpaCy.py 0&
python3.6 appSpaCy.py 1&
python3.6 appSpaCy.py 2&
python3.6 appSpaCy.py 3&
python3.6 appSpaCy.py 4&
python3.6 appSpaCy.py 5&
python3.6 appSpaCy.py 6&
python3.6 appSpaCy.py 7&
python3.6 appSpaCy.py 8&
python3.6 appSpaCy.py 9&

