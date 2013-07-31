package com.kurento.kmf.content.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.WebApplicationInitializer;

import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.PlayerService;
import com.kurento.kmf.content.RecorderHandler;
import com.kurento.kmf.content.RecorderService;
import com.kurento.kmf.content.WebRtcMediaHandler;
import com.kurento.kmf.content.WebRtcMediaService;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

public class ContentApiWebApplicationInitializer implements
		WebApplicationInitializer {

	public static final String PLAYER_HANDLER_CLASS_PARAM_NAME = ContentApiWebApplicationInitializer.class
			.getName() + "playerHandlerClassParamName";

	public static final String RECORDER_HANDLER_CLASS_PARAM_NAME = ContentApiWebApplicationInitializer.class
			.getName() + "recorderHandlerClassParamName";

	private static final Logger log = LoggerFactory
			.getLogger(ContentApiWebApplicationInitializer.class);

	public static final String WEB_RTC_MEDIA_HANDLER_CLASS_PARAM_NAME = ContentApiWebApplicationInitializer.class
			.getName() + "webRtcMediaHandlerClassParamName";

	@Override
	public void onStartup(ServletContext sc) throws ServletException {

		// At this stage we cannot create KurentoApplicationContext given that
		// we don't know y App developer wants to instantiate a Spring root
		// WebApplicationContext
		// ... so we need to live without Spring

		// Initialize ContentApi locating handlers and creating their associated
		// servlets
		initializeRecorders(sc);
		initializePlayers(sc);
		initializeWebRtcMediaServices(sc);

		// Register Kurento ServletContextListener
		KurentoApplicationContextUtils
				.registerKurentoServletContextListener(sc);
	}

	private void initializePlayers(ServletContext sc) throws ServletException {
		for (String ph : findServices(PlayerHandler.class, PlayerService.class)) {
			try {
				PlayerService playerService = Class.forName(ph).getAnnotation(
						PlayerService.class);
				if (playerService != null) {
					String name = playerService.name();
					String path = playerService.path();

					ServletRegistration.Dynamic sr = sc.addServlet(name,
							PlayerHandlerServlet.class);
					sr.addMapping(path);
					sr.setInitParameter(PLAYER_HANDLER_CLASS_PARAM_NAME, ph);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error("Error: could not find player class in classpath", e);
				throw new ServletException(e);
			}
		}
	}

	private void initializeRecorders(ServletContext sc) throws ServletException {
		for (String rh : findServices(RecorderHandler.class, RecorderService.class)) {
			try {
				RecorderService recorderService = Class.forName(rh)
						.getAnnotation(RecorderService.class);
				if (recorderService != null) {
					String name = recorderService.name();
					String path = recorderService.path();
					ServletRegistration.Dynamic sr = sc.addServlet(name,
							RecorderHandlerServlet.class);
					sr.addMapping(path);
					sr.setInitParameter(RECORDER_HANDLER_CLASS_PARAM_NAME, rh);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error("Error: could not find recorder class in classpath",
						e);
				throw new ServletException(e);
			}
		}
	}

	private void initializeWebRtcMediaServices(ServletContext sc)
			throws ServletException {
		for (String wh : findServices(WebRtcMediaHandler.class, WebRtcMediaService.class)) {
			try {
				WebRtcMediaService mediaService = Class.forName(wh)
						.getAnnotation(WebRtcMediaService.class);
				if (mediaService != null) {
					String name = mediaService.name();
					String path = mediaService.path();
					ServletRegistration.Dynamic sr = sc.addServlet(name,
							WebRtcMediaHandlerServlet.class);
					sr.addMapping(path);
					sr.setInitParameter(WEB_RTC_MEDIA_HANDLER_CLASS_PARAM_NAME,
							wh);
					sr.setAsyncSupported(true);
				}
			} catch (ClassNotFoundException e) {
				log.error("Error: could not find recorder class in classpath",
						e);
				throw new ServletException(e);
			}
		}

	}

	private List<String> findServices(Class<?> handlerClass,
			Class<? extends Annotation> serviceAnnotation) {
		// TODO: perhaps exclude packages? -- add third parameter new
		// FilterBuilder().execute("org.jboss")
		Reflections reflections = new Reflections("",
				new TypeAnnotationsScanner());
		Set<Class<?>> annotatedList = reflections
				.getTypesAnnotatedWith(serviceAnnotation);
		List<String> handlerList = new ArrayList<String>();
		for (Class<?> clazz : annotatedList) {
			if (handlerClass.isAssignableFrom(clazz)) {
				handlerList.add(clazz.getCanonicalName());
			}
		}
		return handlerList;
	}
}
