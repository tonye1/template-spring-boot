package com.ibm.cloud_garage.logging.inbound;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class HttpRequestWrapper implements HttpRequest {
    private final HttpServletRequest request;

    public HttpRequestWrapper(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public String getMethodValue() {
        return request.getMethod();
    }

    @Override
    public URI getURI() {
        try {
            return new URI(getRequestPath(request.getRequestURI(), request.getParameterMap()));
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    protected String getRequestPath(String requestUri, Map<String, String[]> parameterMap) {
        String requestPath = !StringUtils.isEmpty(requestUri) ? requestUri : "/";

        if (!CollectionUtils.isEmpty(parameterMap)) {
            String parameterString = parameterMap.entrySet().stream()
                    .flatMap(this::flattenValueArrayIntoStreamOfKeyAndValue)
                    .collect(Collectors.joining("&"));

            requestPath += "?" + parameterString;
        }

        return requestPath;
    }

    protected Stream<String> flattenValueArrayIntoStreamOfKeyAndValue(Map.Entry<String, String[]> entry) {
        return Arrays.stream(entry.getValue())
                .map(value -> entry.getKey() + "=" + value);
    }

    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();

        new IterableEnumeration<>(request.getHeaderNames()).forEach(headerName -> {
            new IterableEnumeration<>(request.getHeaders(headerName)).forEach(headerValue -> {
                headers.add(headerName, headerValue);
            });
        });

        return headers;
    }

    public static class IterableEnumeration<T> implements Iterable<T> {
        private final Enumeration<T> enumeration;

        public IterableEnumeration(Enumeration<T> enumeration) {
            this.enumeration = enumeration;
        }

        @Override
        public Iterator<T> iterator() {
            return new EnumerationIterator<>(enumeration);
        }
    }

    public static class EnumerationIterator<T> implements Iterator<T> {
        private final Enumeration<T> enumeration;

        public EnumerationIterator(Enumeration<T> enumeration) {
            this.enumeration = enumeration;
        }

        @Override
        public boolean hasNext() {
            return enumeration.hasMoreElements();
        }

        @Override
        public T next() {
            return enumeration.nextElement();
        }
    }
}
