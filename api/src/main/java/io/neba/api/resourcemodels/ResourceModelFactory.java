package io.neba.api.resourcemodels;

/**
 * @author Olaf Otto
 */
public interface ResourceModelFactory {
    Object getModel(String modelName);
    Class<?> getType(String modelName);
}
