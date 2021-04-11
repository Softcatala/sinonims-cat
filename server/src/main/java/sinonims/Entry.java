package sinonims;

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
  }

  public Entry(String gc, String c, List<Word> w) {
    grammarCategory = gc;
    comment = c;
    synonimWords = w;
  }

  public Entry(String gc, String c, List<Word> w, List<Word> w2) {
    grammarCategory = gc;
    comment = c;
    synonimWords = w;
    antonymWords = w2;
  }
}