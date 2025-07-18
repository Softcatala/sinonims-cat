package org.softcatala.sinonims;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.language.Catalan;
import org.languagetool.language.ValencianCatalan;
import org.languagetool.rules.CategoryId;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.ca.MorfologikCatalanSpellerRule;
import org.languagetool.rules.spelling.morfologik.MorfologikMultiSpeller;
import org.languagetool.synthesis.ca.CatalanSynthesizer;
import org.languagetool.tagging.ca.CatalanTagger;
import org.languagetool.tools.StringTools;

import org.softcatala.sinonims.Response.Result;

public class Dictionary {

  // índex en minúscules
  private static TreeMap<String, List<Integer>> mainDict;
  // índex de paraules parcials (parts d'expressions)
  private static TreeMap<String, Set<String>> secondDictIndex = new TreeMap<String, Set<String>>();
  // llista d'entrades, des de sinonims.txt
  private static List<Entry> entries = new ArrayList<>();

  // llista que conserva majúscules i minúscules
  private static Set<String> mainIndexSet = new HashSet<>();
  private static List<String> mainIndex = new ArrayList<>();

  private static final Collator caCollator = Collator.getInstance(new Locale("ca", "ES"));

  static final Pattern wordAndComment = Pattern.compile("\\s*(.+?)\\s*\\((.+)\\)\\s*");
  static final Pattern Comments = Pattern.compile("(.+?)#.+");

  private static JLanguageTool ltCat;
  private static JLanguageTool ltCatVal;
  private static CatalanTagger tagger;
  private static CatalanSynthesizer synth;
  private static MorfologikMultiSpeller speller;

  private final List<String> grammarCategories = Arrays
      .asList(new String[] { "n", "adj/n", "adj", "v", "adv", "ij", "det", "indef", "prep", "pron", "conj", "loc", });

  private final List<String> stopWords = Arrays.asList(new String[] { "es", "se", "s", "s'", "com", "fer", "de", "a",
      "el", "la", "en", "els", "als", "les", "per", "d", "d'", "del", "l", "l'", "pel", "-", "re", "o", "i", "no", "us",
      "ser", "estar", "jo", "tu", "ell", "ella", "son", ".", "un", "'hi", "-hi", "'ho", "'m", "'n", "'s", ",", "-ho",
      "-la", "-les", "-lo", "-me", "-n", "-ne", "-s", "-se", "-t", "?", "-te", "'l", "'t", "-li", ":" });
  // "nosaltres", "vosaltres", "ells", "elles"

  private final List<String> moveToEndTags = Arrays
      .asList(new String[] { "col·loquial", "infantil", "antic", "popular", "pejoratiu", "obsolet", "familiar" });
  private final static Pattern NOUNADJ = Pattern.compile("NCMS.*|A..MS.|V.P..SM.");
  private final static Pattern NOUN = Pattern.compile("NCMS.*");
  private final static Pattern ADJ = Pattern.compile("A..MS.|V.P..SM.");
  private final static Pattern FEMININE_FORM = Pattern.compile("\\bFEM (.+)$");

  private static ThesaurusConfig conf;

  private static MorfologikCatalanSpellerRule morfologikRule;
  final private int MAX_SUGGESTIONS = 10;
  final private int MAX_AUTOCOMPLETE = 10;
  private String firstLemmaFound = "";

  // TODO: usar les regles replace....txt de LT
  private static Map<String, String> commonErrors = new HashMap<>();
  static {
    commonErrors.put("anel", "anhel");
    commonErrors.put("desitx", "desig");
    commonErrors.put("desitg", "desig");
    commonErrors.put("insertar", "inserir");
    commonErrors.put("properament", "pròximament");
    commonErrors.put("paulatina", "gradual");
    commonErrors.put("gama", "gamma");
    commonErrors.put("extrany", "estrany");
    commonErrors.put("extranya", "estrany");
    commonErrors.put("interesant", "interessant");
    commonErrors.put("reflexar", "reflectir");
    commonErrors.put("aclarar", "aclarir");
    commonErrors.put("otorgar", "atorgar");
    commonErrors.put("grabar", "gravar");
    commonErrors.put("cumplir", "complir");
    commonErrors.put("compendre", "comprendre");
    commonErrors.put("empendre", "emprendre");
    commonErrors.put("pendre", "prendre");
    commonErrors.put("apendre", "aprendre");
    commonErrors.put("conseguir", "aconseguir");
    commonErrors.put("event", "esdeveniment");
    commonErrors.put("pasar", "passar");
    commonErrors.put("transfons", "rerefons");
    commonErrors.put("complexe", "complex");
    commonErrors.put("extendre", "estendre");
    commonErrors.put("concluir", "concloure");
    commonErrors.put("actitut", "actitud");
    commonErrors.put("evaluar", "avaluar");
    commonErrors.put("evaluació", "avaluació");
    commonErrors.put("aumentar", "augmentar");
    commonErrors.put("virtud", "virtut");
    commonErrors.put("rencor", "rancor");
    commonErrors.put("tranquila", "tranquil");
    commonErrors.put("comprobar", "comprovar");
    commonErrors.put("trascendir", "transcendir");
    commonErrors.put("atravessar", "travessar");
    commonErrors.put("debatir", "debatre");
    commonErrors.put("inquietut", "inquietud");
    commonErrors.put("demapassat", "demà passat");
    commonErrors.put("passatdema", "demà passat");
    commonErrors.put("passatdemà", "demà passat");
    commonErrors.put("adecuat", "adequat");
    commonErrors.put("abarcar", "abraçar");
    commonErrors.put("avarcar", "abraçar");
    commonErrors.put("recomenar", "recomanar");
    commonErrors.put("exisitir", "existir");
    commonErrors.put("ademés", "a més");
    commonErrors.put("ademes", "a més");
    commonErrors.put("desde", "des de");
    commonErrors.put("averiguar", "esbrinar");
    commonErrors.put("solventar", "resoldre");
    commonErrors.put("juntar", "ajuntar");
    commonErrors.put("descubrir", "descobrir");
    commonErrors.put("realizar", "realitzar");
    commonErrors.put("llimb", "llimbs");
    commonErrors.put("algo", "quelcom");
    commonErrors.put("ninfa", "nimfa");
    commonErrors.put("baliga balaga", "baliga-balaga");
    commonErrors.put("companyerisme", "companyonia");
    commonErrors.put("coneixament", "coneixement");
    commonErrors.put("concienciar", "conscienciar");
    commonErrors.put("conciencia", "consciència");
    commonErrors.put("conciència", "consciència");
    commonErrors.put("tristement", "tristament");
    commonErrors.put("bofetada", "bufetada");
    commonErrors.put("garantitzar", "garantir");
    commonErrors.put("multitut", "multitud");
    commonErrors.put("deleitar", "delectar");
    commonErrors.put("frustant", "frustrant");
    commonErrors.put("afeminat", "efeminat");
    commonErrors.put("devallada", "davallada");
    commonErrors.put("devallar", "davallar");
    commonErrors.put("tot i aixi", "tot i així");
    commonErrors.put("tot i aixo", "tot i això");
    commonErrors.put("tantmateix", "tanmateix");
    commonErrors.put("durader", "durador");
    commonErrors.put("eradicar", "erradicar");
    commonErrors.put("eradicació", "erradicació");
    commonErrors.put("llurs", "llur");
    commonErrors.put("sequetat", "sequedat");
    commonErrors.put("desarrollar", "desenvolupar");
    commonErrors.put("estornut", "esternut");
    commonErrors.put("montar", "muntar");
    commonErrors.put("incluir", "incloure");
    commonErrors.put("àmbar", "ambre");
    commonErrors.put("ambar", "ambre");
    commonErrors.put("enfermetat", "malaltia");
    commonErrors.put("utilizar", "utilitzar");
    commonErrors.put("fundamental", "fonamental");
    commonErrors.put("enfatitzar", "emfatitzar");
    commonErrors.put("armonia", "harmonia");
    commonErrors.put("recurrir", "recórrer");
    commonErrors.put("soportar", "suportar");
    commonErrors.put("probar", "provar");
    commonErrors.put("aprobar", "aprovar");
    commonErrors.put("recolzament", "suport");
    commonErrors.put("búsqueda", "recerca");
    commonErrors.put("busqueda", "recerca");
    commonErrors.put("qui sembra vents recull tempestats", "qui sembra vents cull tempestats");
    commonErrors.put("trovada", "trobada");
    commonErrors.put("col·le", "col·legi");
    commonErrors.put("enganxina", "adhesiu");
    commonErrors.put("tratge", "trage");
    commonErrors.put("apretada de mans", "encaixada de mans");
  }

  private List<String> noSuggestions = Arrays.asList("pato");

  // ignore when testing
  private List<String> wordsToIgnore = Arrays.asList("fer un paperàs", "querellador", "barça", "argigalera", "moisès",
      "aclevillar", "aguar", "almussafes", "al·lotea", "al·lotim", "amucionar", "anar de xurrut",
      "antiestatalisme", "astrosia", "balaclava", "beneitot", "binar", "botellón", "cafenet", "can Garlanda",
      "can Pixa", "can Seixanta", "can Taps", "can Xauxa", "can", "ceballí", "cherry", "com s'entén?", "daoisme",
      "de coixinereta", "de panfonteta", "disfèmic", "diàdrom", "Déu n'hi doret", "déu-n'hi-do", "déu-n'hi-doret",
      "egomaníac", "embruta-sopars", "encabat", "encaterinador", "enllambordar", "ensentinar", "escambuixar",
      "escambuixat", "escarotament", "escorxa-rosses", "espertinar", "espetència", "estroncallat", "eudemonia",
      "evangelístic", "fabliau", "feinejador", "fillar", "frapar", "glai", "globalisme", "gorrinet", "gorrinyeu",
      "guixeta", "incarcerar", "inessencial", "infermetat", "incontaminació", "judia", "lausengeria", "malconformat",
      "marcianada", "menysestimació", "merengot", "mesellia", "palmineta", "papa-sastres", "pepito", "perico",
      "perimetrar", "piocar", "plis-plai", "pompis", "pseudoartístic", "punyalet", "què dius, ara?", "Reixos",
      "retsina", "ricotta", "sa porcelleta", "sarabastall", "sardanapàlic", "Satan", "satanització", "sisplau",
      "snack-bar", "sussú", "terrosset de gel", "teteria", "ting-ting", "universalime", "ventís", "vinyeda", "volanda",
      "xambiteria", "xst", "espàrec", "qui sap quant", "no... sinó", "no... més que", "no... excepte", "sia... sia...",
      "com vulgues", "o siga", "tot lo món", "donar-se vergonya", "donar la baca", "fer la baca", "fer l'esqueta",
      "semblar una bóta de set cargues", "fer fòllega", "de vint-i-un punt", "a gom", "a tiri i baldiri", "fluixera",
      "flaquera", "camí morraler", "sumarietat", "panxeta", "pito", "contradiscurs", "canal epitrocleoolecranià",
      "fer el manta", "tocar-se la pamparruana", "barrabum", "terraplanista", "odiador", "llarg en el donar",
      "filàntrop", "impurificable", "mig sec", "de cop sobte", "a la pul pul", "amb corruixes",
      "amb presses i corruixes", "sinograma", "mà-i-mà", "dos dret", "mudabilitat", "anar de corruixes", "encartellar",
      "calent de cap", "pluricèntric", "dia per altre i dos arreu", "jo et flic", "i un be negre", "panglossià",
      "plagiador", "irruent", "pixapolit", "panxaplè", "cametes em valguen", "de ver", "smog", "blackjack", "cretlla",
      "UFO", "reena", "rehena", "coldre", "enfilerar", "irruir", "fer ufana", "repertoriar", "a tot allargar",
      "dir adeu", "envant", "bacon", "solterot", "fadrinardo", "assossegador", "vidriat", "gr", "botelló", "a remà",
      "fox-terrier", "aconseguible", "inatent", "netejable", "descorticar", "despilotar-se", "intransparent",
      "enrufolar-se", "ovovegetarianisme", "piscivegetarianisme", "avipiscivegetarianisme", "lactovegetarianisme",
      "avivegetarianisme", "apivegetarianisme", "crudivegetarianisme", "crudiveganisme", "avipiscivegetarià",
      "avivegetarià", "apivegetarià", "catxet", "enrevessar", "caravermell", "feinassa", "nord-estejar",
      "nord-oestejar", "bumerol", "spa", "xurriacar", "malcriador", "aviciador", "buscabaralles", "taikonauta",
      "breguejador", "englobador", "estudiador", "pelacolzes", "colzepelat", "socarracelles", "cellacremat",
      "memorietes", "xuclapàgines", "rosegaapunts", "xuclaapunts", "covallibres", "bonatxàs", "coexpedicionari",
      "fotoreporter", "identitarisme", "per... que sigui", "de... estant", "de... ençà", "Cèrber", "benparit",
      "implementable", "llicenciositat", "afilamines", "pronosticabilitat", "semiinconsciència", "pablanquer",
      "obesofòbia", "traspassable", "arcade", "ID", "terrenalitat", "gossam", "sabatam", "castigable", "grimori",
      "compendiositat", "interlocutar", "reincloure", "conversió analògica-digital", "mantis religiosa", "festarra",
      "macrobotellada", "pa amb...", "més-donant", "canyardo", "gardela", "demofòbic", "poltergeist",
      "cançó de la lileta", "night-club", "sallir", "sàller", "emponnar-se", "dipsomaníac", "atzero", "pull",
      "tiragomes", "mecagondeu", "cagondena", "mecàgon", "càson", "càgon", "mecàgon", "mecagondena", "cagondeu",
      "mecagoncony", "cagoncony", "mecàgon l'hòstia", "càgon l'hòstia", "mecàgon l'ou", "càgon l'ou",
      "mecàgon la mar salada", "càgon la mar salada", "mecàson", "tant és... com...", "tant se val... com...",
      "unes vegades... altres...", "tan aviat... com...", "ara... ara...", "ara... adés...", "adés... adés...", "vaser",
      "enunciable", "verbalitzable", "mainstream", "fotris", "xirlis-mirlis", "jonqui", "pu", "puà", "brainstorming",
      "poruguesa", "porugueria", "sotsxantre", "tuiter", "locus amoenus", "autodidaxi", "pallussada", "ullalada",
      "semala", "bascoparlant", "enxonar-se", "enconyar-se", "encigalar-se", "ad infinitum", "aixafador", "ciar",
      "bicefalisme", "tanoqueria", "sovietologia", "ultrasecret", "parvenu", "belleu", "mostatxada", "catxeta",
      "no dir ni mu", "feèric", "portadista", "decacordi", "tocatimbals", "a becameta", "ultraestatista", "chatbot",
      "contaire", "contador", "afterhours", "sistema de Ponzi", "piràmide de Ponzi", "desromantitzar", "nassarrut",
      "inidentificat", "inofensivitat", "panem et circenses", "aftershave", "vandalitzar",
      "de l'any de la Mariacastanya", "de l'any tirurany", "de l'any de la catapumba", "escondiment", "reprise",
      "musicalitzar", "bé... bé...", "iubarta", "a Déu sien dades", "tot xanco i manco", "d'ordre n", "pàfia",
      "ubertat", "can pixa i rellisca", "can pixa-i-rellisca", "can penja-i-despenja", "barrejable", "endormiscador",
      "per fas o per nefas", "cer", "antiedat", "bon vivant", "edam", "pirineus", "desvirginar", "desverjar",
      "torrapebrots", "pintxo", "indefallible", "ransomware", "mai dels mais", "cric-crec", "dessusdit",
      "malaltia de Cotugno", "lapsus calami", "lapsus linguae", "copiable", "duplicable", "rallentado", "ritenuto",
      "sia... o...", "divertimento", "calorassa", "calorota", "escaiar-se", "rallentando", "xòped", "riff",
      "ecogastronomia", "slow food", "no tindre un qüe", "cloroformitzador", "a bacs i redolons", "de bòbilis-bòbilis",
      "menjaclosques", "imbarrejable", "opinaire", "storytelling", "torcaboquer", "portatovallons", "tovallonera",
      "de bocons", "entrepussar", "lliberticidi", "camperitzar", "camperització", "rojoncós", "emmetxament", "ginoide",
      "encadellament", "escrache", "ITS", "girajaquetes", "polímata", "ual·la", "melindrositat", "sílex piròmac",
      "tripatinet", "tant és Alí com Camalí", "deixar KO", "irreproduïble", "KO", "muà", "muac", "xuic", "txuic",
      "escanyapets", "tapavergonyes", "avergonyidor", "pudícia", "burocratès", "Suillus", "portaplatets", "xaila",
      "amira", "khimar", "recançós", "com-s'ha-fet", "catso", "catsoleta", "ensarronador", "apocalipticisme", "ullerut",
      "verborreic", "antigitanisme", "romofòbia", "racisme antigitano", "diabolus in musica", "esbrufar", "llegiguera",
      "manicurista", "mu", "mitjamerda", "menjamerda", "cigalot", "cracker", "dissintonia", "Nadalet", "collins",
      "collinsa", "collinses", "fole", "fótrins", "hosti", "collona", "antologar", "farsi", "barralbuit", "expat",
      "tantsemenfotisme", "peplòmer", "gotícula", "lotocràcia", "vaccinòdrom", "multisegment", "vacunòdrom",
      "crossover", "business-friendly", "somiacoques", "vehicle multisegment", "arrodir", "cliffhanger", "arrodir",
      "xatbot", "arrodir-se", "barrufar", "daixonar", "dallonar", "daçonar", "camacu", "titafluixa", "titafreda",
      "surotaper", "pessigavidres", "càgon tot", "de can Fanga", "can Fanga", "forasterada", "expo", "homo universalis",
      "glossònim", "like", "agradament", "mesoclític", "endoclític", "càiron", "xuleta", "inculturar", "camacu",
      "quemaco", "guaitacaragols", "llepafinestres", "ganassot", "ensumapets", "ser la repera", "belvedere",
      "catalanor", "sucatinters", "valencianor", "sobrereaccionar", "objectificació", "méteo", "objectificar",
      "ressuat", "de can passavia", "bentornat", "flamisell", "opinòleg", "TPV", "satanisme", "pantanada", "clafert",
      "de trucalembut", "irrecompensable", "fritfrit", "gaitó", "ram pataplam", "regantellar", "escallimpar", "dana",
      "caiuc", "robinson", "americanet", "Xangri-La", "jogueroi", "bangladeshià", "tucuixi", "temacle", "temarro",
      "aquaplàning", "óblast", "franquiciar", "germanoparlant", "túrquic", "encausament", "atzabó", "en pac de", "qüens",
      "tito", "ionqui", "insocial", "community manager", "road manager", "casota", "esgarradet");

  Dictionary(ThesaurusConfig configuration) throws IOException {

    conf = configuration;
    ThesaurusServer.log("Start preparing dictionary.");

    caCollator.setStrength(Collator.IDENTICAL);
    mainDict = new TreeMap<String, List<Integer>>(caCollator);

    ltCatVal = new JLanguageTool(ValencianCatalan.getInstance());
    ltCat = new JLanguageTool(Catalan.getInstance());

    ltCat.disableRule("UPPERCASE_SENTENCE_START");
    ltCat.disableRule("MORFOLOGIK_RULE_CA_ES");
    ltCat.disableRule("CA_SIMPLEREPLACE_DIACRITICS_IEC");
    ltCat.disableRule("ALTRE_UN_ALTRE");
    ltCat.disableRule("EXIGEIX_VERBS_CENTRAL");
    ltCat.disableRule("DISFRUTAR");
    ltCat.disableRule("EXIGEIX_POSSESSIUS_V");
    ltCat.disableCategory(new CategoryId("PUNCTUATION"));
    ltCatVal.disableRule("UPPERCASE_SENTENCE_START");
    tagger = (CatalanTagger) ltCatVal.getLanguage().getTagger();
    synth = (CatalanSynthesizer) ltCatVal.getLanguage().getSynthesizer();
    speller = new MorfologikMultiSpeller("/ca/ca-ES-valencia.dict", Collections.singletonList("/ca/spelling.txt"), null, 1);

    ResourceBundle messages = JLanguageTool.getDataBroker().getResourceBundle(JLanguageTool.MESSAGE_BUNDLE,
        new Locale("ca"));
    morfologikRule = new MorfologikCatalanSpellerRule(messages, ValencianCatalan.getInstance(), null, null);

    ThesaurusServer.log("Reading source and building dictionary.");
    if (conf.production.equalsIgnoreCase("yes")) {
      ThesaurusServer.logger.info("Skipping LanguageTool checks.");
      readFromFile();
      // saveToFile();
    } else {
      ThesaurusServer.log("Checking dictionary with LanguageTool.");
      List<String> lines;
      try {
        lines = Files.readAllLines(Paths.get(conf.srcFile.toURI()));
      } catch (IOException e) {
        ThesaurusServer.log(e.toString());
        throw new IOException("Error reading source dictionary. ");
      }
      for (String line : lines) {
        ThesaurusServer.log(addLine(line));
      }
      // índex de paraules
      List<String> wordList = new ArrayList<>(mainIndexSet);
      wordList.sort(new IndexSortComparator());
      mainIndex = wordList;
      saveToFile();
    }

  }

  public class ElementSortComparator implements Comparator<Word> {
    @Override
    public int compare(Word o1, Word o2) {
      // moure paraules amb marca al final
      if (moveToEndTags.contains(o1.wordComment) && !moveToEndTags.contains(o2.wordComment)) {
        return 100;
      }
      if (moveToEndTags.contains(o2.wordComment) && !moveToEndTags.contains(o1.wordComment)) {
        return -100;
      }
      return caCollator.compare(o1.wordString, o2.wordString);
    }
  }

  public class IndexSortComparator implements Comparator<String> {
    @Override
    public int compare(String arg0, String arg1) {
      Collator caCollator = Collator.getInstance(new Locale("ca", "ES"));
      caCollator.setStrength(Collator.IDENTICAL);
      String a0 = arg0.toLowerCase().replaceAll("·", "");
      String a1 = arg1.toLowerCase().replaceAll("·", "");
      int result = caCollator.compare(a0, a1);
      return result;
    }
  }

  public class FrequencySortComparator implements Comparator<String> {
    @Override
    public int compare(String arg0, String arg1) {
      int f0 = speller.getFrequency(arg0);
      int f1 = speller.getFrequency(arg1);
      return f1 - f0;
    }
  }

  public String getEntryString(Integer i, String exception) {
    StringBuilder result = new StringBuilder();
    StringBuilder antonyms = new StringBuilder();
    boolean first = true;
    boolean firstAntonym = true;
    boolean isAntonym = false;
    int entrySize = entries.get(i).synonimWords.size();
    for (Integer j = 0; j < entrySize; j++) {
      Word w = entries.get(i).synonimWords.get(j);
      if (w.wordString.equals(exception) && w.wordComment.isEmpty()) {
        continue;
      }

      if (w.wordComment.contains("antònim") && w.wordString.equals(exception)) {
        isAntonym = true;
      }

      if (w.wordComment.contains("antònim")) {
        if (!firstAntonym) {
          antonyms.append(", ");
        } else {
          firstAntonym = false;
        }
        antonyms.append(w.wordString);
        String comment = w.wordComment.replace("antònim", "");
        if (!comment.isEmpty()) {
          antonyms.append(" [" + w.wordComment + "]");
        }
      } else if (w.wordString.equals(exception) && !w.wordComment.isEmpty()) {
        // no eliminar el mot de l'entrada principal si té marca, i posar-la al principi
        StringBuilder wordAtStart = new StringBuilder();
        wordAtStart.append(w.wordString + " [" + w.wordComment + "]");
        if (!first) {
          wordAtStart.append(", ");
        } else {
          first = false;
        }
        result.insert(0, wordAtStart);
      } else {
        if (!first) {
          result.append(", ");
        } else {
          first = false;
        }
        result.append(w.wordString);
        if (!w.wordComment.isEmpty()) {
          result.append(" [" + w.wordComment + "]");
        }
      }
    }

    StringBuilder header = new StringBuilder();
    header.append("(" + entries.get(i).grammarCategory + ") ");
    if (!entries.get(i).comment.isEmpty()) {
      header.append("[" + entries.get(i).comment + "] ");
    }
    result.insert(0, header);
    if (!antonyms.toString().isEmpty()) {
      result.append(" | ANTÒNIMS: " + antonyms);
    }
    if (isAntonym) {
      result.insert(0, "-");
    }
    return result.toString();
  }

  public String printAllDict() {
    StringBuilder result = new StringBuilder();
    for (Map.Entry<String, List<Integer>> me : mainDict.entrySet()) {
      result.append("\n\n==" + me.getKey() + "==");
      int accepcio = 1;
      for (Integer i : me.getValue()) {
        result.append("\n" + accepcio + ". " + getEntryString(i, me.getKey()));
        accepcio++;
      }
    }
    return result.toString();
  }

  public String printWord(String searchedWord) throws IOException {
    StringBuilder result = new StringBuilder();
    List<String> words = searchWord(searchedWord);
    if (words.isEmpty()) {
      return "\n\n** NOT FOUND **: " + searchedWord;
    }
    for (String word : words) {
      if (mainDict.containsKey(word)) {
        List<String> antonymsEntries = new ArrayList<>();
        result.append("\n\n==" + word + "==");
        int accepcio = 1;
        for (Integer i : mainDict.get(word)) {
          String entry = getEntryString(i, word);
          if (!entry.startsWith("-")) {
            result.append("\n" + accepcio + ". " + entry);
            accepcio++;
          } else {
            antonymsEntries.add(entry.substring(1).replaceFirst(" \\| ANTÒNIMS.*", ""));
          }
        }
        if (!antonymsEntries.isEmpty()) {
          accepcio = 1;
          result.append("\n--antònims--");
          for (String entry : antonymsEntries) {
            result.append("\n" + accepcio + ". " + entry);
            accepcio++;
          }
        }

      }
    }
    return result.toString();
  }

  private String fixEncoding(String input) {
    // inconsciÃƒÂ¨ncia, pÃƒÂ­tima, providÃƒÂ¨ncia, insÃ­gnia, romanÃƒÂ§,
    // amenaÃƒÆ’Ã‚Â§ar, insÃ
    String output = input;
    if (output.endsWith("Ã")) {
      // \u00a0 is trimmed at the end
      output = output.substring(0, output.length() - 1) + "à";
    }
    if (output.contains("`")) {
      output = output.replaceAll("`a", "à").replaceAll("`e", "è").replaceAll("`o", "ò");
    }
    for (int i = 0; i < 3; i++) {
      if (output.contains("\u00C3") || output.contains("Â")) {
        output = output.replaceAll("â‚¬", "€").replaceAll("â€š", "‚").replaceAll("Æ’", "ƒ").replaceAll("â€ž", "„")
            .replaceAll("â€¦", "…").replaceAll("â€", "†").replaceAll("â€¡", "‡").replaceAll("Ë†", "ˆ")
            .replaceAll("â€°", "‰").replaceAll("Å\u00a0", "Š").replaceAll("â€¹", "‹").replaceAll("Å’", "Œ")
            .replaceAll("Å½", "Ž").replaceAll("â€˜", "‘").replaceAll("â€™", "’").replaceAll("â€œ", "“")
            .replaceAll("â€", "”").replaceAll("â€¢", "•").replaceAll("â€“", "–").replaceAll("â€”", "—")
            .replaceAll("Ëœ", "˜").replaceAll("â„¢", "™").replaceAll("Å¡", "š").replaceAll("â€º", "›")
            .replaceAll("Å“", "œ").replaceAll("Å¾", "ž").replaceAll("Å¸", "Ÿ").replace("Â\u00ad", "\u00ad")
            .replaceAll("Â¡", "¡").replaceAll("Â¢", "¢").replaceAll("Â£", "£").replaceAll("Â¤", "¤")
            .replaceAll("Â¥", "¥").replaceAll("Â¦", "¦").replaceAll("Â§", "§").replaceAll("Â¨", "¨")
            .replaceAll("Â©", "©").replaceAll("Âª", "ª").replaceAll("Â«", "«").replaceAll("Â¬", "¬")
            .replaceAll("Â®", "®").replaceAll("Â¯", "¯").replaceAll("Â°", "°").replaceAll("Â±", "±")
            .replaceAll("Â²", "²").replaceAll("Â³", "³").replaceAll("Â´", "´").replaceAll("Âµ", "µ")
            .replaceAll("Â¶", "¶").replaceAll("Â·", "·").replaceAll("Â¸", "¸").replaceAll("Â¹", "¹")
            .replaceAll("Âº", "º").replaceAll("Â»", "»").replaceAll("Â¼", "¼").replaceAll("Â½", "½")
            .replaceAll("Â¾", "¾").replaceAll("Â¿", "¿").replaceAll("Ã€", "À").replaceAll("Ã‚", "Â")
            .replaceAll("Ãƒ", "Ã").replaceAll("Ã„", "Ä").replaceAll("Ã…", "Å").replaceAll("Ã†", "Æ")
            .replaceAll("Ã‡", "Ç").replaceAll("Ãˆ", "È").replaceAll("Ã‰", "É").replaceAll("ÃŠ", "Ê")
            .replaceAll("Ã‹", "Ë").replaceAll("ÃŒ", "Ì").replaceAll("ÃŽ", "Î").replaceAll("Ã‘", "Ñ")
            .replaceAll("Ã’", "Ò").replaceAll("Ã“", "Ó").replaceAll("Ã”", "Ô").replaceAll("Ã•", "Õ")
            .replaceAll("Ã–", "Ö").replaceAll("Ã—", "×").replaceAll("Ã˜", "Ø").replaceAll("Ã™", "Ù")
            .replaceAll("Ãš", "Ú").replaceAll("Ã›", "Û").replaceAll("Ãœ", "Ü").replaceAll("Ãž", "Þ")
            .replaceAll("ÃŸ", "ß").replaceAll("Ã\u00a0", "à").replaceAll("Ã¡", "á").replaceAll("Ã¢", "â")
            .replaceAll("Ã£", "ã").replaceAll("Ã¤", "ä").replaceAll("Ã¥", "å").replaceAll("Ã¦", "æ")
            .replaceAll("Ã§", "ç").replaceAll("Ã¨", "è").replaceAll("Ã©", "é").replaceAll("Ãª", "ê")
            .replaceAll("Ã«", "ë").replaceAll("Ã¬", "ì").replaceAll("Ã\u00ad", "í").replaceAll("Ã®", "î")
            .replaceAll("Ã¯", "ï").replaceAll("Ã°", "ð").replaceAll("Ã±", "ñ").replaceAll("Ã²", "ò")
            .replaceAll("Ã³", "ó").replaceAll("Ã´", "ô").replaceAll("Ãµ", "õ").replaceAll("Ã¶", "ö")
            .replaceAll("Ã·", "÷").replaceAll("Ã¸", "ø").replaceAll("Ã¹", "ù").replaceAll("Ãº", "ú")
            .replaceAll("Ã»", "û").replaceAll("Ã¼", "ü").replaceAll("Ã½", "ý").replaceAll("Ã¾", "þ")
            .replaceAll("Ã¿", "ÿ");
        /*
         * Error chars: .replaceAll("Ã", "Ý").replaceAll("Ã", "Á").replaceAll("Ã",
         * "Í").replaceAll("Ã", "Ï").replaceAll("Ã", "Ð")
         */
      } else {
        break;
      }
    }
    return output;
  }

  public List<String> searchWord(String searchedWordOriginal) throws IOException {
    String searchedWord = fixEncoding(searchedWordOriginal);
    searchedWord = searchedWord.replaceAll("l\\.l", "l·l").replaceAll("l•l", "l·l").replaceAll("l-l", "l·l")
        .replaceAll("l • l", "l·l").replaceAll("’", "'").replaceAll(",", "").replaceAll("&apos;", "'");

    Set<String> resultsSet = new HashSet<>();
    Set<String> alternativesSet = new HashSet<>();
    List<String> resultsList = new ArrayList<>();
    if (searchedWord.isEmpty()) {
      return resultsList;
    }

    String lowercase = searchedWord.toLowerCase();
    String searchedAscii = StringTools.removeDiacritics(lowercase);

    if (commonErrors.containsKey(lowercase)) {
      resultsList.add(commonErrors.get(lowercase));
      return resultsList;
    }

    if (mainDict.containsKey(lowercase)) {
      resultsSet.add(lowercase);
    }
    if (mainDict.containsKey(lowercase.replaceAll(" ", "-"))) {
      resultsSet.add(lowercase.replaceAll(" ", "-"));
    }
    if (mainDict.containsKey(lowercase + "-se")) {
      resultsSet.add(lowercase + "-se");
    }
    if (mainDict.containsKey(lowercase + "'s")) {
      resultsSet.add(lowercase + "'s");
    }
    if (secondDictIndex.containsKey(lowercase)) {
      resultsSet.addAll(secondDictIndex.get(lowercase));
    }

    // search lemma
    firstLemmaFound = "";
    List<AnalyzedSentence> aSentences = ltCatVal.analyzeText(lowercase);
    for (AnalyzedSentence aSentence : aSentences) {
      AnalyzedTokenReadings[] tokens = aSentence.getTokensWithoutWhitespace();
      if (tokens.length > 1 && !resultsSet.isEmpty()) {
        continue;
      }
      for (AnalyzedTokenReadings atr : tokens) {
        if (atr.isTagged() && !atr.getToken().equalsIgnoreCase("-se") && !atr.getToken().equalsIgnoreCase("'s")) {
          for (AnalyzedToken at : atr) {
            String lemma = at.getLemma();
            if (firstLemmaFound.isEmpty() && lemma != null) {
              firstLemmaFound = lemma;
            }
            if (lemma != null && mainDict.containsKey(lemma) && !stopWords.contains(lemma)) {
              resultsSet.add(lemma);
            }
            if (lemma != null && secondDictIndex.containsKey(lemma) && !stopWords.contains(lemma)) {
              resultsSet.addAll(secondDictIndex.get(lemma));
            }
          }
        }
      }
    }

    // added diacritics
    // TODO: as an index
    for (String w : mainIndex) {
      if (StringTools.removeDiacritics(w).toLowerCase().equals(lowercase)) {
        resultsSet.add(w);
      }
    }
    for (String w : secondDictIndex.keySet()) {
      if (StringTools.removeDiacritics(w).toLowerCase().equals(lowercase)) {
        resultsSet.addAll(secondDictIndex.get(w));
      }
    }

    // find alternative spellings
    int myTry = 0;
    while (resultsSet.isEmpty() && myTry < 2) {
      if (myTry == 1) {
        aSentences = ltCatVal.analyzeText(makeWrong(lowercase));
      }
      for (AnalyzedSentence aSentence : aSentences) {
        RuleMatch[] matches = morfologikRule.match(aSentence);
        if (matches != null && matches.length > 0) {
          List<String> suggestions = matches[0].getSuggestedReplacements();
          int i = 0;
          for (String suggestion : suggestions) {
            // nuvia -> núvia -> nuvi
            if (StringTools.removeDiacritics(suggestion).equalsIgnoreCase(searchedAscii)) {
              List<AnalyzedTokenReadings> atrs = tagger.tag(Arrays.asList(new String[] { suggestion }));
              if (atrs != null && atrs.size() > 0) {
                for (AnalyzedToken at : atrs.get(0)) {
                  String atLemma = at.getLemma();
                  if (!atLemma.isEmpty() && !stopWords.contains(atLemma)) {
                    resultsSet.add(atLemma);
                  }
                }
              }
            }
            if (!resultsSet.contains(suggestion) && !resultsSet.contains(suggestion.toLowerCase())
                && !resultsSet.contains("-" + suggestion) && !resultsSet.contains("-" + suggestion.toLowerCase())
                && mainDict.containsKey(suggestion)) {
              // Donar com a resultat directe si només difereix en diacrítics o s/ss
              if (StringTools.removeDiacritics(suggestion).equalsIgnoreCase(searchedAscii)
                  || StringTools.removeDiacritics(suggestion.replace("l·l", "l")).equalsIgnoreCase(searchedAscii)
                  || StringTools.removeDiacritics(suggestion.replace("ss", "s")).equalsIgnoreCase(searchedAscii)
                  || StringTools.removeDiacritics(suggestion.replace("nn", "n")).equalsIgnoreCase(searchedAscii)) {
                resultsSet.add(suggestion);
              } else {
                alternativesSet.add("-" + suggestion);
              }
              i++;
              if (i >= MAX_SUGGESTIONS) {
                break;
              }
            }
          }
        }
        myTry++;
      }
    }

    for (String s : noSuggestions) {
      if (resultsSet.contains(s)) {
        resultsSet.remove(s);
      }
    }
    // suggeriments d'altres regles de LanguageTool (no ortografia)
    if (resultsSet.isEmpty() || (resultsSet.size() == 1 && resultsSet.contains(searchedWordOriginal.toLowerCase()))) {
      List<RuleMatch> matches = ltCat.check(searchedWord);
      Set<String> alternativesLTSet = new HashSet<>();
      for (RuleMatch m : matches) {
        for (String r : m.getSuggestedReplacements()) {
          alternativesLTSet.add("-" + r);
        }
      }
      if (!alternativesLTSet.isEmpty()) {
        alternativesSet.clear();
        alternativesSet.addAll(alternativesLTSet);
        alternativesSet.addAll(resultsSet);
      }
    }
    resultsSet.addAll(alternativesSet);
    resultsList.addAll(resultsSet);
    return resultsList;
  }

  // TODO: comprovar les etiquetes (que els v. són V.N.*, etc.)
  public String addLine(String originalLine) throws IOException {
    StringBuilder message = new StringBuilder();
    Matcher m = Comments.matcher(originalLine);
    String line;
    if (m.matches()) {
      line = m.group(1).trim();
    } else {
      line = originalLine.trim();
    }
    if (line.isEmpty()) {
      return "Empty line";
    }
    line = line.replaceAll("\\\\:", "_DOS_PUNTS_");
    String[] parts = line.split(":");
    if (!line.startsWith("-") || parts.length != 2) {
      return ("Error in source dictionary, line: " + line + "\n");
    }

    if (line.contains(",(") || line.contains(", (") || line.contains("  ") || line.contains("\t")) {
      return ("Error in source dictionary, line: " + line + "\n");
    }
    String grammarCat = parts[0].substring(1);

    if (!grammarCategories.contains(extractWordComment(grammarCat)[0])) {
      return ("Unknown grammar category, line: " + line + "\n");
    }

    parts[1] = parts[1].replaceAll("\\\\,", "_COMA_");
    String[] stringBetweenCommas = parts[1].split(",");

    List<Word> words = new ArrayList<Word>();
    for (String s : stringBetweenCommas) {
      s = s.replaceAll("_COMA_", ",");
      s = s.replaceAll("_DOS_PUNTS_", ":");
      String[] wordStringComment = extractWordComment(s);
      String femForm = getFeminineForm(wordStringComment[0], extractWordComment(grammarCat)[0], wordStringComment[1]);
      Word w = new Word(wordStringComment, femForm);
      if (!wordsToIgnore.contains(w.wordString) && w.wordString.contains("...")) {
        if (w.wordString.length() > 3) {
          message.append("Word with ellipsis: " + w.wordString + "; ");
        } else {
          // ignore word="..."
          continue;
        }
      }
      if (!w.wordString.contains("...") && w.wordString.contains(".")) {
        message.append("Word with dot: " + w.wordString + "; ");
      }

      if (!conf.production.contentEquals("yes")) {
        List<AnalyzedSentence> aSentences = ltCatVal.analyzeText(w.wordString);
        for (AnalyzedSentence aSentence : aSentences) {
          if (!wordsToIgnore.contains(aSentence.getText().toString())) {
            for (AnalyzedTokenReadings atr : aSentence.getTokensWithoutWhitespace()) {
              if (!atr.isWhitespace() && atr.isPosTagUnknown() && !atr.isIgnoredBySpeller()
                  && !atr.hasPartialPosTag("_PUNCT")) {
                message.append("Unknown word: " + atr.getToken() + " in line: " + w.wordString + "; ");
              }
            }
            List<RuleMatch> matches = ltCat.check(aSentence.getText());
            if (matches.size() > 1) {
              message.append("LT error found in: " + w.wordString + "; ");
            }
          }
        }

        for (Word w2 : words) {
          if (w.getOriginalComment().equals(w2.getOriginalComment()) && w.wordString.equals(w2.wordString)) {
            message.append("Duplicate word '" + w.wordString + "' in line: " + line + "\n");
          }
        }
      }
      words.add(w);
    }

    if (words.size() < 2) {
      // message.append("Only one word in line: " + line);
      return message.toString();
    }

    words.sort(new ElementSortComparator());
    Entry e = new Entry(extractWordComment(grammarCat), words);
    entries.add(e);
    Integer position = entries.size() - 1;
    for (Word w : e.synonimWords) {
      mainIndexSet.add(w.wordString);
      String wlc = w.wordString.toLowerCase();
      if (mainDict.containsKey(wlc)) {
        List<Integer> l = mainDict.get(wlc);
        l.add(position);
        mainDict.put(wlc, l);
      } else {
        List<Integer> l = new ArrayList<>();
        l.add(position);
        mainDict.put(wlc, l);
      }
      // assegura la forma femenina en l'índex
      String fem = "";
      if (w.getOriginalComment().contains("FEM") && !w.getOriginalComment().contains("NOFEM")) {
        fem = getFeminineForm(w.wordString, grammarCat, w.getOriginalComment());
        addToSecondDictIndex(fem, wlc);
      }
      // crea el secondDictIndex per a multiparaules
      List<String> allForms = getAllForms(wlc + " " + fem);
      // què dius, ara?
      if (wlc.contains("!") || wlc.contains("?") || wlc.contains(",") || wlc.contains("-") || wlc.contains(":")) {
        String cleanWlc = wlc.replaceAll("!", "").replaceAll("\\?", "").replaceAll(",", "").replaceAll("-", "")
            .replaceAll(":", "");
        addToSecondDictIndex(cleanWlc, wlc);
      }
      if (allForms.size() > 1) {
        for (String wordPart : allForms) {
          if (stopWords.contains(wordPart)) {
            continue;
          }
          addToSecondDictIndex(wordPart, wlc);
        }
      }

    }
    return message.toString();
  }

  private void addToSecondDictIndex(String indexWord, String targetWord) {
    if (indexWord.isEmpty()) {
      return;
    }
    if (secondDictIndex.containsKey(indexWord)) {
      Set<String> l = secondDictIndex.get(indexWord);
      l.add(targetWord);
      secondDictIndex.put(indexWord, l);
    } else {
      Set<String> l = new HashSet<>();
      l.add(targetWord);
      secondDictIndex.put(indexWord, l);
    }
  }

  private String[] extractWordComment(String s) {
    String[] result = new String[2];
    result[0] = "";
    result[1] = "";
    Matcher m = wordAndComment.matcher(s);
    if (m.matches()) {
      result[0] = m.group(1).trim();
      result[1] = m.group(2).trim();
    } else {
      result[0] = s.trim();
    }
    return result;
  }

  private static String getFeminineForm(String lemma, String grammarCat, String wordComment) throws IOException {

    List<String> comments = Arrays.asList(wordComment.split("[ ;:,.]"));
    if (comments.contains("f") || comments.contains("m") || comments.contains("NOFEM")) {
      return "";
    }
    Matcher mFem = FEMININE_FORM.matcher(wordComment);
    if (mFem.find()) {
      return mFem.group(1);
    }
    Pattern p;
    String feminineRegexp;
    if (grammarCat.equals("n")) {
      p = NOUN;
      feminineRegexp = "NCF[SN].*";
    } else if (grammarCat.equals("adj")) {
      p = ADJ;
      feminineRegexp = "A..F[SN].|V.P..SF.";
    } else if (grammarCat.equals("adj/n")) {
      p = NOUNADJ;
      feminineRegexp = "NCF[SN].*|A..F[SN].|V.P..SF.";
    } else {
      return "";
    }

    List<AnalyzedTokenReadings> atrs = tagger.tag(Arrays.asList(new String[] { lemma }));
    AnalyzedToken analyzedToken = null;
    if (atrs != null && atrs.size() > 0) {
      for (AnalyzedToken at : atrs.get(0)) {
        String posTag = at.getPOSTag();
        if (posTag != null) {
          final Matcher m = p.matcher(posTag);
          if (m.matches()) {
            analyzedToken = at;
          }
        }
      }
      if (analyzedToken != null) {
        if (analyzedToken.getPOSTag().contentEquals("VMP00SM0")) {
          feminineRegexp = "VMP00SF0";
        }
        if (analyzedToken.getPOSTag().contentEquals("AQ0MS0")) {
          feminineRegexp = "AQ0FS0";
        }

        String femForm[] = synth.synthesize(analyzedToken, feminineRegexp, true);
        if (femForm.length > 0) {
          if (!femForm[0].equalsIgnoreCase(lemma)) {
            return femForm[0];
          }
        }
      }
    }
    return "";
  }

  private List<String> getAllForms(String s) throws IOException {
    Set<String> forms = new HashSet<>();
    List<AnalyzedSentence> aSentences = ltCatVal.analyzeText(s);
    for (AnalyzedSentence aSentence : aSentences) {
      AnalyzedTokenReadings[] tokens = aSentence.getTokensWithoutWhitespace();
      for (AnalyzedTokenReadings atr : tokens) {
        if (atr.getToken().trim().isEmpty()) {
          continue;
        }
        forms.add(atr.getToken());
        if (stopWords.contains(atr.getToken())) {
          continue;
        }
        for (AnalyzedToken at : atr) {
          if (stopWords.contains(at.getLemma())) {
            continue;
          }
          String[] synthForms = synth.synthesize(at, "[^V].*", true);
          for (String sf : synthForms) {
            if (!stopWords.contains(sf)) {
              forms.add(sf);
            }
          }
        }
      }
    }
    return new ArrayList<>(forms);
  }

  public Index getIndex(String startWith) {
    List<String> wordList = new ArrayList<>();
    String myStartWith = StringTools.removeDiacritics(startWith).toLowerCase();
    for (String w : mainIndex) {
      if (StringTools.removeDiacritics(w).toLowerCase().startsWith(myStartWith)) {
        wordList.add(w);
      }
    }
    return new Index(wordList, startWith);
  }

  public Index getAutocomplete(String startWith) {
    List<String> wordList = new ArrayList<>();
    for (String w : mainIndex) {
      if (StringTools.removeDiacritics(w).toLowerCase().startsWith(startWith.toLowerCase())) {
        wordList.add(w);
      }
    }
    wordList.sort(new FrequencySortComparator());
    if (wordList.size() > MAX_AUTOCOMPLETE) {
      wordList = wordList.subList(0, MAX_AUTOCOMPLETE);
    }
    return new Index(wordList, startWith);
  }

  public Response getResponse(String searchedWord) throws IOException {
    Response response = new Response(searchedWord);
    List<String> lemmas = searchWord(searchedWord);

    for (String lemma : lemmas) {
      if (lemma.startsWith("-")) {
        response.alternatives.add(lemma.substring(1));
        continue;
      }
      if (mainDict.containsKey(lemma)) {
        Map<String, Result> resultsMap = new LinkedHashMap<>();
        for (String grammarCat : grammarCategories) {
          for (int i : mainDict.get(lemma)) {
            if (entries.get(i).grammarCategory.equals(grammarCat)) {
              boolean isAntonym = false;
              String lemmaComment = "";
              List<Word> antonyms = new ArrayList<>();
              List<Word> synonyms = new ArrayList<>();
              String lemmaResult = lemma;
              for (Integer j = 0; j < entries.get(i).synonimWords.size(); j++) {
                Word w = entries.get(i).synonimWords.get(j);
                w.updateLink(mainDict.get(w.wordString.toLowerCase()).size() > 1);
                if (w.wordString.equalsIgnoreCase(lemma) && w.wordComment.isEmpty()
                    && !w.getOriginalComment().contains("antònim")) {
                  lemmaComment = w.getOriginalComment();
                  lemmaResult = w.wordString;
                  continue;
                }
                if (w.getOriginalComment().contains("antònim") && w.wordString.equalsIgnoreCase(lemma)) {
                  isAntonym = true;
                }
                if (w.getOriginalComment().contains("antònim")) {
                  antonyms.add(w);
                } else if (w.wordString.equalsIgnoreCase(lemma) && !w.wordComment.isEmpty()) {
                  // si el lema té comentari s'afegeix en la primera posició
                  synonyms.add(0, w);
                  lemmaComment = w.getOriginalComment();
                  lemmaResult = w.wordString;
                } else {
                  synonyms.add(w);
                }
              }
              String key = entries.get(i).grammarCategory + lemmaComment + lemmaResult;
              if (!resultsMap.containsKey(key)) {
                resultsMap.put(key, response.new Result(lemmaResult,
                    getFeminineForm(lemmaResult, grammarCat, lemmaComment), grammarCat));
              }
              if (isAntonym) {
                Entry entry = new Entry(entries.get(i).grammarCategory, entries.get(i).comment, synonyms);
                resultsMap.get(key).antonymEntries.add(entry);
              } else {
                Entry entry = new Entry(entries.get(i).grammarCategory, entries.get(i).comment, synonyms, antonyms);
                resultsMap.get(key).synonymEntries.add(entry);
              }
            }
          }
        }
        for (Map.Entry<String, Result> r : resultsMap.entrySet()) {
          Result result = r.getValue();
          if (result.synonymEntries.size() > 0) {
            result.sort();
            response.results.add(result);
          }
        }
      }
    }
    if (response.results.size() == 0 || response.alternatives.size() > 0) {
      StringBuilder message = new StringBuilder("NOT FOUND: " + searchedWord + ";");
      if (response.alternatives.size() > 0) {
        message.append(" ALTERNATIVES: " + response.alternatives.toString());
      }
      if (response.results.size() > 0) {
        message.append(" RESULTS: ");
        for (Result r : response.results) {
          message.append(r.lemma + ", ");
        }
      }
      ThesaurusServer.log(message.toString());
    }
    response.createCanonicalFrom(firstLemmaFound);
    response.sort();
    return response;
  }

  private String makeWrong(String s) {
    if (s.contains("a")) {
      return s.replace("a", "ä");
    }
    if (s.contains("e")) {
      return s.replace("e", "ë");
    }
    if (s.contains("i")) {
      return s.replace("i", "í");
    }
    if (s.contains("o")) {
      return s.replace("o", "ö");
    }
    if (s.contains("u")) {
      return s.replace("u", "ü");
    }
    if (s.contains("é")) {
      return s.replace("é", "ë");
    }
    if (s.contains("à")) {
      return s.replace("à", "ä");
    }
    if (s.contains("è")) {
      return s.replace("è", "ë");
    }
    if (s.contains("ü")) {
      return s.replace("ü", "u");
    }
    if (s.contains("ï")) {
      return s.replace("ï", "i");
    }
    if (s.contains("ê")) {
      return s.replace("ê", "ë");
    }
    if (s.contains("î")) {
      return s.replace("î", "ï");
    }
    if (s.contains("ô")) {
      return s.replace("ô", "ö");
    }
    if (s.contains("û")) {
      return s.replace("û", "ü");
    }
    return s + "-";
  }

  public static String toTitleCase(String input) {
    StringBuilder titleCase = new StringBuilder(input.length());
    boolean nextTitleCase = true;
    for (char c : input.toCharArray()) {
      if (Character.isSpaceChar(c)) {
        nextTitleCase = true;
      } else if (nextTitleCase) {
        c = Character.toTitleCase(c);
        nextTitleCase = false;
      }
      titleCase.append(c);
    }
    return titleCase.toString();
  }

  private void saveToFile() throws IOException {
    FileWriter writer = new FileWriter(conf.auxFile);
    writer.write("===mainDict===\n");
    for (String line : mainDict.keySet()) {
      writer.write(escape(line) + ":" + mainDict.get(line) + "\n");
    }
    writer.write("===secondDictIndex===\n");
    for (String line : secondDictIndex.keySet()) {
      Set<String> set = secondDictIndex.get(line);
      Set<String> newSet = new HashSet<>();
      for (String str : set) {
        newSet.add(escape(str));
      }
      writer.write(line + ":" + newSet.toString() + "\n");
    }
    writer.write("===mainIndexSet===\n");
    for (String line : mainIndexSet) {
      writer.write(line + "\n");
    }
    writer.write("===mainIndex===\n");
    for (String line : mainIndex) {
      writer.write(line + "\n");
    }
    writer.write("===entries===\n");
    for (Entry line : entries) {
      writer.write(line.toString() + "\n");
    }
    writer.close();
  }

  private void readFromFile() throws FileNotFoundException, IOException {
    mainDict.clear();
    secondDictIndex.clear();
    mainIndexSet.clear();
    mainIndex.clear();
    entries.clear();
    try (BufferedReader br = new BufferedReader(new FileReader(conf.auxFile))) {
      String line;
      String mode = "";
      while ((line = br.readLine()) != null) {
        line = line.strip();
        if (line.startsWith("===")) {
          mode = line;
        } else {
          switch (mode) {
          case "===mainDict===":
            String[] parts = line.split(":");
            String numbers[] = parts[1].substring(1, parts[1].length() - 1).split(", ");
            List<Integer> l = new ArrayList<>();
            for (String number : numbers) {
              l.add(Integer.parseInt(number));
            }
            mainDict.put(unescape(parts[0]), l);
            break;
          case "===secondDictIndex===":
            parts = line.split(":");
            String strs[] = parts[1].substring(1, parts[1].length() - 1).split(", ");
            Set<String> s = new HashSet<>();
            for (String str : strs) {
              s.add(unescape(str));
            }
            secondDictIndex.put(parts[0], s);
            break;
          case "===mainIndexSet===":
            mainIndexSet.add(line);
            break;
          case "===mainIndex===":
            mainIndex.add(line);
            break;
          case "===entries===":
            Entry e = new Entry(line);
            entries.add(e);
            break;
          }

        }

      }
    }
  }

  private String escape(String s) {
    return s.replaceAll("\\\\:", "_DOS_PUNTS_").replaceAll("\\\\,", "_COMA_").replaceAll(":", "_DOS_PUNTS_")
        .replaceAll(",", "_COMA_");
  }

  private String unescape(String s) {
    return s.replaceAll("_COMA_", ",").replaceAll("_DOS_PUNTS_", ":");
  }

}