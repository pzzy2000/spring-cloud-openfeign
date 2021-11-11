package org.springframework.cloud.openfeign.annotation;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import cn.oxo.iworks.web.controller.bind.annotation.FormModel;
import feign.MethodMetadata;

public class FormModelParameterProcessor implements AnnotatedParameterProcessor {

    private static final Class<FormModel> ANNOTATION = FormModel.class;

    @Override
    public Class<? extends Annotation> getAnnotationType () {

        return ANNOTATION;
    }

    @Override
    public boolean processArgument (AnnotatedParameterContext context, Annotation annotation, Method method) {
        FormModel requestParam = ANNOTATION.cast(annotation);
        String name = requestParam.parameterName();
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
