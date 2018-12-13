package io.auklet.config;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.Jsoner;
import io.auklet.Auklet;
import io.auklet.AukletException;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.github.cliftonlabs.json_simple.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * <p>The <i>device authentication file</i> contains the Auklet organization ID to which the application ID
 * belongs, as well as the credentials used to authenticate to the {@code auklet.io} data pipeline.</p>
 */
public final class DeviceAuth extends AbstractConfigFileFromApi<DeviceAuth, JsonObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceAuth.class);
    public static final String FILENAME = "AukletAuth";

    private final Cipher aesCipher;
    private final Key aesKey;
    private final String organizationId;
    private final String clientId;
    private final String clientUsername;
    private final String clientPassword;

    /**
     * <p>Constructor.</p>
     *
     * @param agent the Auklet agent object.
     * @throws AukletException if the underlying config file cannot be obtained from the filesystem/API,
     * or if it cannot be written to disk.
     */
    public DeviceAuth(Auklet agent) throws AukletException {
        super(agent);
        // Setup AES cipher.
        try {
            this.aesCipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new AukletException(e);
        }
        // MQTT credentials are derived from the app ID, so use that as the encryption key.
        // This way, if the app ID changes, we will obtain new credentials.
        this.aesKey = new SecretKeySpec(this.agent.getAppId().substring(0,16).getBytes(), "AES");
        // Read/parse the config.
        JsonObject config = this.loadConfig();
        this.organizationId = (String) config.get("organization");
        this.clientId = (String) config.get("client_id");
        this.clientUsername = (String) config.get("id");
        this.clientPassword = (String) config.get("client_password");
    }

    @Override
    public String getName() {
        return DeviceAuth.FILENAME;
    }

    /**
     * <p>Returns the organization ID for this device.</p>
     *
     * @return never {@code null}.
     */
    public String getOrganizationId() {
        return this.organizationId;
    }

    /**
     * <p>Returns the MQTT client ID for this device.</p>
     *
     * @return never {@code null}.
     */
    public String getClientId() {
        return this.clientId;
    }

    /**
     * <p>Returns the MQTT client username for this device.</p>
     *
     * @return never {@code null}.
     */
    public String getClientUsername() {
        return this.clientUsername;
    }

    /**
     * <p>Returns the MQTT client password for this device.</p>
     *
     * @return never {@code null}.
     */
    public String getClientPassword() {
        return this.clientPassword;
    }

    /**
     * <p>Returns the MQTT topic that should be used for publishing event messages.</p>
     *
     * @return never {@code null}.
     */
    public String getMqttEventsTopic() {
        return "java/events/" + this.getOrganizationId() + "/" + this.getClientUsername();
    }

    @Override
    protected JsonObject readFromDisk() {
        try {
            // Read and decrypt the device auth file from disk.
            byte[] authFileBytes = Files.readAllBytes(this.file.toPath());
            this.aesCipher.init(Cipher.DECRYPT_MODE, this.aesKey);
            String authFileDecrypted = new String(this.aesCipher.doFinal(authFileBytes));
            // Parse the JSON and set relevant fields.
            return (JsonObject) Jsoner.deserialize(authFileDecrypted);
        } catch (IOException | SecurityException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | JsonException e) {
            LOGGER.warn("Could not read device auth file from disk, will re-register device with API", e);
            return null;
        }
    }

    @Override
    protected JsonObject fetchFromApi() throws AukletException {
        try {
            JsonObject requestJson = new JsonObject();
            requestJson.put("mac_address_hash", this.agent.getMacHash());
            requestJson.put("application", this.agent.getAppId());
            Request.Builder request = new Request.Builder()
                    .url(this.agent.getBaseUrl() + "/private/devices/")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestJson.toJson()))
                    .header("Content-Type", "application/json; charset=utf-8");
            try (Response response = this.agent.api(request)) {
                String responseJson = response.body().string();
                if (response.isSuccessful()) {
                    return (JsonObject) Jsoner.deserialize(responseJson);
                } else {
                    throw new AukletException(String.format("Error while creating device: %s: %s", response.message(), responseJson));
                }
            }
        } catch (IOException | JsonException e) {
            throw new AukletException("Could not register device", e);
        }
    }

    @Override
    protected void writeToDisk(JsonObject contents) throws AukletException {
        try {
            // Encrypt and save the JSON string to disk.
            this.aesCipher.init(Cipher.ENCRYPT_MODE, this.aesKey);
            byte[] encrypted = this.aesCipher.doFinal(contents.toJson().getBytes("UTF-8"));
            Files.write(this.file.toPath(), encrypted);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | IOException e) {
            throw new AukletException("Could not encrypt/save device data to disk", e);
        }
    }

}
