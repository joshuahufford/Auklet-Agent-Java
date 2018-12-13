package io.auklet.config;

import io.auklet.Auklet;
import io.auklet.AukletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

/**
 * <p>Base class of all Auklet agent config files that are sourced from the API.</p>
 *
 * <p>Type {@code A} represents the data type that is returned by the Auklet API. This can be the same
 * as type {@code T}.</p>
 */
public abstract class AbstractConfigFileFromApi<T, A> extends AbstractConfigFile<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfigFileFromApi.class);

    /**
     * <p>Constructor.</p>
     *
     * @param agent the Auklet agent object.
     */
    protected AbstractConfigFileFromApi(Auklet agent) {
        super(agent);
    }

    /**
     * <p>Loads the config for this object, either from disk or from the API. If the latter, this method
     * persists the API response to disk prior to returning.</p>
     *
     * @return never {@code null}.
     * @throws AukletException if the config cannot be read from disk or fetched from the API, or if it
     * cannot be written to disk.
     */
    protected final A loadConfig() throws AukletException {
        A config = this.readFromDisk();
        if (config == null) {
            config = this.fetchFromApi();
            this.writeToDisk(config);
        }
        return config;
    }

    /**
     * <p>Returns the config file contents from disk.</p>
     *
     * @return {@code null} if and only if the file does not exist on disk or could not be read.
     */
    protected abstract A readFromDisk();

    /**
     * <p>Fetches the config file from the Auklet API.</p>
     *
     * @return never {@code null}.
     * @throws AukletException if there is a problem communicating with the API.
     */
    protected abstract A fetchFromApi() throws AukletException;

    /**
     * <p>Writes the config file to disk.</p>
     *
     * @param contents never {@code null}.
     * @throws AukletException if an error occurs while writing the file.
     */
    protected abstract void writeToDisk(A contents) throws AukletException;

    /**
     * <p>Loads the config file from disk into a string, using the UTF-8 charset.</p>
     *
     * @return never {@code null}.
     * @throws IOException if the file does not exit or cannot be read.
     */
    protected final String getStringFromDisk() throws IOException {
        try {
            byte[] bytes = Files.readAllBytes(this.file.toPath());
            return new String(bytes, "UTF-8");
        } catch (SecurityException e) {
            throw new IOException(e);
        }
    }

    /**
     * <p>Writes the given string to the config file on disk, using the UTF-8 charset.</p>
     *
     * @param s the string to write.
     * @throws AukletException if an error occurs while writing to disk.
     */
    protected final void saveStringToDisk(String s) throws AukletException {
        try {
            Files.write(this.file.toPath(), s.getBytes("UTF-8"));
        } catch (IOException e) {
            throw new AukletException("Could not write Auklet device cert file to disk", e);
        }
    }

}
