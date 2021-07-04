package org.softcatala.sinonims;

class Word {
  final String wordString;
  final String wordComment;
  final String feminineForm;
  boolean link;
  final private String wordOriginalComment;

  public Word(String[] w, String femForm) {
    wordString = w[0];
    wordOriginalComment = w[1];
    wordComment = wordOriginalComment.replaceAll("antònim", "").replaceAll("NOFEM", "").replaceFirst("FEM .+$","").trim();
    feminineForm = femForm;
  }

  void updateLink(boolean l) {
    link = l;
  }

  public String toString() {
    return wordString + "[" + feminineForm + "] (" + wordOriginalComment + ")";
  }

  public String getOriginalComment() {
    return wordOriginalComment;
  }

}
