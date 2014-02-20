/*
 * Copyright (C) 2013 Pavel Stastny
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.proarc.authentication;

import static cz.cas.lib.proarc.authentication.utils.AddressUtils.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.stringtemplate.v4.ST;

import cz.cas.lib.proarc.authentication.utils.AddressUtils;

/**
 * Authentication filter
 * @author pavels
 */
public class ProarcAuthFilter implements Filter {

    public static final Logger LOGGER = Logger.getLogger(ProarcAuthFilter.class.getName());
    
    public static final String SESSION_KEY = "user";

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest arg0, ServletResponse arg1,
            FilterChain chain) throws IOException, ServletException {
        String reqUri = ((HttpServletRequest) arg0).getRequestURI();
        // only proarclogin is public resource
        if (reqUri.endsWith("proarclogin")) {
            chain.doFilter(arg0, arg1);
        } else {
            // secured content 
            securedContent(arg0, arg1, chain);
        }
    }
        
    /**
     * Filter secured content
     * @param arg0 {@link HttpServletRequest}
     * @param arg1 {@link HttpServletResponse}
     * @param chain {@link FilterChain}
     * @throws IOException
     * @throws ServletException
     */
    public void securedContent(ServletRequest arg0, ServletResponse arg1,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) arg0;
        HttpServletResponse httpResp = (HttpServletResponse) arg1;
        HttpSession session = httpReq.getSession();
        if (session != null) {
            ProarcPrincipal p = (ProarcPrincipal) session.getAttribute(SESSION_KEY);
            if (p != null) {
                // already logged
                HttpServletRequest authenticatedRequest = ProarcAuthenticatedHTTPRequest.newInstance(httpReq, p, p.getName());
                chain.doFilter(authenticatedRequest, httpResp);
            } else {
                // not logged -> test forbidden resource ?
                if (isForbinddenResource(httpReq.getRequestURL().toString())) {
                    forbiddenResource(httpResp, sessiontimeout().render());
                } else {
                    // not forbidden resource -> chain
                    chain.doFilter(httpReq, httpResp);
                }
            }
        } else {
            // no session -> not logged -> forbidden resource ?
            if (isForbinddenResource(httpReq.getRequestURL().toString())) {
                forbiddenResource(httpResp,sessiontimeout().render());
            } else {
                chain.doFilter(httpReq, httpResp);
            }
        }
    }
        
    
    private boolean isForbinddenResource(String reqUrl) {
        return reqUrl != null && reqUrl.contains("/rest/");
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }

    /**
     * Session timeout content
     * 
     * @return Session timeout template
     * @throws IOException IO Error has been occured
     * @throws UnsupportedEncodingException UTF-8 is unsupported
     * {@link http://www.smartclient.com/docs/6.5.1/a/b/c/go.html#group..relogin}
     */
    public static ST sessiontimeout() throws IOException, UnsupportedEncodingException {
            URL urlRes = ProarcHTTPServlet.class.getClassLoader().getResource("sessiontimeout.st");
        InputStream isStream = urlRes.openStream();
        try {
            String str = IOUtils.toString(isStream, "UTF-8");
            isStream.close();
            ST st = new ST(str, '$', '$');
            return st;
        } finally {
            IOUtils.closeQuietly(isStream);
        }
    }
}
