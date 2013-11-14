package de.tuberlin.cit.livescale.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;


public class CitStreamerWebServer extends AbstractHandler {
	
	public static boolean DEBUG = true;
	public static String TEMPLATE_LINK_PLACEHOLDER = "###";
	public static String TEMPLATE_INFO_PLACEHOLDER = "***";
	public static String URL_REGEXP = "([0-9]{1,5})/watch/(.*)";
	public static String ILLEGAL_CHARS_REGEXP = "[^a-z,A-Z,0-9,-]";
	
	private static String mTemplateString = "";
	private static int port = 8080;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//args = new String[] { "/home/dust/uni/ba/jetty/watch_template.html","8080" };
		
		if (args.length != 2) {
			throw new Exception("Unable to start! Required parameters: <html template file> <port>");
		}
		
		try {
			File file = new File(args[0]);
			BufferedReader buff = new BufferedReader(new FileReader(file));
			String line;
			while ((line = buff.readLine()) != null) {
				mTemplateString += "\n" + line;
			}
			
			if (! mTemplateString.contains(TEMPLATE_LINK_PLACEHOLDER) || ! mTemplateString.contains(TEMPLATE_INFO_PLACEHOLDER))
				throw new Exception("Unable to find template placeholders " + TEMPLATE_LINK_PLACEHOLDER + " or " + TEMPLATE_INFO_PLACEHOLDER);
			
			port = Integer.parseInt(args[1]);
		} catch (Exception e) {
			throw new Exception("Error while parsing template file " + args[0] + ": " + e.getMessage());
		}
		
		Server server = new Server(port);
		server.setHandler(new CitStreamerWebServer());
        server.start();
        server.join();
	}

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		Matcher urlMatcher = Pattern.compile(URL_REGEXP).matcher(target);
		if (urlMatcher.find()) {
			if (DEBUG)
				System.out.println("Correct request from " + baseRequest.getRemoteHost() + ": " + target);
			
			String port = urlMatcher.group(1);
			String rcvToken = urlMatcher.group(2);
			
			response.setContentType("text/html;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			String localAddress = request.getLocalAddr();
			String newTarget = localAddress + "/" + port + "/watch/" + rcvToken.replaceAll(ILLEGAL_CHARS_REGEXP, "");
			String manualData = "Server " + localAddress + ", port " + port + ", stream code: " + rcvToken;
			if (newTarget.length() > 64)
				newTarget = newTarget.substring(0, 63);
			String newTemplateString = mTemplateString.replace(TEMPLATE_LINK_PLACEHOLDER, newTarget).replace(TEMPLATE_INFO_PLACEHOLDER, manualData);
			response.getWriter().print(newTemplateString);

		} else {
			if (DEBUG)
				System.out.println("Illegal request from " + baseRequest.getRemoteHost() + ": " + target);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			baseRequest.setHandled(true);
		}
	}
}