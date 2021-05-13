#!/usr/bin/python3 -S
# -*- coding: utf-8 -*-

import sys, codecs, re, unicodedata
import xml.etree.ElementTree as ET
from collections import defaultdict
from os import listdir
from os.path import isfile, join


areas = open('arees-tematiques.txt', 'r')
lines = areas.readlines()
areasDict = {}
for line in lines:
	parts=line.split("=")
	areasDict[parts[0]]=parts[1].rstrip()

termcatDict = {}

folder="./xml-dicts/"

myfiles = listdir(folder)
for file in myfiles:
	filePath = join(folder, file)
	if isfile(filePath) and file.endswith("xml") and "cdlproductesinformatics" not in file and "cdlnomsdepeixos.xml" not in file:

		#tree = ET.parse(sys.argv[1])
		#tree = ET.parse("xml-dicts/wdlseguretatviaria.xml")
		tree = ET.parse(filePath)
		root = tree.getroot()


		for f in root.iter('fitxa'):
			wordList=[]

			#Comprova si hi ha dades
			countCat=0
			for d in f.iter('denominacio'):
				if (d.attrib.get('llengua') == 'ca'):
					countCat = countCat+1
					text = d.text
					if "| " in text:
						text = text.replace("| ", "(FEM ")
						text = text +")"
					wordList.append(text)
			
			if countCat<2:
				continue
			wordList.sort()
			categoria = "n"
			for d in f.iter('denominacio'):
				if d.attrib.get('categoria').startswith('adj'):
					categoria = "adj"
				if d.attrib.get('categoria').startswith('v'):
					categoria = "v"
				if d.attrib.get('categoria').startswith('inter'):
					categoria = "ij"
			for d in f.iter('denominacio'):
				if (d.attrib.get('llengua') == 'ca'):
					categoria = d.attrib.get('categoria');
					if categoria == "n m":
						categoria = "n"
					if categoria == "n m pl":
						categoria = "n"
					if categoria == "n f pl":
						categoria = "n"
					if categoria == "n f":
						categoria = "n"
					if categoria == "n m, f":
						categoria = "n"
					if categoria == "n m/f":
						categoria = "n"
					if categoria == "v tr":
						categoria = "v"
					if categoria == "v intr":
						categoria = "v"
					if categoria == "interj":
						categoria = "ij"
					if categoria == "v intr pron":
						categoria = "v"
					if categoria == "v pron":
						categoria = "v"
					if categoria == "v prep pron":
						categoria = "v"
					if categoria == "v prep":
						categoria = "v"
					if categoria == "v tr/prep":
						categoria = "v"
	
			#print ("-"+categoria+" (Termcat: "+areasDict[file]+"): " + catWords)
			#print ("-: " + catWords)
			catWords = ", ".join(wordList)
			if catWords in termcatDict:
				if termcatDict[catWords].endswith("repertoris multidisciplinaris") and not areasDict[file]=="repertoris multidisciplinaris":
					termcatDict[catWords] = "-"+categoria+" (Termcat: "+areasDict[file]
				elif not termcatDict[catWords].endswith(areasDict[file]) and not areasDict[file]=="repertoris multidisciplinaris":
					termcatDict[catWords] = termcatDict[catWords] + "|"+areasDict[file]
			else:
				termcatDict[catWords] = "-"+categoria+" (Termcat: "+areasDict[file]
		

for key in termcatDict:
	print (termcatDict[key] + "): " + key)