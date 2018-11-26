package io.auklet.agent.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import purejavacomm.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SerialClient implements Client {


    static private Logger logger = LoggerFactory.getLogger(SerialClient.class);
    private Boolean setUp;
    private SerialPort comm;
    private OutputStream stream;

    public SerialClient(String portName) {
        try {
            comm = (SerialPort) CommPortIdentifier.getPortIdentifier(portName).open("AukletPort", 1000);
            stream = comm.getOutputStream();
            setUp = true;
        } catch (NoSuchPortException | PortInUseException | IOException e) {
            setUp = false;
            logger.error(e.toString());
        }
    }

    @Override
    public boolean isSetUp() {
        return setUp;
    }

    @Override
    public void sendEvent(String topic, byte[] bytesToSend) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("topic", topic);
            map.put("payload", bytesToSend);

            ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
            byte[] bytes = objectMapper.writeValueAsBytes(map);
            stream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown(ScheduledExecutorService threadPool) {
        try {
            stream.close();
            comm.close();
        } catch (IOException e) {
            logger.error("Error while shutting down Serial Client", e);
        } finally {
            threadPool.shutdown();
            try {
                threadPool.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e2) {}
        }
    }
}
