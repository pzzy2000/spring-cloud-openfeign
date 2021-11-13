package org.springframework.cloud.openfeign.thirds;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class MYCloudRequestParamMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private Logger logger = LogManager.getLogger(MYCloudRequestParamMethodArgumentResolver.class);

    @Override
    public boolean supportsParameter (MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MyParams.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object resolveArgument (MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);

        Assert.state(servletRequest != null, "No HttpServletRequest");
        JSONObject data = null;
        if (servletRequest.getAttribute("params_value") == null) {
            data = create(servletRequest);
            if (data == null)
                return null;
            servletRequest.setAttribute("params_value", data);
        } else {
            data = (JSONObject)servletRequest.getAttribute("params_value");
        }

        MyParams iFormModel = parameter.getParameterAnnotation(MyParams.class);

        Object value = data.get(iFormModel.name());

        if (value == null) {
            return null;
        }

        if (value instanceof JSONObject) {
            return ((JSONObject)value).toJavaObject(parameter.getParameterType());
        }

        else

        if (value instanceof JSONArray) {
            return ((JSONArray)value).toJavaObject(parameter.getParameterType());
        }

        else {

            Converter iConverter = ConvertUtils.lookup(parameter.getParameterType());
            if (iConverter != null) {
                return iConverter.convert(parameter.getParameterType(), value);
            }

            if (parameter.getParameterType().isEnum()) {

                return Enum.valueOf(((Class)parameter.getParameterType()), value.toString());

            }

            {
                throw new Exception("not support " + parameter.getParameterType() + "  type class !");
            }

        }

    }

    private JSONObject create (HttpServletRequest servletRequest) throws Exception {

        ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(servletRequest);

        EmptyBodyCheckingHttpInputMessage message = new EmptyBodyCheckingHttpInputMessage(inputMessage);
        if (message.hasBody()) {
            InputStream iInputStream = message.body;
            try {

                byte[] data = new byte[iInputStream.available()];
                iInputStream.read(data);

                JSONObject result = JSONObject.parseObject(new String(data));
                return result;
            } finally {
                if (iInputStream != null) {
                    try {
                        iInputStream.close();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        } else {
            return null;
        }

    }

    private static class EmptyBodyCheckingHttpInputMessage implements HttpInputMessage {

        private final HttpHeaders headers;

        @Nullable
        private final InputStream body;

        public EmptyBodyCheckingHttpInputMessage(HttpInputMessage inputMessage) throws IOException {
            this.headers = inputMessage.getHeaders();
            InputStream inputStream = inputMessage.getBody();
            if (inputStream.markSupported()) {
                inputStream.mark(1);
                this.body = (inputStream.read() != -1 ? inputStream : null);
                inputStream.reset();
            } else {
                PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
                int b = pushbackInputStream.read();
                if (b == -1) {
                    this.body = null;
                } else {
                    this.body = pushbackInputStream;
                    pushbackInputStream.unread(b);
                }
            }
        }

        @Override
        public HttpHeaders getHeaders () {
            return this.headers;
        }

        @Override
        public InputStream getBody () {
            return (this.body != null ? this.body : StreamUtils.emptyInput());
        }

        public boolean hasBody () {
            return (this.body != null);
        }
    }

}
