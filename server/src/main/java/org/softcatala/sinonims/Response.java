package org.softcatala.sinonims;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Response {
  String searchedWord;
  String canonical;
  List<String> alternatives;
  List<Result> results;

  Response(String w) {
    searchedWord = w;
    alternatives = new ArrayList<>();
    results = new ArrayList<>();
  }

  public class Result {
    final String lemma;
    final String feminineLemma;
    final String grammarCategory;
    List<Entry> synonymEntries;
    List<Entry> antonymEntries;

    Result(String l, String feminineForm, String gramCat) {
      lemma = l;
      feminineLemma = feminineForm;
      grammarCategory = gramCat;
      synonymEntries = new ArrayList<>();
      antonymEntries = new ArrayList<>();
    }

    public void sort() {
      synonymEntries.sort(new EntryComparator());
    }
  }
  
  public void createCanonicalFrom(String defaultCanonical) {
    for (Result result: results) {
      if (result.lemma.equalsIgnoreCase(defaultCanonical)) {
        canonical = defaultCanonical.toLowerCase();
        return;
      }
    }
    for (Result result: results) {
      if (result.lemma.equalsIgnoreCase(searchedWord)) {
        canonical = result.lemma.toLowerCase();
        return;
      }
    }
    if (defaultCanonical.isEmpty() && results.size()>0) {
      canonical = results.get(0).lemma;
      return;
    }
    canonical = defaultCanonical.toLowerCase();
  }

  public void sort() {
    results.sort(new ResultComparator());
  }

  private class EntryComparator implements Comparator<Entry> {
    @Override
    public int compare(Entry o1, Entry o2) {
      return o2.synonimWords.size() - o1.synonimWords.size();
    }
  }
  
  private class ResultComparator implements Comparator<Result> {
    @Override
    public int compare(Result o1, Result o2) {
      if (o1.lemma.equalsIgnoreCase(canonical) && !o2.lemma.equalsIgnoreCase(canonical)) {
        return -150;
      }
      if (!o1.lemma.equalsIgnoreCase(canonical) && o2.lemma.equalsIgnoreCase(canonical)) {
        return 150;
      }
      if (o1.lemma.length() > o2.lemma.length()) {
        return 100;
      }
      if (o1.lemma.length() < o2.lemma.length()) {
        return -100;
      }
      final List<String> grammarCategories = Arrays
          .asList(new String[] { "n", "adj/n", "adj", "v", "adv", "ij", "det", "indef", "prep", "pron", "conj", "loc" });
      
      int gc1 = grammarCategories.indexOf(o1.grammarCategory);
      int gc2 = grammarCategories.indexOf(o2.grammarCategory);
            
      return gc1 - gc2;
    }
  }
}
