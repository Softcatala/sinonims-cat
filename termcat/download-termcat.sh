#!/bin/bash

rm -rf xml-dicts
mkdir xml-dicts
cd xml-dicts
for i in {0..15}
do
	wget https://www.termcat.cat/ca/terminologia-oberta?page=${i}
done
grep 'href=".*.xml"' terminologia* | sed -E 's/.*href=\"(.*\.xml)\".*/\1/' > termcat-dicts.txt
rm terminologia-oberta*
wget -i termcat-dicts.txt
