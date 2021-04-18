#!/usr/bin/python
# -*- encoding: utf-8 -*-
#
# Copyright (c) 2021 Joan Montané <jmontane@softcatala.org>
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public
# License as published by the Free Software Foundation; either
# version 2.0 of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this program; if not, write to the
# Free Software Foundation, Inc., 59 Temple Place - Suite 330,
# Boston, MA 02111-1307, USA.

import re
import icu
import os
import shutil

DATA = './dict/sinonims.txt'
OUTPUT = './results/'
OXT_DIR = './oxt/'
OXT_NAME = 'thesaurus-ca'
filename = 'th_ca_ES_v4'

collator = icu.Collator.createInstance(icu.Locale('ca_ES.UTF-8'))

def clean_word(word):

   bracket = word.find('(')
   if bracket > 0:
      clean_word = word[:bracket]
      clean_word = clean_word.strip()
      tag = word[bracket+1:-1]
   else:
      clean_word = word
      tag = ''

   return(clean_word.lower(), tag)

def parse_line(line):

   separator = line.find(':')
   category = line[:separator]

   cleaned_line = line[(separator+1):]
   cleaned_line = re.sub ('\\\,', ';', cleaned_line)
   cleaned_line = re.sub (' ?#TODO.*$', '', cleaned_line)
   cleaned_line = re.sub (',? ?#separar.*$', '', cleaned_line)
   cleaned_line = re.sub(',? ?\.\.\.*$', '', cleaned_line)
   cleaned_line = cleaned_line.replace('NOFEM', '')
   cleaned_line = re.sub('\( ', '(', cleaned_line)
   cleaned_line = re.sub(' \)', ')', cleaned_line)
   cleaned_line = re.sub(' \(\)', '', cleaned_line)
   cleaned_line = re.sub(' ', ' ', cleaned_line) 
   cleaned_line = re.sub(', ', ',', cleaned_line)
   cleaned_line = re.sub(',$', '', cleaned_line)
   cleaned_line = cleaned_line.strip()
   cleaned_line = re.sub (',', '|', cleaned_line)
   cleaned_line = re.sub (';', ',', cleaned_line)
   words = cleaned_line.split('|')

   words = sorted(words, key=collator.getSortKey)
   
   if len(words) <= 1:
      return 0, category, words
   else:
      return 1, category, words



def generate_output():

   oxt_idx = 'UTF-8' + '\n' + str(len(wordlist)) + '\n'
   oxt_dat = 'UTF-8' + '\n'
   
   foffset = 0
   foffset = foffset + len(oxt_dat.encode())

   for word in sorted(wordlist):
      oxt_idx = oxt_idx + word + '|' + str(foffset) + '\n'
      oxt_dat = oxt_dat + word + '|' + str(wordlist[word]) + '\n'
      oxt_dat = oxt_dat + wordmeanings[word]
      foffset = foffset + len(word.encode()) + 1 + len(str(wordlist[word]).encode()) + 1 + len( wordmeanings[word].encode())

   thes_idx = open(OUTPUT + filename + '.idx', "w")
   thes_idx.write(oxt_idx)
   thes_idx.close()
   thes_idx.close()

   thes_dat = open(OUTPUT + filename + '.dat', "w")
   thes_dat.write(oxt_dat)
   thes_dat.close()
   thes_dat.close()

   shutil.copyfile(OUTPUT + filename + '.idx', OXT_DIR + OXT_NAME + '/dictionaries/' + filename + '.idx')
   shutil.copyfile(OUTPUT + filename + '.dat', OXT_DIR + OXT_NAME + '/dictionaries/' + filename + '.dat')

   shutil.make_archive(OUTPUT + OXT_NAME, 'zip', OXT_DIR + OXT_NAME + '/')

   oxt_file = OUTPUT + OXT_NAME + '.zip' 
   base_name = os.path.splitext(oxt_file)[0]
   os.rename(oxt_file, base_name + '.oxt')

   return


wordlist = {}
wordmeanings = {}

with open(DATA, 'r') as dades:
   linies = dades.readlines()
   numlines = len(linies)
   print('Nombre de línies del fitxer: ' + str(numlines))

   lcount = 0
   for linia in linies:
       (status, category, words) = parse_line(linia)
       if status == 1:
           lcount +=1
           i = 0
           for current_word in words:
               removed_word = words[:]
               removed_word.pop(i)
               i +=1
               m = '|'.join(removed_word)
               cleaned_word, tag = clean_word(current_word)
               if cleaned_word in wordlist:
                  wordlist[cleaned_word] +=1
               else:
                  wordlist.update({cleaned_word: 1})
                  wordmeanings.update({cleaned_word: ''})

               if tag == '': wordmeanings.update({cleaned_word: wordmeanings[cleaned_word] + category + '-|' + m + '\n'})
               else:  wordmeanings.update({cleaned_word: wordmeanings[cleaned_word] + category + ' ' + tag + '-|' + m + '\n'})

   print('Nombre de línies útils: ' + str(lcount))
   print('Nombre de línies sense sinònims: ' + str(numlines-lcount))
   print('Nombre total d\'entrades: ' + str(len(wordlist)))

   generate_output()
   
