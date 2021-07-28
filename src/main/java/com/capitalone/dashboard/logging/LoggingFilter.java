package com.capitalone.dashboard.logging;


import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.RequestLog;
import com.capitalone.dashboard.repository.RequestLogRepository;
import com.capitalone.dashboard.settings.ApiSettings;
import com.capitalone.dashboard.util.CommonConstants;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
@Order(1)
public class LoggingFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger("LoggingFilter");

    private static final String API_USER_KEY = "apiUser";

    private static final String UNKNOWN_USER = "unknown";


    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private ApiSettings settings;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        Map<String, String> requestMap = this.getTypesafeRequestMap(httpServletRequest);
        BufferedRequestWrapper bufferedRequest = new BufferedRequestWrapper(httpServletRequest);
        BufferedResponseWrapper bufferedResponse = new BufferedResponseWrapper(httpServletResponse);

        if (httpServletRequest.getMethod().equals(HttpMethod.PUT.toString()) ||
                (httpServletRequest.getMethod().equals(HttpMethod.POST.toString())) ||
                (httpServletRequest.getMethod().equals(HttpMethod.DELETE.toString()))) {
            long startTime = System.currentTimeMillis();
            String apiUser = bufferedRequest.getHeader(API_USER_KEY);
            String endPointURI = httpServletRequest.getRequestURI();
            if (settings.checkIgnoreEndPoint(endPointURI) || settings.checkIgnoreApiUser(apiUser)) {
                chain.doFilter(bufferedRequest, bufferedResponse);
                return;
            }
            RequestLog requestLog = new RequestLog();
            try {

                requestLog.setClient(httpServletRequest.getRemoteAddr());
                requestLog.setEndpoint(httpServletRequest.getRequestURI());
                requestLog.setMethod(httpServletRequest.getMethod());
                requestLog.setParameter(requestMap.toString());
                requestLog.setApiUser(StringUtils.isNotEmpty(apiUser) ? apiUser : UNKNOWN_USER);
                requestLog.setRequestSize(httpServletRequest.getContentLengthLong());
                requestLog.setRequestContentType(httpServletRequest.getContentType());
                requestLog.setApplication("hygieia-api");

                chain.doFilter(bufferedRequest, bufferedResponse);
                String request_client_reference = bufferedRequest.getHeader(CommonConstants.HEADER_CLIENT_CORRELATION_ID);
                String response_client_reference = bufferedResponse.getHeader(CommonConstants.HEADER_CLIENT_CORRELATION_ID);
                String clientReference = StringUtils.isEmpty(request_client_reference) ? response_client_reference : request_client_reference;
                requestLog.setClientReference(clientReference);
                requestLog.setResponseContentType(httpServletResponse.getContentType());

                boolean skipBody = settings.checkIgnoreBodyEndPoint(endPointURI);
                JSONParser jsonParser = new JSONParser();
                if ((httpServletRequest.getContentType() != null) && (new MimeType(httpServletRequest.getContentType()).match(new MimeType(APPLICATION_JSON_VALUE)))) {
                    requestLog.setRequestBody(jsonParser.parse(bufferedRequest.getRequestBody()));
                }
                if ((bufferedResponse.getContentType() != null) && (new MimeType(bufferedResponse.getContentType()).match(new MimeType(APPLICATION_JSON_VALUE)))) {
                    requestLog.setResponseBody( skipBody ? StringUtils.EMPTY : jsonParser.parse(bufferedResponse.getContent()));
                }
            }
            catch (MimeTypeParseException e) {
                LOGGER.error("Invalid MIME Type detected. Request MIME type=" + httpServletRequest.getContentType() + ". Response MIME Type=" + bufferedResponse.getContentType());
            }
            catch (ParseException e) {
                LOGGER.error("request / response json parsing failed. Error : "+ e.getMessage());
            }
            catch (Exception e){
                LOGGER.error("Internal Error =" + e.getMessage());
                requestLog.setResponseBody(ExceptionUtils.getMessage(e));
                requestLog.setResponseSize(bufferedResponse.getContent().length());
                if(e instanceof HygieiaException){
                    HygieiaException ex = (HygieiaException) e;
                    requestLog.setResponseCode(ex.getErrorCode());
                }else{
                    requestLog.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
                throw e;
            }
            finally {
                requestLog.setResponseSize(bufferedResponse.getContent().length());
                requestLog.setResponseCode(bufferedResponse.getStatus());
                long endTime = System.currentTimeMillis();
                requestLog.setResponseTime(endTime - startTime);
                requestLog.setTimestamp(endTime);
                try {
                    requestLogRepository.save(requestLog);
                } catch (RuntimeException re) {
                    LOGGER.error("Encountered exception while saving request log - " + requestLog.toString(), re);
                }
            }

        } else {
            if (settings.isCorsEnabled()) {

                String clientOrigin = httpServletRequest.getHeader("Origin");

                String corsWhitelist = settings.getCorsWhitelist();
                if (!StringUtils.isEmpty(corsWhitelist)) {
                    List<String> incomingURLs = Arrays.asList(corsWhitelist.trim().split(","));

                    if (incomingURLs.contains(clientOrigin)) {
                        //adds headers to response to allow CORS
                        httpServletResponse.addHeader("Access-Control-Allow-Origin", clientOrigin);
                        httpServletResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
                        httpServletResponse.addHeader("Access-Control-Allow-Headers", "Content-Type");
                        httpServletResponse.addHeader("Access-Control-Max-Age", "1");
                    }
                }

            }
            chain.doFilter(httpServletRequest, httpServletResponse);
        }
    }


    private Map<String, String> getTypesafeRequestMap(HttpServletRequest request) {
        Map<String, String> typesafeRequestMap = new HashMap<>();
        Enumeration<?> requestParamNames = request.getParameterNames();
        if (requestParamNames != null) {
            while (requestParamNames.hasMoreElements()) {
                String requestParamName = (String) requestParamNames.nextElement();
                String requestParamValue = request.getParameter(requestParamName);
                typesafeRequestMap.put(requestParamName, requestParamValue);
            }
        }
        return typesafeRequestMap;
    }


    @Override
    public void destroy() {
    }


    private static final class BufferedRequestWrapper extends HttpServletRequestWrapper {

        private ByteArrayInputStream bais = null;
        private ByteArrayOutputStream baos = null;
        private BufferedServletInputStream bsis = null;
        private byte[] buffer = null;


        public BufferedRequestWrapper(HttpServletRequest req) throws IOException {
            super(req);
            // Read InputStream and store its content in a buffer.

            this.baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int letti;
            try (InputStream is = req.getInputStream()) {
                while ((letti = is.read(buf)) > 0) {
                    this.baos.write(buf, 0, letti);
                }
            }
            this.buffer = this.baos.toByteArray();
        }


        @Override
        public ServletInputStream getInputStream() {
            this.bais = new ByteArrayInputStream(this.buffer);
            this.bsis = new BufferedServletInputStream(this.bais);
            return this.bsis;
        }

        String getRequestBody() throws IOException {
            String line;
            StringBuilder inputBuffer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.getInputStream()))) {
                do {
                    line = reader.readLine();
                    if (null != line) {
                        inputBuffer.append(line);
                    }
                } while (line != null);
            }
            return inputBuffer.toString();
        }

    }


    private static final class BufferedServletInputStream extends ServletInputStream {

        private ByteArrayInputStream bais;

        public BufferedServletInputStream(ByteArrayInputStream bais) {
            this.bais = bais;
        }

        @Override
        public int available() {
            return this.bais.available();
        }

        @Override
        public int read() {
            return this.bais.read();
        }

        @Override
        public int read(byte[] buf, int off, int len) {
            return this.bais.read(buf, off, len);
        }


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
    }

    public class TeeServletOutputStream extends ServletOutputStream {

        private final TeeOutputStream targetStream;

        public TeeServletOutputStream(OutputStream one, OutputStream two) {
            targetStream = new TeeOutputStream(one, two);
        }

        @Override
        public void write(int arg0) throws IOException {
            this.targetStream.write(arg0);
        }

        public void flush() throws IOException {
            super.flush();
            this.targetStream.flush();
        }

        public void close() throws IOException {
            super.close();
            this.targetStream.close();
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {

        }
    }


    public class BufferedResponseWrapper implements HttpServletResponse {

        private HttpServletResponse original;
        private TeeServletOutputStream teeStream;
        private ByteArrayOutputStream bos;
        private PrintWriter teeWriter;

        public BufferedResponseWrapper(HttpServletResponse response) {
            original = response;
        }

        public String getContent() {

            return (bos == null) ? "" : bos.toString();
        }

        @Override
        public PrintWriter getWriter() throws IOException {

            if (this.teeWriter == null) {
                this.teeWriter = new PrintWriter(new OutputStreamWriter(getOutputStream()));
            }
            return this.teeWriter;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {

            if (LoggingFilter.BufferedResponseWrapper.this.teeStream == null) {
                bos = new ByteArrayOutputStream();
                LoggingFilter.BufferedResponseWrapper.this.teeStream = new TeeServletOutputStream(original.getOutputStream(), bos);
            }
            return LoggingFilter.BufferedResponseWrapper.this.teeStream;
        }

        @Override
        public String getCharacterEncoding() {
            return original.getCharacterEncoding();
        }

        @Override
        public String getContentType() {
            return original.getContentType();
        }

        @Override
        public void setCharacterEncoding(String charset) {
            original.setCharacterEncoding(charset);
        }

        @Override
        public void setContentLength(int len) {
            original.setContentLength(len);
        }

        @Override
        public void setContentLengthLong(long l) {

        }

        @Override
        public void setContentType(String type) {
            original.setContentType(type);
        }

        @Override
        public void setBufferSize(int size) {
            original.setBufferSize(size);
        }

        @Override
        public int getBufferSize() {
            return original.getBufferSize();
        }


        @Override
        public void flushBuffer() throws IOException {
            if (teeStream != null) {
                teeStream.flush();
            }
            if (this.teeWriter != null) {
                this.teeWriter.flush();
            }
        }

        @Override
        public void resetBuffer() {
            original.resetBuffer();
        }

        @Override
        public boolean isCommitted() {
            return original.isCommitted();
        }

        @Override
        public void reset() {
            original.reset();
        }

        @Override
        public void setLocale(Locale loc) {
            original.setLocale(loc);
        }

        @Override
        public Locale getLocale() {
            return original.getLocale();
        }

        @Override
        public void addCookie(Cookie cookie) {
            if(cookie != null) {
                cookie.setSecure(Boolean.TRUE);
                original.addCookie(cookie);
            }
        }

        @Override
        public boolean containsHeader(String name) {
            return original.containsHeader(name);
        }

        @Override
        public String encodeURL(String url) {
            return original.encodeURL(url);
        }

        @Override
        public String encodeRedirectURL(String url) {
            return original.encodeRedirectURL(url);
        }

        @SuppressWarnings("deprecation")
        @Override
        public String encodeUrl(String url) {
            return original.encodeUrl(url);
        }

        @SuppressWarnings("deprecation")
        @Override
        public String encodeRedirectUrl(String url) {
            return original.encodeRedirectUrl(url);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            original.sendError(sc, msg);
        }

        @Override
        public void sendError(int sc) throws IOException {
            original.sendError(sc);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            original.sendRedirect(location);
        }

        @Override
        public void setDateHeader(String name, long date) {
            original.setDateHeader(name, date);
        }

        @Override
        public void addDateHeader(String name, long date) {
            original.addDateHeader(name, date);
        }

        @Override
        public void setHeader(String name, String value) {
            original.setHeader(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            original.addHeader(name, value);
        }

        @Override
        public void setIntHeader(String name, int value) {
            original.setIntHeader(name, value);
        }

        @Override
        public void addIntHeader(String name, int value) {
            original.addIntHeader(name, value);
        }

        @Override
        public void setStatus(int sc) {
            original.setStatus(sc);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void setStatus(int sc, String sm) {
            original.setStatus(sc, sm);
        }

        @Override
        public int getStatus() {
            return original.getStatus();
        }

        @Override
        public String getHeader(String s) {
            return null;
        }

        @Override
        public Collection<String> getHeaders(String s) {
            return null;
        }

        @Override
        public Collection<String> getHeaderNames() {
            return null;
        }

    }

}

