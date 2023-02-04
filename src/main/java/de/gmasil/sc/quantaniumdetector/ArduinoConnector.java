package de.gmasil.sc.quantaniumdetector;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;

public class ArduinoConnector {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public SerialPort openDevicePort() {
        SerialPort[] ports = SerialPort.getCommPorts();
        LOG.info("Scanning {} hardware devices...", ports.length);
        for (SerialPort port : ports) {
            if (port.openPort()) {
                if (performHandshake(port, 5000)) {
                    LOG.info("Hardware device found: {}", port.getDescriptivePortName());
                    return port;
                }
                port.closePort();
            }
        }
        return null;
    }

    private boolean performHandshake(SerialPort port, long timeout) {
        requestHandshake(port);
        long start = new Date().getTime();
        long time = 0;
        while (port.bytesAvailable() == 0 && time < timeout) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            time = new Date().getTime() - start;
        }
        if (port.bytesAvailable() > 0) {
            byte[] readBuffer = new byte[port.bytesAvailable()];
            port.readBytes(readBuffer, readBuffer.length);
            if (new String(readBuffer).equals("handshake")) {
                return true;
            }
        }
        return false;
    }

    public void requestHandshake(SerialPort port) {
        String handshake = "handshake;";
        byte[] bytes = handshake.getBytes(StandardCharsets.US_ASCII);
        port.writeBytes(bytes, bytes.length);
    }
}
