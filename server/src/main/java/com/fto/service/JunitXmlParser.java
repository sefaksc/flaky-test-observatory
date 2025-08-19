package com.fto.service;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class JunitXmlParser {

    public static class ParsedCase {
        public String suite;
        public String name;
        public String file;
        public long durationMs;
        public String status; // passed|failed|skipped|error
        public String failureMessage; // null unless failed/error
    }

    public static class ParsedReport {
        public Instant startedAt; // nullable
        public List<ParsedCase> cases = new ArrayList<>();
    }

    public ParsedReport parse(InputStream in) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // XXE koruması
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setExpandEntityReferences(false);
        Document doc = dbf.newDocumentBuilder().parse(in);
        doc.getDocumentElement().normalize();

        ParsedReport report = new ParsedReport();
        List<Element> suites = new ArrayList<>();

        Element root = doc.getDocumentElement();
        if ("testsuite".equals(root.getTagName())) {
            suites.add(root);
        } else if ("testsuites".equals(root.getTagName())) {
            NodeList nl = root.getElementsByTagName("testsuite");
            for (int i = 0; i < nl.getLength(); i++) suites.add((Element) nl.item(i));
        } else {
            throw new IllegalArgumentException("Unsupported root element: " + root.getTagName());
        }

        // startedAt (ilk suite'in timestamp'i varsa)
        for (Element s : suites) {
            String ts = s.getAttribute("timestamp");
            if (ts != null && !ts.isBlank()) {
                try { report.startedAt = Instant.parse(ts); break; } catch (Exception ignored) {}
            }
        }
        if (report.startedAt == null) report.startedAt = Instant.now();

        for (Element s : suites) {
            String suiteName = s.getAttribute("name");
            NodeList tcs = s.getElementsByTagName("testcase");
            for (int i = 0; i < tcs.getLength(); i++) {
                Element tc = (Element) tcs.item(i);
                ParsedCase pc = new ParsedCase();
                pc.suite = suiteName != null ? suiteName : "";
                pc.name  = tc.getAttribute("name");
                pc.file  = tc.getAttribute("file"); // bazı raporlarda yok -> null olabilir
                String timeAttr = tc.getAttribute("time");
                pc.durationMs = parseDurationMs(timeAttr);

                // Status belirleme: failure > error > skipped > passed
                NodeList failures = tc.getElementsByTagName("failure");
                NodeList errors   = tc.getElementsByTagName("error");
                NodeList skipped  = tc.getElementsByTagName("skipped");

                if (failures.getLength() > 0) {
                    pc.status = "failed";
                    pc.failureMessage = textOf((Element) failures.item(0));
                } else if (errors.getLength() > 0) {
                    pc.status = "error";
                    pc.failureMessage = textOf((Element) errors.item(0));
                } else if (skipped.getLength() > 0) {
                    pc.status = "skipped";
                } else {
                    pc.status = "passed";
                }

                report.cases.add(pc);
            }
        }
        return report;
    }

    private static long parseDurationMs(String timeAttr) {
        if (timeAttr == null || timeAttr.isBlank()) return 0L;
        try {
            // JUnit çoğunlukla saniye (double) verir: 0.123
            double sec = Double.parseDouble(timeAttr);
            return (long)Math.round(sec * 1000.0);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String textOf(Element el) {
        if (el == null) return null;
        StringBuilder sb = new StringBuilder();
        NodeList children = el.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
                sb.append(n.getNodeValue());
            }
        }
        String s = sb.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
