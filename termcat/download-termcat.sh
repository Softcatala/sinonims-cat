#!/bin/bash
rm -rf xml-dicts
mkdir xml-dicts
cd xml-dicts
for i in {0..15}
do
	wget https://www.termcat.cat/ca/terminologia-oberta?page=${i}
done
rm termcat-dicts.txt
rm area-dicts.txt
for i in {0..15}
do
	grep 'href=".*.xml"' terminologia*page=${i} | sed -E 's/.*href=\"(.*\.xml)\".*/\1/' >> termcat-dicts.txt
	grep 'class="service-item__tag--main"' terminologia*page=${i} | sed -E 's/.*>&lt;(.*)&gt;<.*/\1/' >> area-dicts.txt 
done
rm terminologia-oberta*
wget -i termcat-dicts.txt
