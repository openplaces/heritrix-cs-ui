package com.openplaces.heritrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.bdb.DisposableStoredSortedMap;
import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.Engine;
import org.archive.crawler.reporting.SeedRecord;
import org.archive.crawler.reporting.StatisticsTracker;
import org.archive.crawler.restlet.BaseResource;
import org.archive.crawler.restlet.EngineApplication;
import org.archive.crawler.restlet.Flash;
import org.archive.modules.net.CrawlHost;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.TextUtils;
import org.restlet.Context;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.restlet.resource.WriterRepresentation;
import org.restlet.util.XmlWriter;
import org.xml.sax.SAXException;

public class SeedStatResource extends BaseResource {

	private static final Logger LOGGER = Logger.getLogger(SeedStatResource.class.getName());

	protected CrawlJob cj;
	private String _seedUrl;

	public SeedStatResource(Context ctx, Request req, Response res) {
		super(ctx, req, res);
		setModifiable(true);
		getVariants().add(new Variant(MediaType.TEXT_HTML));
		getVariants().add(new Variant(MediaType.APPLICATION_XML));
		cj = getEngine().getJob(TextUtils.urlUnescape((String)req.getAttributes().get("job")));

		String seedUrlString = (String)req.getAttributes().get("seed_url");
		if (seedUrlString != null)
			_seedUrl = TextUtils.urlUnescape(seedUrlString);
	}

	protected Engine getEngine() {
		return ((EngineApplication)getApplication()).getEngine();
	}

	public Representation represent(Variant variant) throws ResourceException {
		if (cj == null) {
			throw new ResourceException(404);
		}

		Representation representation = null;
		if (variant.getMediaType() == MediaType.APPLICATION_XML) {
			representation = new WriterRepresentation(MediaType.APPLICATION_XML) {
				public void write(Writer writer) throws IOException {
					SeedStatResource.this.writeXml(writer);
				}
			};
		} else {
			representation = new WriterRepresentation(MediaType.TEXT_HTML) {
				public void write(Writer writer) throws IOException {
					SeedStatResource.this.writeHtml(writer);
				}
			};
		}

		representation.setCharacterSet(CharacterSet.UTF_8);

		return representation;
	}

	private Map<String, CrawlHost> getCrawlHosts() {
		StatisticsTracker st = cj.getCrawlController().getStatisticsTracker();
		Map<String, CrawlHost> crawlHosts = new HashMap<String, CrawlHost>();

		if (_seedUrl != null) {
			try {
				UURI seedUuri = UURIFactory.getInstance(_seedUrl);
				CrawlHost host = st.getServerCache().getHostFor(seedUuri);
				crawlHosts.put(host.getHostName(), host);
			} catch (URIException e) {
				e.printStackTrace();
			}
		} else {
			DisposableStoredSortedMap<Long,String> hd = st.calcReverseSortedHostsDistribution();
			for (Map.Entry<Long,String> entry : hd.entrySet()) {
				CrawlHost host = st.getServerCache().getHostFor(entry.getValue());
				crawlHosts.put(host.getHostName(), host);
			}
			hd.dispose();
		}

		return crawlHosts;
	}

	private Map<String, List<SeedRecord>> getSeedRecords(Map<String, CrawlHost> crawlHosts) {
		Map<String, List<SeedRecord>> seedRecords = new HashMap<String, List<SeedRecord>>();

		StatisticsTracker stats = cj.getCrawlController().getStatisticsTracker();
		DisposableStoredSortedMap<Integer, SeedRecord> seedsByCode = stats.calcSeedRecordsSortedByStatusCode();
		Iterator<Map.Entry<Integer,SeedRecord>> iter = seedsByCode.entrySet().iterator();

		while(iter.hasNext()) {
			Map.Entry<Integer,SeedRecord> entry = iter.next();
			SeedRecord seedRecord = entry.getValue();

			try {
				UURI seedUuri = UURIFactory.getInstance(seedRecord.getUri());
				String host = seedUuri.getHost();
				if (crawlHosts.containsKey(host)) {
					if (seedRecords.containsKey(host)) {
						seedRecords.get(host).add(seedRecord);
					} else {
						List<SeedRecord> list = new ArrayList<SeedRecord>();
						list.add(seedRecord);
						seedRecords.put(host, list);
					}
				}
			} catch (URIException e) {
				e.printStackTrace();
			}
		}

		return seedRecords;
	}


	protected void writeXml(Writer writer) {
		PrintWriter pw = new PrintWriter(writer);

		// I hate XML. I'm aware of XmlMarshaller, but unfortunately could not use it.
		XmlWriter xmlWriter = new XmlWriter(pw);
		xmlWriter.setDataFormat(true);
		xmlWriter.setIndentStep(2);

		try {
			xmlWriter.startDocument();
			xmlWriter.startElement("hosts");

			if (cj.isRunning()) {
				Map<String, CrawlHost> crawlHosts = getCrawlHosts();
				Map<String, List<SeedRecord>> seedRecords = getSeedRecords(crawlHosts);

				StatisticsTracker stats = cj.getCrawlController().getStatisticsTracker();

				for (CrawlHost host : crawlHosts.values()) {
					xmlWriter.startElement("host");
					writeDataElement(xmlWriter, "name", host.getHostName());
					writeDataElement(xmlWriter, "ip", (host.getIP() != null) ? host.getIP().getHostAddress() : null);
					String totalDownloaded;
					try {
						totalDownloaded = Long.toString(stats.getBytesPerHost(host.getHostName()));
					} catch (Exception e) {
						totalDownloaded = null;
					}
					writeDataElement(xmlWriter, "bytesDownloaded", totalDownloaded);

					for(Map.Entry<String, Object> entry : host.getSubstats().shortReportMap().entrySet()) {
						writeDataElement(xmlWriter, entry.getKey(), entry.getValue().toString());
					}

					xmlWriter.startElement("seeds");
					if (seedRecords.containsKey(host.getHostName())) {
						for (SeedRecord sr : seedRecords.get(host.getHostName())) {
							xmlWriter.startElement("seed");
							writeDataElement(xmlWriter, "url", sr.getUri());
							writeDataElement(xmlWriter, "redirectUri", sr.getRedirectUri());
							xmlWriter.endElement("seed");
						}
					}
					xmlWriter.endElement("seeds");

					xmlWriter.endElement("host");
				}
			}

			xmlWriter.endElement("hosts");
			xmlWriter.endDocument();
			pw.flush();
		} catch (SAXException e) {
			LOGGER.log(Level.SEVERE, "The following errors were encountered while creating the " +
					"XML document of hosts:\n" + e.getMessage());
		}
	}

	private void writeDataElement(XmlWriter xmlWriter, String key, String value) throws SAXException {
		if (value == null) {
			xmlWriter.emptyElement(key);
		} else {
			xmlWriter.dataElement(key, value);
		}
	}

	protected void writeHtml(Writer writer) {
		PrintWriter pw = new PrintWriter(writer);

		String jobTitle = cj.getShortName() + " - Seed statistics page";

		String baseRef = getRequest().getResourceRef().getBaseRef().toString();
		if(!baseRef.endsWith("/")) {
			baseRef += "/";
		}

		pw.println("<html><head><title>"+jobTitle+"</title>");
		pw.println("<base href='"+baseRef+"'/>");
		pw.println("</head><body>");
		pw.print("<h1>Job <i>"+cj.getShortName()+"</i></h1>");
		cj.writeHtmlTo(pw, "/engine/job/");

		Flash.renderFlashesHTML(pw, getRequest());

		pw.println("<br/>");

		if (cj.isRunning()) {
			Map<String, CrawlHost> crawlHosts = getCrawlHosts();

			for (CrawlHost crawlHost : crawlHosts.values()) {
				pw.println("<p>");
				pw.println(crawlHost.getHostName());
				pw.println(crawlHost.getSubstats().shortReportMap());
				pw.println("</p>");
			}
		}

		pw.println("</body></html>");
		pw.flush();
	}
}
