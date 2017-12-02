package fr.estela.peerframe.device.util;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Basic request filter to set permissive CORS response headers.
 * Required to allow all applications to connect to the APIs.
 * @author aestela
 */
public class CORSFilter implements javax.servlet.Filter {

    /**
     * Sets the appropriate permissive CORS response headers.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletResponse res = (HttpServletResponse) response;
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        res.addHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // not implemented
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // not implemented
    }
}