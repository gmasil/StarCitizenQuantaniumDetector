package de.gmasil.sc.quantaniumdetector;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;

import net.sourceforge.tess4j.ITessAPI.TessOcrEngineMode;
import net.sourceforge.tess4j.ITessAPI.TessPageSegMode;
import net.sourceforge.tess4j.Tesseract;

public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ArduinoConnector ARDUINO = new ArduinoConnector();

    private static boolean service = true;
    private static boolean isRunning = true;

    public static void main(String[] args) {
        if (!service) {
            testImage();
            return;
        }
        SerialPort port = ARDUINO.openDevicePort();
        while (isRunning) {
            int currentTimeLeft = scanScreen();
            if (currentTimeLeft >= 0) {
                sendTime(port, currentTimeLeft);
                LOG.info("Time left: {}s", currentTimeLeft);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void testImage() {
        BufferedImage img;
        try {
            img = ImageIO.read(new File("timer-full.jpg"));
        } catch (IOException e) {
            throw new IllegalStateException("Error while loading image", e);
        }
        invertImage(img);
        img = cutImageMiningStability(img);
        String text = performOCR(img, false);
        int timeLeft = extractTime(text);
        LOG.info("Time left: {}s", timeLeft);
    }

    public static int scanScreen() {
        BufferedImage img = takeScreenshot();
        invertImage(img);
        img = cutImageMiningStability(img);
        String text = performOCR(img, false);
        try {
            return extractTime(text);
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    public static void sendTime(SerialPort port, int time) {
        String msg = "time:" + time + ";";
        byte[] bytes = msg.getBytes(StandardCharsets.US_ASCII);
        port.writeBytes(bytes, bytes.length);
    }

    public static int extractTime(String text) {
        text = text.trim().replace("\n", " ");
        Pattern pattern = Pattern.compile(".*TIME UNTIL CRITICAL +(\\d\\d):(\\d\\d):(\\d\\d).*");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("The input text '%s' has an invalid form", text));
        }
        String hoursText = matcher.group(1);
        String minutesText = matcher.group(2);
        String secondsText = matcher.group(3);
        int hours = Integer.parseInt(hoursText);
        int minutes = Integer.parseInt(minutesText);
        int seconds = Integer.parseInt(secondsText);
        return hours * 60 * 60 + minutes * 60 + seconds;
    }

    public static String performOCR(BufferedImage img, boolean saveImage) {
        try {
            if (saveImage) {
                ImageIO.write(img, "png", new File("out.png"));
            }
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("tessdata");
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(TessPageSegMode.PSM_SPARSE_TEXT);
            tesseract.setOcrEngineMode(TessOcrEngineMode.OEM_DEFAULT);
            tesseract.setVariable("user_defined_dpi", "270");
            tesseract.setVariable("min_characters_to_try", "5");
            return tesseract.doOCR(img);
        } catch (Exception e) {
            throw new IllegalStateException("Error while performing OCR", e);
        }
    }

    public static BufferedImage cutImageMiningStability(BufferedImage img) {
        int x = img.getWidth() * 2 / 3;
        int y = img.getHeight() / 2;
        int width = img.getWidth() / 3;
        int height = img.getHeight() / 2;
        return cutImage(img, x, y, width, height);
    }

    public static BufferedImage cutImage(BufferedImage img, int x, int y, int width, int height) {
        try {
            BufferedImage cut = new BufferedImage(width, height, img.getType());
            cut.getGraphics().drawImage(img, 0, 0, width, height, x, y, x + width, y + height, null);
            return cut;
        } catch (Exception e) {
            throw new IllegalStateException("Error while cutting image", e);
        }
    }

    public static BufferedImage takeScreenshot() {
        try {
            Rectangle screenRect = new Rectangle((Toolkit.getDefaultToolkit().getScreenSize().width),
                    (Toolkit.getDefaultToolkit().getScreenSize().height));
            BufferedImage screenshot = new Robot().createScreenCapture(screenRect);
            ImageIO.write(screenshot, "png", new File("screenshot.png"));
            return screenshot;
        } catch (Exception e) {
            throw new IllegalStateException("Error while taking screenshot", e);
        }
    }

    public static void invertImage(BufferedImage input) {
        for (int x = 0; x < input.getWidth(); x++) {
            for (int y = 0; y < input.getHeight(); y++) {
                int rgba = input.getRGB(x, y);
                Color col = new Color(rgba, true);
                col = new Color(255 - col.getRed(), 255 - col.getGreen(), 255 - col.getBlue());
                input.setRGB(x, y, col.getRGB());
            }
        }
    }
}
