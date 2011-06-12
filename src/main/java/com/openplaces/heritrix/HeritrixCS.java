package com.openplaces.heritrix;

import org.archive.crawler.Heritrix;
import org.archive.crawler.restlet.EngineApplication;
import org.archive.crawler.restlet.RateLimitGuard;
import org.restlet.Router;


public class HeritrixCS extends Heritrix {

	public void instanceMain(String[] args) throws Exception {
		super.instanceMain(args);

		RateLimitGuard rateLimitGuard = (RateLimitGuard) getComponent().getDefaultHost().getRoutes().get(0).getNext();
		EngineApplication engineApplication = (EngineApplication) rateLimitGuard.getNext();

		Router router = (Router) engineApplication.getRoot();
		router.attach("/engine/job/{job}/seeding", SeedResource.class);
		router.attach("/engine/job/{job}/seed-stats", SeedStatResource.class);
		router.attach("/engine/job/{job}/seed-stats/{seed_url}", SeedStatResource.class);
	}

	public static void main(String[] args) throws Exception {
        new HeritrixCS().instanceMain(args); 
    }
}
