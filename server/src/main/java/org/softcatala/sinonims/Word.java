package org.softcatala.sinonims;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Word {
  final String wordString;
  final String wordComment;
  final String feminineForm;
  boolean link;
  final private String wordOriginalComment;

  private static final Pattern wordPattern = Pattern.compile("(.*)\\[(.*)\\]\\((.*)\\)", Pattern.UNICODE_CASE);

  public Word(String[] w, String femForm) {
    wordString = w[0];
    wordOriginalComment = w[1];
    wordComment = wordOriginalComment.replaceAll("antònim", "").replaceAll("NOFEM", "").replaceFirst("FEM .+$", "")
        .trim();
    feminineForm = femForm;
  }

  public Word(String s) {
    Matcher m = wordPattern.matcher(s);
    String group1 = "";
    String group2 = "";
    String group3 = "";
    if (m.matches()) {
      group1 = m.group(1);
      group2 = m.group(2);
      group3 = m.group(3);
    }
    wordString = unEscapeCommas(group1);
    feminineForm = unEscapeCommas(group2);
    wordOriginalComment = unEscapeCommas(group3);
    wordComment = wordOriginalComment.replaceAll("antònim", "").replaceAll("NOFEM", "").replaceFirst("FEM .+$", "")
        .trim();

  }

  void updateLink(boolean l) {
    link = l;
  }

  public String toString() {
    return escapeCommas(wordString) + "[" + escapeCommas(feminineForm) + "](" + escapeCommas(wordOriginalComment) + ")";
  }

  public String getOriginalComment() {
    return wordOriginalComment;
  }

  private String escapeCommas(String s) {
    return s.replaceAll(",", ";;");
  }

  private String unEscapeCommas(String s) {
    return s.replaceAll(";;", ",");
  }

}
