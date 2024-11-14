package utb.fai;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class Parser implements Runnable {

    private URI pageURI;
    private int depth;
    Set<URI> visitedURIs;
    ConcurrentLinkedQueue<URIinfo> foundURIs;
    ConcurrentHashMap<String, Integer> map;

    Parser(Set<URI> visitedURIs, ConcurrentLinkedQueue<URIinfo> foundURIs,
            ConcurrentHashMap<String, Integer> map, URI pageURI, int depth) {
        this.pageURI = pageURI;
        this.depth = depth;
        this.foundURIs = foundURIs;
        this.visitedURIs = visitedURIs;
        this.map = map;
    }

    private void crawl() throws IOException {
        if (visitedURIs.contains(pageURI)) {
            return;
        }
        Document doc = Jsoup.connect(pageURI.toString()).get();
        parseLinks(doc);
        parseText(doc);
        synchronized (visitedURIs) {
            visitedURIs.add(pageURI);
        }
    }

    public void parseLinks(Document doc) {
        Elements elements = doc.select("a[href]");
        for (Element element : elements) {
            String link = element.absUrl("href");
            if (link == null || link.isEmpty()) {
                continue;
            }
            URIinfo uriInfo = new URIinfo(link, depth + 1);
            synchronized (foundURIs) {
                foundURIs.add(uriInfo);
            }
        }
    }

    private void parseText(Document doc) throws IOException {
        String[] words = doc.text().toLowerCase().replaceAll("[^a-zA-Z\\s]", "").trim().split("\\s+");
        for (String word : words) {
            if (word.length() == 1 && !word.equals("a")) {
                continue;
            }
            map.put(word, map.containsKey(word) ? map.get(word) + 1 : 1);
        }
    }

    public static void printResults(ConcurrentHashMap<String, Integer> map) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
        for (Map.Entry<String, Integer> entry : list.subList(0, 20)) {
            System.out.println(entry.getKey() + ";" + entry.getValue());
        }
    }

    @Override
    public void run() {
        try {
            crawl();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
