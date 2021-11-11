package org.springframework.cloud.openfeign.annotation;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.cloud.openfeign.thirds.MyParams;

import feign.MethodMetadata;

public class MyParamsterProcessor implements AnnotatedParameterProcessor {

    private static final Class<MyParams> ANNOTATION = MyParams.class;

    @Override
    public Class<? extends Annotation> getAnnotationType () {

        return ANNOTATION;
    }

    @Override
    public boolean processArgument (AnnotatedParameterContext context, Annotation annotation, Method method) {
        MyParams requestParam = ANNOTATION.cast(annotation);
        String name = requestParam.name();
        checkState(emptyToNull(name) != null, "FormModel.value() was empty on parameter %s", context.getParameterIndex());
        context.setParameterName(name);

        MethodMetadata data = context.getMethodMetadata();
        String varName = '{' + name + '}';
        if (!data.template().url().contains(varName) && !searchMapValues(data.template().queries(), varName) && !searchMapValues(data.template().headers(), varName)) {
            data.formParams().add(name);
        }
        data.setFormModule(true);
        return true;
    }

    private <K, V> boolean searchMapValues (Map<K, Collection<V>> map, V search) {
        Collection<Collection<V>> values = map.values();
        if (values == null) {
            return false;
        }
        for (Collection<V> entry : values) {
            if (entry.contains(search)) {
                return true;
            }
        }
        return false;
    }

}
