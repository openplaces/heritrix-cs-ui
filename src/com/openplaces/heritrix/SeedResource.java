package com.openplaces.heritrix;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.framework.CrawlJob;
import org.archive.crawler.framework.Engine;
import org.archive.crawler.reporting.AlertThreadGroup;
import org.archive.crawler.restlet.BaseResource;
import org.archive.crawler.restlet.EngineApplication;
import org.archive.crawler.restlet.Flash;
import org.archive.util.TextUtils;
import org.restlet.Context;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.restlet.resource.WriterRepresentation;

public class SeedResource extends BaseResource {

	private static final Logger LOGGER = Logger.getLogger(SeedResource.class.getName());

	protected CrawlJob cj; 
	
	public SeedResource(Context ctx, Request req, Response res) {
		super(ctx, req, res);
		setModifiable(true);
        getVariants().add(new Variant(MediaType.TEXT_HTML));
        cj = getEngine().getJob(TextUtils.urlUnescape((String)req.getAttributes().get("job")));
	}
	
	protected Engine getEngine() {
        return ((EngineApplication)getApplication()).getEngine();
    }
	
	@Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        if (cj == null) {
            throw new ResourceException(404);
        }

        Form form = null;
        try {
	        form = getRequest().getEntityAsForm();

	        // The only action that should happen here is 'seed'
	        String action = form.getFirstValue("action");
	        if("seed".equals(action)) {
	        	// Grabbing the seed list from the form 'seed_list'
		        String seedList = form.getFirstValue("seed_list");
		        
		        if (!seedList.endsWith("\n"))
		        	seedList += "\n";

	        	Flash.addFlash(getResponse(), "Seeds added: " + seedList, Flash.Kind.ACK);

	        	// Write the seed list to the job's 'action' directory
	        	File actionDir = new File(cj.getJobDir() + "/action");
	        	LOGGER.info("Action Directory: " + actionDir.getAbsolutePath());

	        	if (actionDir.exists()) {
	        		File seedFile = new File(actionDir, "new.seed");
		        	LOGGER.info("Action Directory: " + seedFile.getAbsolutePath());
		        	
		        	try{
						// Create file 
						FileWriter fstream = new FileWriter(seedFile, true);
						BufferedWriter out = new BufferedWriter(fstream);
						out.write(seedList);
						out.close();
	        	    } catch (Exception e) {
	        	    	LOGGER.severe("The following error occurred while trying to add new seeds for continuous crawling:"
	        	    			+ e.getMessage());
	        	    }
	        	}
	        }

	        AlertThreadGroup.setThreadLogger(null);

	        // default: redirect to GET self
	        getResponse().redirectSeeOther(getRequest().getOriginalRef());
        } catch (IllegalStateException e) {
        	LOGGER.log(Level.WARNING, "problem accepting input (redirecting to GET self anyway): " + e, e);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e, "problem accepting input");
        }
    }
	
	public Representation represent(Variant variant) throws ResourceException {
        Representation representation = new WriterRepresentation(MediaType.TEXT_HTML) {
            public void write(Writer writer) throws IOException {
            	SeedResource.this.writeHtml(writer);
            }
        };

        // TODO: remove if not necessary in future?
        representation.setCharacterSet(CharacterSet.UTF_8);

        return representation;
    }
	
	protected void writeHtml(Writer writer) {
        PrintWriter pw = new PrintWriter(writer);

        String jobTitle = cj.getShortName() + " - Job continuous seeding page";

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
        pw.println("<form method='POST'>");
        pw.println("<textarea name='seed_list' rows='10' cols='50'></textarea>");
        pw.println("<br/>");
        pw.println("<input type='submit' name='action' value='seed' />");
        pw.println("</form>");

        pw.println("</body>");
        pw.flush();
	}
}
