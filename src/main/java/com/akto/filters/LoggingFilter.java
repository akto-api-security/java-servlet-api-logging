package com.akto.filters;

import com.akto.utils.CustomHttpServletRequestWrapper;
import com.akto.utils.CustomHttpServletResponseWrapper;
import com.akto.utils.Kafka;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LoggingFilter implements Filter {
    private String brokerIP;
    private String topic;
    private Kafka kafka;
    private String akto_account_id;

    @Override
    public void init(FilterConfig filterConfig){
        brokerIP = filterConfig.getInitParameter("brokerIP");
        topic = filterConfig.getInitParameter("topic");
        this.akto_account_id  = filterConfig.getInitParameter("akto_account_id");
        kafka = new Kafka(brokerIP);
    }

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest= (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        CustomHttpServletRequestWrapper aa = new CustomHttpServletRequestWrapper(httpServletRequest);
        CustomHttpServletResponseWrapper bb = new CustomHttpServletResponseWrapper(httpServletResponse);
        chain.doFilter(aa,bb);

        if (httpServletRequest!=null) {
            Map<String,String> finalMap = new HashMap<>();
            finalMap.put("time", (int)(System.currentTimeMillis() / 1000L)+"");

            String contentType = bb.getContentType();
            List<String> dd = Arrays.asList(contentType.split(";"));
            if (contentType == null || !dd.contains("application/json")) {
                return ;
            }

            logRequest(akto_account_id,aa, finalMap);
            logResponse(bb, finalMap);
            
            final String json = mapper.writeValueAsString(finalMap);
            // todo:
            kafka.send(json, topic);
        }

    }

    private void logRequest(String akto_account_id,final CustomHttpServletRequestWrapper request, Map<String, String> finalMap) throws IOException {
        finalMap.put("akto_account_id", akto_account_id);
        finalMap.put("path", request.getRequestURI());
        finalMap.put("method", request.getMethod());
        finalMap.put("ip", request.getRemoteAddr());
        finalMap.put("type", request.getProtocol());
        Map<String,String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        String json = mapper.writeValueAsString(headers);
        finalMap.put("requestHeaders", json);
        ServletInputStream inputStream = request.getInputStream();
        String req = IOUtils.toString(inputStream, "UTF-8");
        finalMap.put("requestPayload", req);
    }

    private void logResponse(final CustomHttpServletResponseWrapper response, Map<String, String> finalMap) throws IOException {
        Map<String,String> headers = new HashMap<>();
        Collection<String> headerNames = response.getHeaderNames();
        for (String headerName: headerNames) {
            headers.put(headerName, response.getHeader(headerName));
        }

        String json = mapper.writeValueAsString(headers);

        finalMap.put("responseHeaders", json);
        String res = new String(response.toByteArray(), StandardCharsets.UTF_8);
        finalMap.put("statusCode", response.getStatus() + "");
        finalMap.put("status", null);
        String contentType = response.getContentType();
        finalMap.put("contentType", contentType);

        finalMap.put("responsePayload",res);

    }

    @Override
    public void destroy() {}
}
