package org.softcatala.sinonims;

import java.util.ArrayList;
import java.util.List;

class Entry {
  List<Word> synonimWords;
  List<Word> antonymWords;
  String comment;
  String grammarCategory;

  public Entry(String[] g, List<Word> w) {
    grammarCategory = g[0];
    comment = g[1];
    synonimWords = w;
    antonymWords = new ArrayList<>();
  }

  public Entry(String gc, String c, List<Word> w) {
    grammarCategory = gc;
    comment = c;
    synonimWords = w;
    antonymWords = new ArrayList<>();
  }

  public Entry(String gc, String c, List<Word> w, List<Word> w2) {
    grammarCategory = gc;
    comment = c;
    synonimWords = w;
    antonymWords = w2;
  }

  public Entry(String line) {
    String[] parts = line.split("\\|");
    grammarCategory = parts[0];
    comment = parts[1];
    synonimWords = new ArrayList<>();
    if (parts[2].length() > 2) {
      String[] synonyms = parts[2].substring(1, parts[2].length() - 1).split(", ");
      for (String synonym : synonyms) {
        Word w = new Word(synonym);
        synonimWords.add(w);
      }
    }
    antonymWords = new ArrayList<>();
    if (parts[3].length() > 2) {
      String[] antonyms = parts[3].substring(1, parts[3].length() - 1).split(", ");
      for (String antonym : antonyms) {
        Word w = new Word(antonym);
        antonymWords.add(w);
      }
    }
  }

  public String toString() {
    return grammarCategory + "|" + comment + "|" + synonimWords.toString() + "|" + antonymWords.toString();
  }
}