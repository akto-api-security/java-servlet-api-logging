package com.akto.filters;

import com.akto.utils.CustomHttpServletRequestWrapper;
import com.akto.utils.CustomHttpServletResponseWrapper;
import com.akto.utils.Kafka;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LoggingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig){ }

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest= (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        CustomHttpServletRequestWrapper aa = new CustomHttpServletRequestWrapper(httpServletRequest);
        CustomHttpServletResponseWrapper bb = new CustomHttpServletResponseWrapper(httpServletResponse);
        chain.doFilter(aa,bb);

        if (httpServletRequest!=null) {
            Map<String,Object> finalMap = new HashMap<>();
            finalMap.put("time", (int)(System.currentTimeMillis() / 1000L));

            logRequest(aa, finalMap);
            logResponse(bb, finalMap);

            final String json = mapper.writeValueAsString(finalMap);
            // TODO:
            new Kafka().send(json);
        }

    }

    private void logRequest(final CustomHttpServletRequestWrapper request, Map<String, Object> finalMap) throws IOException {
        finalMap.put("path", request.getRequestURI());
        finalMap.put("method", request.getMethod());
        finalMap.put("ip", request.getRemoteAddr());
        finalMap.put("type", request.getProtocol());
        Map<String,Object> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        finalMap.put("requestHeaders",headers);
        ServletInputStream inputStream = request.getInputStream();
        Object obj = mapper.readValue(inputStream,Object.class);
        finalMap.put("requestPayload", obj);
    }

    private void logResponse(final CustomHttpServletResponseWrapper response, Map<String, Object> finalMap) throws IOException {
        Map<String,Object> headers = new HashMap<>();
        Collection<String> headerNames = response.getHeaderNames();
        for (String headerName: headerNames) {
            headers.put(headerName, response.getHeader(headerName));
        }
        finalMap.put("responseHeaders", headers);
        String res = new String(response.toByteArray(), StandardCharsets.UTF_8);
        finalMap.put("statusCode", response.getStatus());
        finalMap.put("status", null);
        String contentType = response.getContentType();
        finalMap.put("contentType", contentType);

        if (contentType == null || !contentType.split(";")[0].equals("application/json")) {
            res = "{}";
        }
        Object obj = mapper.readValue(res,Object.class);
        finalMap.put("responsePayload", obj);

    }

    @Override
    public void destroy() {}
}