package com.akto.utils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.*;

public class CustomHttpServletRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {
    private final String body;

    public CustomHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);

        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();

            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                char[] charBuffer = new char[128];
                int bytesRead = -1;

                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
        } catch (IOException ex) {
            System.out.println("Error reading the request body...");
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    System.out.println("Error closing bufferedReader...");
                }
            }
        }

        body = stringBuilder.toString();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new StringReader(body));
    }

    @Override
    public ServletInputStream getInputStream () throws IOException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());

        ServletInputStream inputStream = new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            public int read () throws IOException {
                return byteArrayInputStream.read();
            }
        };

        return inputStream;
    }
}
